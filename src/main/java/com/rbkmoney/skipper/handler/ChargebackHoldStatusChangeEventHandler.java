package com.rbkmoney.skipper.handler;

import com.rbkmoney.damsel.skipper.ChargebackEvent;
import com.rbkmoney.damsel.skipper.ChargebackHoldStatusChangeEvent;
import com.rbkmoney.reporter.domain.tables.pojos.Chargeback;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackHoldState;
import com.rbkmoney.skipper.dao.ChargebackDao;
import com.rbkmoney.skipper.exception.NotFoundException;
import com.rbkmoney.skipper.util.MapperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChargebackHoldStatusChangeEventHandler implements EventHandler {

    private final ChargebackDao chargebackDao;

    @Override
    @Transactional
    public void handle(ChargebackEvent event) throws Exception {
        ChargebackHoldStatusChangeEvent holdStatusChangeEvent = event.getHoldStatusChangeEvent();
        String invoiceId = holdStatusChangeEvent.getInvoiceId();
        String paymentId = holdStatusChangeEvent.getPaymentId();
        String chargebackId = holdStatusChangeEvent.getChargebackId();
        log.info("Processing new chargeback hold status change event (invoice id = {}, payment id = {}, " +
                        "chargeback id = {})", invoiceId, paymentId, chargebackId);
        Chargeback chargeback = chargebackDao.getChargeback(invoiceId, paymentId, chargebackId, false);
        if (chargeback == null) {
            log.error("Source chargeback for hold status change operation not found! (invoice id = {}, " +
                    "payment id = {}, chargeback id = {})", invoiceId, paymentId, chargebackId);
            throw new NotFoundException(String.format("Source chargeback for hold status change operation not found! " +
                    "(invoice id = %s, payment id = %s, chargeback id = %s)", invoiceId, paymentId, chargebackId));
        }
        ChargebackHoldState holdState =
                MapperUtils.mapToChargebackHoldState(holdStatusChangeEvent, chargeback.getId());
        chargebackDao.saveChargebackHoldState(holdState);
        log.info("New chargeback hold status change was saved (invoice id = {}, " +
                        "payment id = {}, chargebackId = {})", invoiceId, paymentId, chargebackId);
    }

}
