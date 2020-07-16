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

import static com.rbkmoney.skipper.util.ChargebackTestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SuccessBehaviorIntegrationTest extends AbstractIntegrationTest {

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
        String chargebackId = "ch_1";
        String providerId = "pr_1";
        skipperService.processChargebackData(createChargebackTestEvent(invoiceId, paymentId, chargebackId, providerId, true));
        skipperService.processChargebackData(createChargebackTestEvent(invoiceId, paymentId, chargebackId, providerId, false));
        ChargebackData chargebackData = skipperService.getChargebackData(invoiceId, paymentId, chargebackId);
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
        String chargebackId = "ch_2";
        String providerId = "pr_1";
        skipperService.processChargebackData(
                createChargebackTestEvent(invoiceId, paymentId, chargebackId, providerId, false)
        );

        ChargebackStage stage_1 = new ChargebackStage();
        stage_1.setChargeback(new StageChargeback());                        //stage - chargeback
        ChargebackStatus status_1 = new ChargebackStatus();
        status_1.setRejected(new ChargebackRejected().setLevyAmount(1000L)); //status - rejected
        skipperService.processChargebackData(
                createChargebackStatusChangeTestEvent(invoiceId, paymentId, chargebackId, stage_1, status_1)
        );

        ChargebackData chargebackData = skipperService.getChargebackData(invoiceId, paymentId, chargebackId);
        assertEquals("Count of events aren't equal to expected", 4, chargebackData.getEvents().size());
        List<ChargebackEvent> chargebackEvents = chargebackData.getEvents().stream()
                .filter(ChargebackEvent::isSetStatusChangeEvent)
                .collect(Collectors.toList());
        assertEquals("Count of status change events aren't equal to expected", 2, chargebackEvents.size());

        ChargebackStage reopenStage = new ChargebackStage();
        reopenStage.setArbitration(new StageArbitration());
        skipperService.processChargebackData(
                createChargebackStatusChangeTestEvent(invoiceId, paymentId, chargebackId, reopenStage)
        );
        ChargebackData chargebackDataAfterReopen = skipperService.getChargebackData(invoiceId, paymentId, chargebackId);
        assertEquals("Count of events after reopen aren't equal to expected",
                5, chargebackDataAfterReopen.getEvents().size());
        List<ChargebackEvent> chargebackEventsAfterReopen = chargebackDataAfterReopen.getEvents().stream()
                .filter(ChargebackEvent::isSetReopenEvent)
                .collect(Collectors.toList());
        assertEquals("Count of reopen events aren't equal to expected", 0, chargebackEventsAfterReopen.size());
    }

    @Test
    public void changeChargebackHoldStatusTest() throws TException {
        String invoiceId = "inv_3";
        String paymentId = "pay_3";
        String chargebackId = "ch_1";
        String providerId = "pr_1";
        skipperService.processChargebackData(createChargebackTestEvent(invoiceId, paymentId, chargebackId, providerId, false));

        ChargebackStage stage = new ChargebackStage();
        stage.setChargeback(new StageChargeback());                        //stage - chargeback
        ChargebackStatus status = new ChargebackStatus();
        status.setAccepted(new ChargebackAccepted().setLevyAmount(1000L)); //status - acceptef
        skipperService.processChargebackData(createChargebackStatusChangeTestEvent(invoiceId, paymentId, chargebackId, stage, status));

        skipperService.processChargebackData(createChargebackHoldStatusChangeTestEvent(invoiceId, paymentId, chargebackId));
        ChargebackData chargebackData = skipperService.getChargebackData(invoiceId, paymentId, chargebackId);
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
            createTestChargebackFlowData("inv_10" + i, "pay_1", "ch_1", "prov_1", status);
        }
        for (int i = 0; i < 5; i++) {
            ChargebackStatus status = new ChargebackStatus();
            status.setRejected(new ChargebackRejected().setLevyAmount(900L));
            createTestChargebackFlowData("inv_20" + i, "ch_1", "pay_1", "prov_2", status);
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
                                              String chargebackId,
                                              String providerId,
                                              ChargebackStatus status) throws TException {
        skipperService.processChargebackData(
                createChargebackTestEvent(invoiceId, paymentId, chargebackId, providerId, false)
        );

        ChargebackStage stage = new ChargebackStage();
        skipperService.processChargebackData(createChargebackStatusChangeTestEvent(invoiceId, paymentId, chargebackId, stage, status));
        skipperService.processChargebackData(createChargebackHoldStatusChangeTestEvent(invoiceId, paymentId, chargebackId));
    }

}
