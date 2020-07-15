package com.rbkmoney.skipper.handler;

import com.rbkmoney.damsel.domain.Cash;
import com.rbkmoney.damsel.domain.CurrencyRef;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.damsel.skipper.*;
import com.rbkmoney.reporter.domain.tables.pojos.Chargeback;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackState;
import com.rbkmoney.skipper.dao.ChargebackDao;
import com.rbkmoney.skipper.exception.NotFoundException;
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
public class ChargebackStatusChangeEventHandler implements EventHandler {

    private final InvoicingSrv.Iface invoicingService;

    private final ChargebackDao chargebackDao;

    @Override
    public void handle(ChargebackEvent event) throws Exception {
        var statusChangeEvent = event.getStatusChangeEvent();
        String invoiceId = statusChangeEvent.getInvoiceId();
        String paymentId = statusChangeEvent.getPaymentId();
        log.info("Processing new chargeback status change event (invoice id = {}, payment id = {})",
                invoiceId, paymentId);
        Chargeback chargeback = chargebackDao.getChargeback(invoiceId, paymentId, false);
        List<ChargebackState> chargebackStates = chargebackDao.getChargebackStates(chargeback.getId());
        ChargebackState prevState = chargebackStates.stream()
                .max(Comparator.comparing(ChargebackState::getCreatedAt))
                .orElseThrow(() -> new NotFoundException());
        ChargebackState chargebackState =
                MapperUtils.mapToChargebackState(statusChangeEvent, prevState, chargeback.getId());
        chargebackDao.saveChargebackState(chargebackState);

        saveStateToHellgate(statusChangeEvent, chargebackState, chargeback);
    }

    private void saveStateToHellgate(ChargebackStatusChangeEvent statusChangeEvent,
                                     ChargebackState chargebackState,
                                     Chargeback chargeback) throws TException {
        String invoiceId = statusChangeEvent.getInvoiceId();
        String paymentId = statusChangeEvent.getPaymentId();
        String occuredAt = MapperUtils.localDateTimeToString(chargebackState.getCreatedAt());
        String chargebackId = String.valueOf(chargeback.getId());
        ChargebackStatus status = statusChangeEvent.getStatus();
        if (status.isSetAccepted()) {
            ChargebackAccepted accepted = status.getAccepted();
            InvoicePaymentChargebackAcceptParams acceptParams = new InvoicePaymentChargebackAcceptParams();
            acceptParams.setOccurredAt(occuredAt);
            String currency = chargeback.getCurrency();
            acceptParams.setBody(new Cash()
                    .setAmount(accepted.getBodyAmount())
                    .setCurrency(new CurrencyRef().setSymbolicCode(currency))
            );
            acceptParams.setLevy(new Cash()
                    .setAmount(accepted.getLevyAmount())
                    .setCurrency(new CurrencyRef().setSymbolicCode(currency))
            );
            invoicingService.acceptChargeback(USER_INFO, invoiceId, paymentId, chargebackId, acceptParams);
        } else if (status.isSetCancelled()) {
            InvoicePaymentChargebackCancelParams cancelParams = new InvoicePaymentChargebackCancelParams();
            cancelParams.setOccurredAt(occuredAt);
            invoicingService.cancelChargeback(USER_INFO, invoiceId, paymentId, chargebackId, cancelParams);
        } else if (status.isSetRejected()) {
            ChargebackRejected rejected = status.getRejected();
            InvoicePaymentChargebackRejectParams rejectParams = new InvoicePaymentChargebackRejectParams();
            rejectParams.setOccurredAt(occuredAt);
            rejectParams.setLevy(new Cash()
                    .setAmount(rejected.getLevyAmount())
                    .setCurrency(new CurrencyRef().setSymbolicCode(chargeback.getCurrency()))
            );
            invoicingService.rejectChargeback(USER_INFO, invoiceId, paymentId, chargebackId, rejectParams);
        }else {
            throw new UnsupportedOperationException();
        }
    }

}
