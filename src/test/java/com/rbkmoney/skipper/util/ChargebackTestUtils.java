package com.rbkmoney.skipper.util;

import com.rbkmoney.damsel.skipper.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChargebackTestUtils {

    public static ChargebackEvent createChargebackTestEvent(String invoiceId,
                                                            String paymentId,
                                                            String chargebackId,
                                                            String providerId,
                                                            boolean isRetrievalRequest) {
        ChargebackEvent event = new ChargebackEvent();
        ChargebackCreateEvent createEvent = new ChargebackCreateEvent();

        ChargebackGeneralData generalData = new ChargebackGeneralData();
        generalData.setPretensionDate(MapperUtils.localDateTimeToString(LocalDateTime.now()));
        generalData.setProviderId(providerId);
        generalData.setOperationDate(MapperUtils.localDateTimeToString(LocalDateTime.now().minusDays(20L)));
        generalData.setInvoiceId(invoiceId);
        generalData.setPaymentId(paymentId);
        generalData.setChargebackId(chargebackId);
        generalData.setRrn("rrn_001");
        generalData.setMaskedPan("000000******0000");
        generalData.setLevyAmount(1000L);
        generalData.setBodyAmount(1000L);
        generalData.setCurrency("USD");
        generalData.setShopUrl("some url");
        generalData.setPartyEmail("email 1");
        generalData.setContactEmail("email 2");
        generalData.setShopId("shop-1");
        generalData.setExternalId("ext_1");
        ChargebackReason reason = new ChargebackReason();
        reason.setCode("11");
        ChargebackCategory category = new ChargebackCategory();
        category.setFraud(new ChargebackCategoryFraud());
        reason.setCategory(category);
        generalData.setChargebackReason(reason);
        generalData.setContent(null);
        generalData.setRetrievalRequest(isRetrievalRequest);
        createEvent.setCreationData(generalData);

        event.setCreateEvent(createEvent);
        return event;
    }

    public static ChargebackEvent createChargebackStatusChangeTestEvent(String invoiceId,
                                                                        String paymentId,
                                                                        String chargebackId,
                                                                        ChargebackStage stage,
                                                                        ChargebackStatus status) {
        ChargebackEvent event = new ChargebackEvent();
        ChargebackStatusChangeEvent statusChangeEvent = new ChargebackStatusChangeEvent();
        statusChangeEvent.setInvoiceId(invoiceId);
        statusChangeEvent.setPaymentId(paymentId);
        statusChangeEvent.setChargebackId(chargebackId);
        statusChangeEvent.setStage(stage);
        statusChangeEvent.setStatus(status);
        statusChangeEvent.setCreatedAt(MapperUtils.localDateTimeToString(LocalDateTime.now()));
        statusChangeEvent.setDateOfDecision(null);
        event.setStatusChangeEvent(statusChangeEvent);
        return event;
    }

    public static ChargebackEvent createChargebackStatusChangeTestEvent(String invoiceId,
                                                                        String paymentId,
                                                                        String chargebackId,
                                                                        ChargebackStage stage) {
        ChargebackEvent event = new ChargebackEvent();
        ChargebackReopenEvent reopenEvent = new ChargebackReopenEvent();
        reopenEvent.setInvoiceId(invoiceId);
        reopenEvent.setPaymentId(paymentId);
        reopenEvent.setChargebackId(chargebackId);
        reopenEvent.setCreatedAt(MapperUtils.localDateTimeToString(LocalDateTime.now()));
        reopenEvent.setLevyAmount(1100L);
        reopenEvent.setBodyAmount(1100L);
        reopenEvent.setReopenStage(stage);
        event.setReopenEvent(reopenEvent);
        return event;
    }

    public static ChargebackEvent createChargebackHoldStatusChangeTestEvent(String invoiceId,
                                                                            String paymentId,
                                                                            String chargebackId) {
        ChargebackEvent event = new ChargebackEvent();
        ChargebackHoldStatusChangeEvent holdStatusChangeEvent = new ChargebackHoldStatusChangeEvent();

        holdStatusChangeEvent.setInvoiceId(invoiceId);
        holdStatusChangeEvent.setPaymentId(paymentId);
        holdStatusChangeEvent.setChargebackId(chargebackId);
        holdStatusChangeEvent.setCreatedAt(MapperUtils.localDateTimeToString(LocalDateTime.now()));
        ChargebackHoldStatus status = new ChargebackHoldStatus()
                .setHoldFromUs(false)
                .setWillHoldFromMerchant(true)
                .setWasHoldFromMerchant(true);
        holdStatusChangeEvent.setHoldStatus(status);

        event.setHoldStatusChangeEvent(holdStatusChangeEvent);
        return event;
    }

}
