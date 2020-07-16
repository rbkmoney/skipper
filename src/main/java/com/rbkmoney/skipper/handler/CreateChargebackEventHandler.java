package com.rbkmoney.skipper.handler;

import com.rbkmoney.damsel.payment_processing.InvoicingSrv;
import com.rbkmoney.damsel.skipper.ChargebackEvent;
import com.rbkmoney.damsel.skipper.ChargebackGeneralData;
import com.rbkmoney.reporter.domain.tables.pojos.Chargeback;
import com.rbkmoney.skipper.dao.ChargebackDao;
import com.rbkmoney.skipper.util.ChargebackUtils;
import com.rbkmoney.skipper.util.MapperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.rbkmoney.skipper.util.HellgateUtils.USER_INFO;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateChargebackEventHandler implements EventHandler {

    private final InvoicingSrv.Iface invoicingService;

    private final ChargebackDao chargebackDao;

    @Override
    public void handle(ChargebackEvent event) throws Exception {
        ChargebackGeneralData creationData = event.getCreateEvent().getCreationData();
        String invoiceId = creationData.getInvoiceId();
        String paymentId = creationData.getPaymentId();
        String chargebackId = creationData.getChargebackId();
        log.info("Processing new chargeback (invoice id = {}, payment id = {})", invoiceId, paymentId);
        Chargeback chargeback = MapperUtils.mapToChargeback(creationData);
        long extId = chargebackDao.saveChargeback(chargeback);
        chargebackDao.saveChargebackState(ChargebackUtils.createPendingState(creationData, extId));
        chargebackDao.saveChargebackHoldState(ChargebackUtils.createEmptyHoldState(creationData, extId));
        log.info("New chargeback was saved with id {} (invoice id = {}, payment id = {}, chargeback id = {})",
                extId, invoiceId, paymentId, chargebackId);
        if (creationData.isRetrievalRequest()) {
            return;
        }
        var cbParams = MapperUtils.mapToInvoicePaymentChargebackParams(creationData);
        var hgChargeback = invoicingService.createChargeback(USER_INFO, invoiceId, paymentId, cbParams);
        log.info("Chargeback was created in HG (invoice id = {}, payment id = {}, chargeback id = {}). Return info: {}",
                invoiceId, paymentId, chargebackId, hgChargeback);
    }

}
