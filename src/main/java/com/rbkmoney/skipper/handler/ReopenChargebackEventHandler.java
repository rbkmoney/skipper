package com.rbkmoney.skipper.handler;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChargebackReopenParams;
import com.rbkmoney.damsel.payment_processing.InvoicingSrv;
import com.rbkmoney.damsel.skipper.ChargebackEvent;
import com.rbkmoney.damsel.skipper.ChargebackReopenEvent;
import com.rbkmoney.damsel.skipper.ChargebackStage;
import com.rbkmoney.reporter.domain.enums.ChargebackStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Chargeback;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackState;
import com.rbkmoney.skipper.dao.ChargebackDao;
import com.rbkmoney.skipper.exception.BusinessException;
import com.rbkmoney.skipper.exception.NotFoundException;
import com.rbkmoney.skipper.exception.UnsupportedStageException;
import com.rbkmoney.skipper.util.MapperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

import static com.rbkmoney.skipper.util.HellgateUtils.USER_INFO;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReopenChargebackEventHandler implements EventHandler {

    private final InvoicingSrv.Iface invoicingService;

    private final ChargebackDao chargebackDao;

    @Override
    public void handle(ChargebackEvent event) throws Exception {
        ChargebackReopenEvent reopenEvent = event.getReopenEvent();
        String invoiceId = reopenEvent.getInvoiceId();
        String paymentId = reopenEvent.getPaymentId();
        String chargebackId = reopenEvent.getChargebackId();
        log.info("Processing new reopen chargeback event (invoice id = {}, payment id = {}, chatgebackId = {})",
                invoiceId, paymentId, chargebackId);
        Chargeback chargeback = chargebackDao.getChargeback(invoiceId, paymentId, chargebackId, false);
        if (chargeback == null) {
            log.error("Source chargeback for reopen operation not found! (invoice id = {}, " +
                    "payment id = {}, chargeback id = {})", invoiceId, paymentId, chargebackId);
            throw new NotFoundException(String.format("Source chargeback for reopen operation not found! " +
                    "(invoice id = %s, payment id = %s, chargeback id = %s)", invoiceId, paymentId, chargebackId));
        }
        Long extId = chargeback.getId();
        saveReopenEventToDatabase(reopenEvent, extId);
        sendReopenEventToHellgate(reopenEvent, chargeback);
        log.info("New reopen chargeback event was processed (extId = {}, invoice id = {}, " +
                        "payment id = {}, chatgebackId = {})",
                extId, invoiceId, paymentId, chargebackId);
    }

    private void saveReopenEventToDatabase(ChargebackReopenEvent reopenEvent, long extId) {
        String invoiceId = reopenEvent.getInvoiceId();
        String paymentId = reopenEvent.getPaymentId();
        String chargebackId = reopenEvent.getChargebackId();
        List<ChargebackState> states = chargebackDao.getChargebackStates(invoiceId, paymentId, chargebackId);
        ChargebackState prevState = states.stream()
                .max(Comparator.comparing(ChargebackState::getCreatedAt))
                .orElseThrow(() -> new NotFoundException());
        ChargebackState chargebackState =
                MapperUtils.transformReopenToChargebackState(reopenEvent, prevState, extId);
        if (prevState.getStage() == chargebackState.getStage()) {
            throw new BusinessException(String.format("The stage of the processed event is equal to previous event " +
                    "(current: '%s', previous: '%s')", chargebackState.toString(), prevState.toString()));
        }
        if (prevState.getStatus() != ChargebackStatus.REJECTED) {
            throw new BusinessException(String.format("The stage can be reopen only from REJECTED status " +
                    "(invoice id = %s, payment id = %s, chargeback id = %s)", invoiceId, paymentId, chargebackId));
        }

        chargebackDao.saveChargebackState(chargebackState);
    }

    private void sendReopenEventToHellgate(ChargebackReopenEvent reopenEvent,
                                           Chargeback chargeback) throws TException {
        String invoiceId = reopenEvent.getInvoiceId();
        String paymentId = reopenEvent.getPaymentId();
        InvoicePaymentChargebackReopenParams reopenParams = new InvoicePaymentChargebackReopenParams();
        reopenParams.setOccurredAt(reopenEvent.getCreatedAt());
        String currency = chargeback.getCurrency();
        if (reopenEvent.getBodyAmount() != 0) {
            reopenParams.setBody(new Cash()
                    .setAmount(reopenEvent.getBodyAmount())
                    .setCurrency(new CurrencyRef().setSymbolicCode(currency))
            );
        }
        if (reopenEvent.getLevyAmount() != 0) {
            reopenParams.setLevy(new Cash()
                    .setAmount(reopenEvent.getLevyAmount())
                    .setCurrency(new CurrencyRef().setSymbolicCode(currency))
            );
        }
        if (reopenEvent.isSetReopenStage()) {
            ChargebackStage reopenStage = reopenEvent.getReopenStage();
            InvoicePaymentChargebackStage stage = new InvoicePaymentChargebackStage();
            if (reopenStage.isSetPreArbitration()) {
                stage.setPreArbitration(new InvoicePaymentChargebackStagePreArbitration());
            } else if (reopenStage.isSetArbitration()) {
                stage.setArbitration(new InvoicePaymentChargebackStageArbitration());
            } else {
                throw new UnsupportedStageException();
            }
            reopenParams.setMoveToStage(stage);
        }
        invoicingService.reopenChargeback(
                USER_INFO,
                invoiceId,
                paymentId,
                String.valueOf(chargeback.getId()),
                reopenParams
        );
    }

}
