package com.rbkmoney.skipper.unit;

import com.rbkmoney.damsel.skipper.*;
import com.rbkmoney.reporter.domain.tables.pojos.Chargeback;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackHoldState;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackState;
import com.rbkmoney.skipper.util.MapperUtils;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

public final class MapperUtilsUnitTest {

    @Test
    public void chargebackMappingTest() {
        ChargebackGeneralData testChargebackGeneralData = createTestChargebackGeneralData();
        Chargeback chargeback = MapperUtils.mapToChargeback(testChargebackGeneralData);
        ChargebackEvent resultEvent = MapperUtils.transformToGeneralDataEvent(chargeback);
        assertEquals(testChargebackGeneralData, resultEvent.getCreateEvent().getCreationData());
    }

    @Test
    public void chargebackStateMappingTest() {
        ChargebackStatusChangeEvent testChargebackStatusChangeEvent = createTestChargebackStatusChangeEvent();
        ChargebackState chargebackState = MapperUtils.mapToChargebackState(
                testChargebackStatusChangeEvent,
                createTestPrevChargebackState(),
                1L
        );
        ChargebackEvent chargebackEvent = MapperUtils.transformToChargebackStatusChangeEvent(chargebackState);
        assertEquals(testChargebackStatusChangeEvent, chargebackEvent.getStatusChangeEvent());
    }

    @Test
    public void chargebackHoldStateMappingTest() {
        ChargebackHoldStatusChangeEvent holdStatusChangeEvent = createTestChargebackHoldStatusChangeEvent();
        ChargebackHoldState holdState = MapperUtils.mapToChargebackHoldState(holdStatusChangeEvent, 1L);
        ChargebackEvent event = MapperUtils.transformToChargebackHoldStatusChangeEvent(holdState);
        assertEquals(holdStatusChangeEvent, event.getHoldStatusChangeEvent());
    }

    private static ChargebackGeneralData createTestChargebackGeneralData() {
        ChargebackGeneralData generalData = new ChargebackGeneralData();
        generalData.setPretensionDate("2020-07-15T19:08:06.171708Z");
        generalData.setOperationDate("2020-07-01T19:08:06.171708Z");
        generalData.setInvoiceId("invoice-1");
        generalData.setPaymentId("payment-1");
        generalData.setChargebackId("chargeback-1");
        generalData.setLevyAmount(1000L);
        generalData.setCurrency("USD");
        generalData.setShopId("shop-1");

        ChargebackReason reason = new ChargebackReason();
        ChargebackCategory category = new ChargebackCategory();
        category.setFraud(new ChargebackCategoryFraud());
        reason.setCategory(category);
        generalData.setChargebackReason(reason);
        generalData.setContent(null);
        generalData.setRetrievalRequest(false);
        return generalData;
    }

    private static ChargebackStatusChangeEvent createTestChargebackStatusChangeEvent() {
        ChargebackStatusChangeEvent statusChangeEvent = new ChargebackStatusChangeEvent();
        statusChangeEvent.setInvoiceId("invoice-1");
        statusChangeEvent.setPaymentId("payment-1");
        statusChangeEvent.setChargebackId("chargeback-1");
        ChargebackStage stage = new ChargebackStage();
        stage.setChargeback(new StageChargeback());                        //stage - chargeback
        ChargebackStatus status = new ChargebackStatus();
        status.setAccepted(new ChargebackAccepted().setLevyAmount(1000L));
        statusChangeEvent.setStage(stage);
        statusChangeEvent.setStatus(status);
        statusChangeEvent.setCreatedAt(MapperUtils.localDateTimeToString(LocalDateTime.now()));
        statusChangeEvent.setDateOfDecision(null);
        return statusChangeEvent;
    }

    private static ChargebackState createTestPrevChargebackState() {
        ChargebackState state = new ChargebackState();
        state.setBodyAmount(1000L);
        state.setLevyAmount(800L);
        state.setStage(com.rbkmoney.reporter.domain.enums.ChargebackStage.CHARGEBACK);
        return state;
    }

    private static ChargebackHoldStatusChangeEvent createTestChargebackHoldStatusChangeEvent() {
        ChargebackHoldStatusChangeEvent holdStatusChangeEvent = new ChargebackHoldStatusChangeEvent();

        holdStatusChangeEvent.setInvoiceId("invoice-1");
        holdStatusChangeEvent.setPaymentId("payment-1");
        holdStatusChangeEvent.setChargebackId("chargeback-1");
        holdStatusChangeEvent.setCreatedAt(MapperUtils.localDateTimeToString(LocalDateTime.now()));
        ChargebackHoldStatus status = new ChargebackHoldStatus()
                .setHoldFromUs(false)
                .setWillHoldFromMerchant(true)
                .setWasHoldFromMerchant(true);
        holdStatusChangeEvent.setHoldStatus(status);
        return holdStatusChangeEvent;
    }

}
