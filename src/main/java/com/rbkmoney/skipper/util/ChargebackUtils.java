package com.rbkmoney.skipper.util;

import com.rbkmoney.damsel.skipper.*;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackHoldState;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static com.rbkmoney.reporter.domain.enums.ChargebackStage.*;
import static com.rbkmoney.reporter.domain.enums.ChargebackStatus.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChargebackUtils {

    public static ChargebackState createPendingState(ChargebackGeneralData creationData,
                                                     long chargebackId) {
        ChargebackState state = new ChargebackState();
        state.setChargebackId(chargebackId);
        state.setInvoiceId(creationData.getInvoiceId());
        state.setPaymentId(creationData.getPaymentId());
        //state.setLevyAmount(creationData.getLevyAmount());
        //state.setBodyAmount(creationData.getBodyAmount());
        state.setCreatedAt(TypeUtil.stringToLocalDateTime(creationData.getOperationDate()));
        state.setStatus(PENDING);
        state.setStage(CHARGEBACK);
        return state;
    }

    public static ChargebackHoldState createEmptyHoldState(ChargebackGeneralData creationData,
                                                           long chargebackId) {
        ChargebackHoldState holdState = new ChargebackHoldState();
        holdState.setChargebackId(chargebackId);
        holdState.setInvoiceId(creationData.getInvoiceId());
        holdState.setPaymentId(creationData.getPaymentId());
        //holdState.setCreatedAt(TypeUtil.stringToLocalDateTime(event.getCreatedAt()));
        holdState.setWillHoldFromMerchant(false);
        holdState.setWasHoldFromMerchant(false);
        holdState.setHoldFromUs(false);
        return holdState;
    }

}
