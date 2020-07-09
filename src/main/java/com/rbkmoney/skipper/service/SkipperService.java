package com.rbkmoney.skipper.service;

import com.rbkmoney.damsel.domain.Cash;
import com.rbkmoney.damsel.domain.CurrencyRef;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.damsel.skipper.*;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.domain.tables.pojos.Chargeback;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackHoldState;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackState;
import com.rbkmoney.skipper.dao.ChargebackDao;
import com.rbkmoney.skipper.util.ChargebackUtils;
import com.rbkmoney.skipper.util.MapperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static com.rbkmoney.skipper.util.HellgateUtils.USER_INFO;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkipperService implements SkipperSrv.Iface {

    private final InvoicingSrv.Iface invoicingService;

    private final ChargebackDao chargebackDao;

    @Override
    public void processChargebackData(ChargebackEvent event) throws TException {
        if (event.isSetCreateEvent()) {
            ChargebackGeneralData creationData = event.getCreateEvent().getCreationData();
            String invoiceId = creationData.getInvoiceId();
            String paymentId = creationData.getPaymentId();
            log.info("Processing new chargeback (invoice id = {}, payment id = {})", invoiceId, paymentId);
            Chargeback chargeback = MapperUtils.transformToChargeback(creationData);
            long chargebackId = chargebackDao.saveChargeback(chargeback);
            chargebackDao.saveChargebackState(ChargebackUtils.createPendingState(creationData, chargebackId));
            chargebackDao.saveChargebackHoldState(ChargebackUtils.createEmptyHoldState(creationData, chargebackId));
            log.info("New chargeback was saved with id {} (invoice id = {}, payment id = {})",
                        chargebackId, invoiceId, paymentId);
            if (creationData.isRetrievalRequest()) {
                return;
            }
            var cbParams = MapperUtils.transformToInvoicePaymentChargebackParams(creationData, chargebackId);
            var hgChargeback = invoicingService.createChargeback(USER_INFO, invoiceId, paymentId, cbParams);
            log.info("Chargeback was created in HG (invoice id = {}, payment id = {}). Return info: {}",
                    invoiceId, paymentId, hgChargeback);
        } else if (event.isSetStatusChangeEvent()) {
            var statusChangeEvent = event.getStatusChangeEvent();
            String invoiceId = statusChangeEvent.getInvoiceId();
            String paymentId = statusChangeEvent.getPaymentId();
            log.info("Processing new chargeback status change event (invoice id = {}, payment id = {})",
                    invoiceId, paymentId);
            Chargeback chargeback = chargebackDao.getChargeback(invoiceId, paymentId);
            ChargebackState chargebackState =
                    MapperUtils.transformToChargebackState(statusChangeEvent, chargeback.getId());
            chargebackDao.saveChargebackState(chargebackState);

            ChargebackStage stage = statusChangeEvent.getStage();
            String occuredAt = chargebackState.getCreatedAt().toInstant(ZoneOffset.UTC).toString();
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
                invoicingService.acceptChargeback(
                        USER_INFO,
                        invoiceId,
                        paymentId,
                        String.valueOf(chargeback.getId()),
                        acceptParams
                );
            } else if (status.isSetCancelled()) {
                InvoicePaymentChargebackCancelParams cancelParams = new InvoicePaymentChargebackCancelParams();
                cancelParams.setOccurredAt(occuredAt);
                invoicingService.cancelChargeback(
                        USER_INFO,
                        invoiceId,
                        paymentId,
                        String.valueOf(chargeback.getId()),
                        cancelParams
                );
            } else if (status.isSetRejected()) {
                ChargebackRejected rejected = status.getRejected();
                InvoicePaymentChargebackRejectParams rejectParams = new InvoicePaymentChargebackRejectParams();
                rejectParams.setOccurredAt(occuredAt);
                rejectParams.setLevy(new Cash()
                        .setAmount(rejected.getLevyAmount())
                        .setCurrency(new CurrencyRef().setSymbolicCode(chargeback.getCurrency()))
                );
                invoicingService.rejectChargeback(
                        USER_INFO,
                        invoiceId,
                        paymentId,
                        String.valueOf(chargeback.getId()),
                        rejectParams
                );
            } else if (status.isSetReopen()) {
                ChargebackReopen reopen = status.getReopen();
                InvoicePaymentChargebackReopenParams reopenParams = new InvoicePaymentChargebackReopenParams();
                reopenParams.setOccurredAt(occuredAt);
                String currency = chargeback.getCurrency();
                reopenParams.setBody(new Cash()
                        .setAmount(reopen.getBodyAmount())
                        .setCurrency(new CurrencyRef().setSymbolicCode(currency))
                );
                reopenParams.setLevy(new Cash()
                        .setAmount(reopen.getLevyAmount())
                        .setCurrency(new CurrencyRef().setSymbolicCode(currency))
                );
                invoicingService.reopenChargeback(
                        USER_INFO,
                        invoiceId,
                        paymentId,
                        String.valueOf(chargeback.getId()),
                        reopenParams
                );
            } else {
                throw new UnsupportedOperationException();
            }
        } else if (event.isSetHoldStatusChangeEvent()) {
            ChargebackHoldStatusChangeEvent holdStatusChangeEvent = event.getHoldStatusChangeEvent();
            String invoiceId = holdStatusChangeEvent.getInvoiceId();
            String paymentId = holdStatusChangeEvent.getPaymentId();
            log.info("Processing new chargeback hold status change event (invoice id = {}, payment id = {})",
                    invoiceId, paymentId);
            Chargeback chargeback = chargebackDao.getChargeback(invoiceId, paymentId);
            Long chargebackId = chargeback.getId();
            ChargebackHoldState holdState =
                    MapperUtils.transformChargebackHoldState(holdStatusChangeEvent, chargebackId);
            chargebackDao.saveChargebackHoldState(holdState);
            log.info("New chargeback hold status change was saved (chargebackId = {}, invoice id = {}, payment id = {})",
                    chargebackId, invoiceId, paymentId);
        } else {
            throw new UnsupportedOperationException("Events with type '" + event.getSetField().getFieldName() +
                    "' unsupported");
        }
    }

    @Override
    public ChargebackData getChargebackData(String invoiceId, String paymentId) throws TException {
        //invoicingService.getPaymentChargeback();
        Chargeback chargeback = chargebackDao.getChargeback(invoiceId, paymentId);
        return getChargebackDataByChargeback(chargeback);
    }

    @Override
    public List<ChargebackData> getChargebacksByStep(ChargebackStage step,
                                                     ChargebackStatus status) throws TException {

        return null;
    }

    @Override
    public List<ChargebackData> getChargebacksByDate(String dateFrom, String dateTo) throws TException {
        List<Chargeback> chargebacksByDate = chargebackDao.getChargebacksByDate(
                TypeUtil.stringToLocalDateTime(dateFrom),
                TypeUtil.stringToLocalDateTime(dateTo)
        );
        List<ChargebackData> chargebackDataList = new ArrayList<>();
        for (Chargeback chargeback : chargebacksByDate) {
            chargebackDataList.add(getChargebackDataByChargeback(chargeback));
        }
        return chargebackDataList;
    }

    @Override
    public List<ChargebackData> getChargebacksByProviderId(String provider_id, List<ChargebackStatus> statuses) throws TException {
        return null;
    }

    private ChargebackData getChargebackDataByChargeback(Chargeback chargeback) {
        Long chargebackId = chargeback.getId();
        List<ChargebackState> chargebackStates = chargebackDao.getChargebackStates(chargebackId);
        List<ChargebackHoldState> chargebackHoldStates = chargebackDao.getChargebackHoldStates(chargebackId);
        return MapperUtils.transformToChargebackData(chargeback, chargebackStates, chargebackHoldStates);
    }
}
