package com.rbkmoney.skipper.integration;

import com.rbkmoney.damsel.domain.InvoicePaymentChargeback;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChargebackParams;
import com.rbkmoney.damsel.payment_processing.InvoicingSrv;
import com.rbkmoney.damsel.payment_processing.UserInfo;
import com.rbkmoney.damsel.skipper.*;
import com.rbkmoney.skipper.exception.BusinessException;
import com.rbkmoney.skipper.exception.DaoException;
import com.rbkmoney.skipper.exception.NotFoundException;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.rbkmoney.skipper.util.ChargebackTestUtils.createChargebackStatusChangeTestEvent;
import static com.rbkmoney.skipper.util.ChargebackTestUtils.createChargebackTestEvent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class FailureBehaviorIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SkipperSrv.Iface skipperService;

    @MockBean
    private InvoicingSrv.Iface invoicingService;

    @Before
    public void setUp() throws TException {
        when(invoicingService.createChargeback(any(UserInfo.class), any(String.class), any(String.class),
                any(InvoicePaymentChargebackParams.class)))
                .thenReturn(new InvoicePaymentChargeback());
    }

    @Test(expected = DaoException.class)
    public void duplicateChargebackErrorTest() throws TException {
        String invoiceId = "inv_1", paymentId = "pay_1", chargebackId = "ch_1", providerId = "pr_1";

        skipperService.processChargebackData(createChargebackTestEvent(invoiceId, paymentId, chargebackId, providerId, false));
        skipperService.processChargebackData(createChargebackTestEvent(invoiceId, paymentId, chargebackId, providerId, false));
    }

    @Test(expected = NotFoundException.class)
    public void sourceChargebackNotFoundErrorTest() throws TException {
        String invoiceId = "inv_1", paymentId = "pay_1", chargebackId = "ch_1";

        ChargebackStage stage = new ChargebackStage();
        stage.setChargeback(new StageChargeback());                        //stage - chargeback
        ChargebackStatus status = new ChargebackStatus();
        status.setAccepted(new ChargebackAccepted().setLevyAmount(1000L)); //status - acceptef
        skipperService.processChargebackData(createChargebackStatusChangeTestEvent(invoiceId, paymentId, chargebackId, stage, status));
    }

    @Test(expected = BusinessException.class)
    public void duplicateStatusChangeErrorTest() throws TException {
        String invoiceId = "inv_1", paymentId = "pay_1", chargebackId = "ch_1", providerId = "pr_1";

        skipperService.processChargebackData(
                createChargebackTestEvent(invoiceId, paymentId, chargebackId, providerId, false)
        );

        ChargebackStage stage = new ChargebackStage();
        stage.setChargeback(new StageChargeback());                        //stage - chargeback
        ChargebackStatus status = new ChargebackStatus();
        status.setAccepted(new ChargebackAccepted().setLevyAmount(1000L)); //status - accept
        skipperService.processChargebackData(
                createChargebackStatusChangeTestEvent(invoiceId, paymentId, chargebackId, stage, status)
        );

        ChargebackStatus duplicateStatus = new ChargebackStatus();
        duplicateStatus.setRejected(new ChargebackRejected().setLevyAmount(1000L));
        skipperService.processChargebackData(
                createChargebackStatusChangeTestEvent(invoiceId, paymentId, chargebackId, stage, duplicateStatus)
        );
    }

    @Test(expected = BusinessException.class)
    public void reopenToPreviousStageErrorTest() throws TException {
        String invoiceId = "inv_1", paymentId = "pay_1", chargebackId = "ch_1", providerId = "pr_1";

        skipperService.processChargebackData(
                createChargebackTestEvent(invoiceId, paymentId, chargebackId, providerId, false)
        );

        ChargebackStage stage = new ChargebackStage();
        stage.setChargeback(new StageChargeback());                        //stage - chargeback
        ChargebackStatus status = new ChargebackStatus();
        status.setRejected(new ChargebackRejected().setLevyAmount(1000L)); //status - accept
        skipperService.processChargebackData(
                createChargebackStatusChangeTestEvent(invoiceId, paymentId, chargebackId, stage, status)
        );

        ChargebackStage reopenStage = new ChargebackStage();
        reopenStage.setPreArbitration(new StagePreArbitration());
        skipperService.processChargebackData(
                createChargebackStatusChangeTestEvent(invoiceId, paymentId, chargebackId, reopenStage)
        );
        skipperService.processChargebackData(
                createChargebackStatusChangeTestEvent(invoiceId, paymentId, chargebackId, stage, status)
        );
        skipperService.processChargebackData(
                createChargebackStatusChangeTestEvent(invoiceId, paymentId, chargebackId, reopenStage)
        );
    }

    @Test(expected = BusinessException.class)
    public void reopenFromIncorrectStatusTest() throws TException {
        String invoiceId = "inv_1", paymentId = "pay_1", chargebackId = "ch_1", providerId = "pr_1";

        skipperService.processChargebackData(
                createChargebackTestEvent(invoiceId, paymentId, chargebackId, providerId, false)
        );

        ChargebackStage stage = new ChargebackStage();
        stage.setChargeback(new StageChargeback());                        //stage - chargeback
        ChargebackStatus status = new ChargebackStatus();
        status.setAccepted(new ChargebackAccepted().setLevyAmount(1000L)); //status - accept
        skipperService.processChargebackData(
                createChargebackStatusChangeTestEvent(invoiceId, paymentId, chargebackId, stage, status)
        );

        ChargebackStage reopenStage = new ChargebackStage();
        reopenStage.setPreArbitration(new StagePreArbitration());
        skipperService.processChargebackData(
                createChargebackStatusChangeTestEvent(invoiceId, paymentId, chargebackId, reopenStage)
        );
    }

}
