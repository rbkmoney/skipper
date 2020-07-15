package com.rbkmoney.skipper.handler;

import com.rbkmoney.damsel.skipper.ChargebackEvent;
import com.rbkmoney.damsel.skipper.ChargebackHoldStatusChangeEvent;
import com.rbkmoney.reporter.domain.tables.pojos.Chargeback;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackHoldState;
import com.rbkmoney.skipper.dao.ChargebackDao;
import com.rbkmoney.skipper.util.MapperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChargebackHoldStatusChangeEventHandler implements EventHandler {

    private final ChargebackDao chargebackDao;

    @Override
    public void handle(ChargebackEvent event) throws Exception {
        ChargebackHoldStatusChangeEvent holdStatusChangeEvent = event.getHoldStatusChangeEvent();
        String invoiceId = holdStatusChangeEvent.getInvoiceId();
        String paymentId = holdStatusChangeEvent.getPaymentId();
        log.info("Processing new chargeback hold status change event (invoice id = {}, payment id = {})",
                invoiceId, paymentId);
        Chargeback chargeback = chargebackDao.getChargeback(invoiceId, paymentId, false);
        Long chargebackId = chargeback.getId();
        ChargebackHoldState holdState =
                MapperUtils.mapToChargebackHoldState(holdStatusChangeEvent, chargebackId);
        chargebackDao.saveChargebackHoldState(holdState);
        log.info("New chargeback hold status change was saved (chargebackId = {}, invoice id = {}, payment id = {})",
                chargebackId, invoiceId, paymentId);
    }

}
