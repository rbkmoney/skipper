package com.rbkmoney.skipper.integration;

import com.rbkmoney.damsel.domain.InvoicePaymentChargeback;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChargebackParams;
import com.rbkmoney.damsel.payment_processing.InvoicingSrv;
import com.rbkmoney.damsel.payment_processing.UserInfo;
import com.rbkmoney.damsel.skipper.*;
import com.rbkmoney.skipper.exception.NotFoundException;
import com.rbkmoney.skipper.util.MapperUtils;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SkipperIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SkipperSrv.Iface skipperService;

    @MockBean
    private InvoicingSrv.Iface invoicingService;

    private static final String TEST_DATE = MapperUtils.localDateTimeToString(LocalDateTime.now());

    @Before
    public void setUp() throws TException {
        when(invoicingService.createChargeback(any(UserInfo.class), any(String.class), any(String.class),
                any(InvoicePaymentChargebackParams.class)))
                .thenReturn(new InvoicePaymentChargeback());
    }

    @Test
    public void createNewChargebackTest() throws TException {
        String invoiceId = "inv_1";
        String paymentId = "pay_1";
        String providerId = "pr_1";
        skipperService.processChargebackData(createChargebackTestEvent(invoiceId, paymentId, providerId, true));
        skipperService.processChargebackData(createChargebackTestEvent(invoiceId, paymentId, providerId, false));
        ChargebackData chargebackData = skipperService.getChargebackData(invoiceId, paymentId);
        assertEquals("Count of events aren't equal to expected", 3, chargebackData.getEvents().size());
        List<ChargebackEvent> chargebackEvents = chargebackData.getEvents().stream()
                .filter(ChargebackEvent::isSetCreateEvent)
                .collect(Collectors.toList());
        assertEquals("Count of data events aren't equal to expected", 1, chargebackEvents.size());
        ChargebackCreateEvent createEvent = chargebackEvents.get(0).getCreateEvent();
        ChargebackGeneralData creationData = createEvent.getCreationData();
        assertEquals("Invoice id isn't equal to expected", invoiceId, creationData.getInvoiceId());
        assertEquals("Payment id isn't equal to expected", paymentId, creationData.getPaymentId());
        assertFalse("Returned chargeback is a retrieval request", creationData.isRetrievalRequest());
    }

    @Test
    public void changeChargebackStatusTest() throws TException {
        String invoiceId = "inv_2";
        String paymentId = "pay_2";
        String providerId = "pr_1";
        skipperService.processChargebackData(createChargebackTestEvent(invoiceId, paymentId, providerId, false));

        ChargebackStage stage_1 = new ChargebackStage();
        stage_1.setChargeback(new StageChargeback());                        //stage - chargeback
        ChargebackStatus status_1 = new ChargebackStatus();
        status_1.setRejected(new ChargebackRejected().setLevyAmount(1000L)); //status - rejected
        skipperService.processChargebackData(createChargebackStatusChangeTestEvent(invoiceId, paymentId, stage_1, status_1));

        ChargebackData chargebackData = skipperService.getChargebackData(invoiceId, paymentId);
        assertEquals("Count of events aren't equal to expected", 4, chargebackData.getEvents().size());
        List<ChargebackEvent> chargebackEvents = chargebackData.getEvents().stream()
                .filter(ChargebackEvent::isSetStatusChangeEvent)
                .collect(Collectors.toList());
        assertEquals("Count of status change events aren't equal to expected", 2, chargebackEvents.size());

        ChargebackStage reopenStage = new ChargebackStage();
        reopenStage.setArbitration(new StageArbitration());
        skipperService.processChargebackData(createChargebackStatusChangeTestEvent(invoiceId, paymentId, reopenStage));
        ChargebackData chargebackDataAfterReopen = skipperService.getChargebackData(invoiceId, paymentId);
        assertEquals("Count of events after reopen aren't equal to expected", 5, chargebackDataAfterReopen.getEvents().size());
        List<ChargebackEvent> chargebackEventsAfterReopen = chargebackDataAfterReopen.getEvents().stream()
                .filter(ChargebackEvent::isSetReopenEvent)
                .collect(Collectors.toList());
        assertEquals("Count of reopen events aren't equal to expected", 0, chargebackEventsAfterReopen.size());
    }

    @Test
    public void changeChargebackHoldStatusTest() throws TException {
        String invoiceId = "inv_3";
        String paymentId = "pay_3";
        String providerId = "pr_1";
        skipperService.processChargebackData(createChargebackTestEvent(invoiceId, paymentId, providerId, false));

        ChargebackStage stage = new ChargebackStage();
        stage.setChargeback(new StageChargeback());                        //stage - chargeback
        ChargebackStatus status = new ChargebackStatus();
        status.setAccepted(new ChargebackAccepted().setLevyAmount(1000L)); //status - acceptef
        skipperService.processChargebackData(createChargebackStatusChangeTestEvent(invoiceId, paymentId, stage, status));

        skipperService.processChargebackData(createChargebackHoldStatusChangeTestEvent(invoiceId, paymentId));
        ChargebackData chargebackData = skipperService.getChargebackData(invoiceId, paymentId);
        assertEquals("Count of events aren't equal to expected", 5, chargebackData.getEvents().size());

        List<ChargebackEvent> chargebackHoldEvents = chargebackData.getEvents().stream()
                .filter(ChargebackEvent::isSetHoldStatusChangeEvent)
                .collect(Collectors.toList());
        assertEquals("Count of hold status events aren't equal to expected", 2, chargebackHoldEvents.size());
    }

    @Test
    public void getChargebackDataTest() throws TException {
        for (int i = 0; i < 5; i++) {
            ChargebackStatus status = new ChargebackStatus();
            status.setAccepted(new ChargebackAccepted().setLevyAmount(1000L));
            createTestChargebackFlowData("inv_10" + i, "pay_1", "prov_1", status);
        }
        for (int i = 0; i < 5; i++) {
            ChargebackStatus status = new ChargebackStatus();
            status.setRejected(new ChargebackRejected().setLevyAmount(900L));
            createTestChargebackFlowData("inv_20" + i, "pay_1", "prov_2", status);
        }

        ChargebackFilter filterOne = new ChargebackFilter(); //search all records between date
        filterOne.setDateFrom(TEST_DATE);
        filterOne.setDateTo(MapperUtils.localDateTimeToString(LocalDateTime.now()));
        List<ChargebackData> chargebacksFilterOne = skipperService.getChargebacks(filterOne);
        assertEquals("Count of events aren't equal to expected", 10, chargebacksFilterOne.size());

        ChargebackData firstDataFromFilterOne = chargebacksFilterOne.stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException());
        assertEquals("Count of chargeback events aren't equal to expected",
                5, firstDataFromFilterOne.getEvents().size());

        ChargebackFilter filterTwo = new ChargebackFilter(); // empty search
        filterTwo.setDateFrom(MapperUtils.localDateTimeToString(LocalDateTime.now()));
        filterTwo.setDateTo(MapperUtils.localDateTimeToString(LocalDateTime.now()));
        List<ChargebackData> chargebacksFilterTwo = skipperService.getChargebacks(filterTwo);
        assertEquals("Count of events aren't equal to expected", 0, chargebacksFilterTwo.size());

        ChargebackFilter filterThree = new ChargebackFilter(); // search by provider ID
        filterThree.setDateFrom(TEST_DATE);
        filterThree.setProviderId("prov_2");
        List<ChargebackData> chargebacksFilterThree = skipperService.getChargebacks(filterThree);
        assertEquals("Count of events searched by provider id aren't equal to expected",
                5, chargebacksFilterThree.size());

        ChargebackFilter filterFour = new ChargebackFilter(); // search by status
        filterFour.setDateFrom(TEST_DATE);
        var status = new com.rbkmoney.damsel.skipper.ChargebackStatus();
        status.setAccepted(new ChargebackAccepted().setLevyAmount(800L));
        filterFour.setStatuses(Arrays.asList(status));
        List<ChargebackData> chargebacksFilterFour = skipperService.getChargebacks(filterFour);
        assertEquals("Count of events aren't equal to expected", 5, chargebacksFilterFour.size());
    }

    private void createTestChargebackFlowData(String invoiceId,
                                              String paymentId,
                                              String providerId,
                                              ChargebackStatus status) throws TException {
        skipperService.processChargebackData(
                createChargebackTestEvent(invoiceId, paymentId, providerId, false)
        );

        ChargebackStage stage = new ChargebackStage();
        skipperService.processChargebackData(createChargebackStatusChangeTestEvent(invoiceId, paymentId, stage, status));
        skipperService.processChargebackData(createChargebackHoldStatusChangeTestEvent(invoiceId, paymentId));
    }

    public static ChargebackEvent createChargebackTestEvent(String invoiceId,
                                                            String paymentId,
                                                            String providerId,
                                                            boolean isRetrievalRequest) {
        ChargebackEvent event = new ChargebackEvent();
        ChargebackCreateEvent createEvent = new ChargebackCreateEvent();

        ChargebackGeneralData generalData = new ChargebackGeneralData();
        generalData.setPretensionDate(TEST_DATE);
        generalData.setProviderId(providerId);
        generalData.setOperationDate(TEST_DATE);
        generalData.setInvoiceId(invoiceId);
        generalData.setPaymentId(paymentId);
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
                                                                        ChargebackStage stage,
                                                                        ChargebackStatus status) {
        ChargebackEvent event = new ChargebackEvent();
        ChargebackStatusChangeEvent statusChangeEvent = new ChargebackStatusChangeEvent();
        statusChangeEvent.setInvoiceId(invoiceId);
        statusChangeEvent.setPaymentId(paymentId);
        statusChangeEvent.setStage(stage);
        statusChangeEvent.setStatus(status);
        statusChangeEvent.setCreatedAt(MapperUtils.localDateTimeToString(LocalDateTime.now()));
        statusChangeEvent.setDateOfDecision(null);
        event.setStatusChangeEvent(statusChangeEvent);
        return event;
    }

    public static ChargebackEvent createChargebackStatusChangeTestEvent(String invoiceId,
                                                                        String paymentId,
                                                                        ChargebackStage stage) {
        ChargebackEvent event = new ChargebackEvent();
        ChargebackReopenEvent reopenEvent = new ChargebackReopenEvent();
        reopenEvent.setInvoiceId(invoiceId);
        reopenEvent.setPaymentId(paymentId);
        reopenEvent.setCreatedAt(MapperUtils.localDateTimeToString(LocalDateTime.now()));
        reopenEvent.setLevyAmount(1100L);
        reopenEvent.setBodyAmount(1100L);
        reopenEvent.setReopenStage(stage);
        event.setReopenEvent(reopenEvent);
        return event;
    }

    public static ChargebackEvent createChargebackHoldStatusChangeTestEvent(String invoiceId,
                                                                            String paymentId) {
        ChargebackEvent event = new ChargebackEvent();
        ChargebackHoldStatusChangeEvent holdStatusChangeEvent = new ChargebackHoldStatusChangeEvent();

        holdStatusChangeEvent.setInvoiceId(invoiceId);
        holdStatusChangeEvent.setPaymentId(paymentId);
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
