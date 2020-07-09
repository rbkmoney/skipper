package com.rbkmoney.skipper.util;

import com.rbkmoney.damsel.base.Content;
import com.rbkmoney.damsel.domain.Cash;
import com.rbkmoney.damsel.domain.CurrencyRef;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChargebackParams;
import com.rbkmoney.damsel.skipper.*;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.domain.tables.pojos.Chargeback;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackHoldState;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackState;
import com.rbkmoney.skipper.exception.UnsupportedCategoryException;
import com.rbkmoney.skipper.exception.UnsupportedStageException;
import com.rbkmoney.skipper.exception.UnsupportedStatusException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static com.rbkmoney.reporter.domain.enums.ChargebackCategory.*;
import static com.rbkmoney.reporter.domain.enums.ChargebackStage.*;
import static com.rbkmoney.reporter.domain.enums.ChargebackStatus.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MapperUtils {

    public static Chargeback transformToChargeback(ChargebackGeneralData creationData) {
        Chargeback chargeback = new Chargeback();
        chargeback.setInvoiceId(creationData.getInvoiceId());
        chargeback.setPaymentId(creationData.getPaymentId());
        chargeback.setRetrievalRequest(creationData.isRetrievalRequest());
        chargeback.setPretensionDate(TypeUtil.stringToLocalDateTime(creationData.getPretensionDate()));
        chargeback.setOperationDate(TypeUtil.stringToLocalDateTime(creationData.getOperationDate()));
        chargeback.setLevyAmount(creationData.getLevyAmount());
        chargeback.setBodyAmount(creationData.getBodyAmount());
        chargeback.setCurrency(creationData.getCurrency());
        chargeback.setShopId(creationData.getShopId());
        ChargebackReason chargebackReason = creationData.getChargebackReason();
        ChargebackCategory category = chargebackReason.getCategory();
        if (category.isSetAuthorisation()) {
            chargeback.setChargebackCategory(AUTHORISATION);
        } else if (category.isSetDispute()) {
            chargeback.setChargebackCategory(DISPUTE);
        } else if (category.isSetFraud()) {
            chargeback.setChargebackCategory(FRAUD);
        } else if (category.isSetProcessingError()) {
            chargeback.setChargebackCategory(PROCESSING_ERROR);
        } else {
            throw new UnsupportedCategoryException();
        }
        chargeback.setProviderId(creationData.getProviderId());
        chargeback.setReasonCode(chargebackReason.getCode());
        chargeback.setRrn(creationData.getRrn());
        chargeback.setMaskedPan(creationData.getMaskedPan());
        chargeback.setShopUrl(creationData.getShopUrl());
        chargeback.setPartyEmail(creationData.getPartyEmail());
        chargeback.setContactEmail(creationData.getContactEmail());
        chargeback.setContextType(creationData.getContent().getType());
        chargeback.setContext(creationData.getContent().getData());
        return chargeback;
    }

    public static ChargebackState transformToChargebackState(ChargebackStatusChangeEvent event,
                                                             long chargebackId) {
        ChargebackState state = new ChargebackState();
        state.setChargebackId(chargebackId);
        state.setInvoiceId(event.getInvoiceId());
        state.setPaymentId(event.getPaymentId());
        ChargebackStage stage = event.getStage();
        if (stage.isSetChargeback()) {
            state.setStage(CHARGEBACK);
        } else if (stage.isSetPreArbitration()) {
            state.setStage(PRE_ARBITRATION);
        } else if (stage.isSetArbitration()) {
            state.setStage(ARBITRATION);
        } else {
            throw new UnsupportedStageException();
        }
        ChargebackStatus status = event.getStatus();
        if (status.isSetPending()) {
            state.setStatus(PENDING);
        } else if (status.isSetAccepted()) {
            state.setStatus(ACCEPTED);
            ChargebackAccepted accepted = status.getAccepted();
            state.setLevyAmount(accepted.getLevyAmount());
            state.setBodyAmount(accepted.getBodyAmount());
        } else if (status.isSetRejected()) {
            state.setStatus(REJECTED);
            state.setLevyAmount(status.getRejected().getLevyAmount());
        } else if (status.isSetCancelled()) {
            state.setStatus(CANCELLED);
        } else {
            throw new UnsupportedStatusException();
        }

        state.setCreatedAt(TypeUtil.stringToLocalDateTime(event.getCreatedAt()));
        state.setDateOfDecision(TypeUtil.stringToLocalDateTime(event.getDateOfDecision()));
        return state;
    }

    public static ChargebackHoldState transformChargebackHoldState(ChargebackHoldStatusChangeEvent event,
                                                                   long chargebackId) {
        ChargebackHoldState holdState = new ChargebackHoldState();
        holdState.setChargebackId(chargebackId);
        holdState.setInvoiceId(event.getInvoiceId());
        holdState.setPaymentId(event.getPaymentId());
        holdState.setCreatedAt(TypeUtil.stringToLocalDateTime(event.getCreatedAt()));
        holdState.setWillHoldFromMerchant(event.isWillHoldFromMerchant());
        holdState.setWasHoldFromMerchant(event.isWasHoldFromMerchant());
        holdState.setHoldFromUs(event.isHoldFromUs());
        return holdState;
    }

    public static InvoicePaymentChargebackParams transformToInvoicePaymentChargebackParams(
            ChargebackGeneralData creationData,
            long chargebackId
    ) {
        InvoicePaymentChargebackParams params = new InvoicePaymentChargebackParams();
        params.setId(String.valueOf(chargebackId));
        params.setExternalId(creationData.getExternalId());
        params.setBody(creationData.getBodyAmount() != 0 ?
                new Cash()
                        .setAmount(creationData.getBodyAmount())
                        .setCurrency(new CurrencyRef().setSymbolicCode(creationData.getCurrency())) : null);
        params.setLevy(creationData.getLevyAmount() != 0 ?
                new Cash()
                        .setAmount(creationData.getLevyAmount())
                        .setCurrency(new CurrencyRef().setSymbolicCode(creationData.getCurrency())) : null);
        var content = creationData.getContent();
        params.setContext(content == null ?
                null : new Content().setType(content.getType()).setData(content.getData()));
        return params;
    }

    public static ChargebackData transformToChargebackData(Chargeback chargeback,
                                                           List<ChargebackState> chargebackStates,
                                                           List<ChargebackHoldState> chargebackHoldStates) {
        ChargebackData data = new ChargebackData();
        data.setId(String.valueOf(chargeback.getId()));
        List<ChargebackEvent> events = new ArrayList<>();
        events.add(transformToGeneralDataEvent(chargeback));
        for (ChargebackState chargebackState : chargebackStates) {
            events.add(transformToChargebackStatusChangeEvent(chargebackState));
        }
        for (ChargebackHoldState chargebackHoldState : chargebackHoldStates) {
            events.add(transformToChargebackHoldStatusChangeEvent(chargebackHoldState));
        }
        data.setEvents(events);
        return data;
    }

    private static ChargebackEvent transformToGeneralDataEvent(Chargeback chargeback) {
        ChargebackEvent event = new ChargebackEvent();
        ChargebackCreateEvent createEvent = new ChargebackCreateEvent();
        ChargebackGeneralData generalData = new ChargebackGeneralData();

        generalData.setPretensionDate(chargeback.getPretensionDate().toInstant(ZoneOffset.UTC).toString());
        generalData.setProviderId(chargeback.getProviderId());
        generalData.setOperationDate(chargeback.getOperationDate().toInstant(ZoneOffset.UTC).toString());
        generalData.setInvoiceId(chargeback.getInvoiceId());
        generalData.setPaymentId(chargeback.getPaymentId());
        generalData.setRrn(chargeback.getRrn());
        generalData.setMaskedPan(chargeback.getMaskedPan());
        generalData.setLevyAmount(chargeback.getLevyAmount());
        generalData.setBodyAmount(chargeback.getBodyAmount());
        generalData.setCurrency(chargeback.getCurrency());
        generalData.setShopId(chargeback.getShopId());
        generalData.setPartyEmail(chargeback.getPartyEmail());
        generalData.setContactEmail(chargeback.getContactEmail());
        generalData.setShopId(chargeback.getShopId());
        generalData.setExternalId(chargeback.getExternalId());
        ChargebackReason reason = new ChargebackReason();
        ChargebackCategory category = new ChargebackCategory();
        switch (chargeback.getChargebackCategory()) {
            case FRAUD:
                category.setFraud(new ChargebackCategoryFraud());
                break;
            case DISPUTE:
                category.setDispute(new ChargebackCategoryDispute());
                break;
            case AUTHORISATION:
                category.setAuthorisation(new ChargebackCategoryAuthorisation());
                break;
            case PROCESSING_ERROR:
                category.setProcessingError(new ChargebackCategoryProcessingError());
                break;
            default:
                throw new UnsupportedCategoryException();
        }
        reason.setCategory(category);
        reason.setCode(chargeback.getReasonCode());
        generalData.setChargebackReason(reason);
        var content = new com.rbkmoney.damsel.skipper.Content();
        content.setData(content.getData());
        content.setType(content.getType());
        generalData.setContent(content);
        generalData.setRetrievalRequest(chargeback.getRetrievalRequest());

        createEvent.setCreationData(generalData);
        event.setCreateEvent(createEvent);
        return event;
    }

    public static ChargebackEvent transformToChargebackStatusChangeEvent(ChargebackState chargeback) {
        ChargebackEvent event = new ChargebackEvent();
        ChargebackStatusChangeEvent statusChangeEvent = new ChargebackStatusChangeEvent();
        statusChangeEvent.setInvoiceId(chargeback.getInvoiceId());
        statusChangeEvent.setPaymentId(chargeback.getPaymentId());
        var stage = chargeback.getStage();
        ChargebackStage chargebackStage = new ChargebackStage();
        switch (stage) {
            case CHARGEBACK:
                chargebackStage.setChargeback(new StageChargeback());
                break;
            case PRE_ARBITRATION:
                chargebackStage.setPreArbitration(new StagePreArbitration());
                break;
            case ARBITRATION:
                chargebackStage.setArbitration(new StageArbitration());
                break;
            default:
                throw new UnsupportedStageException();
        }
        statusChangeEvent.setStage(chargebackStage);

        ChargebackStatus chargebackStatus = new ChargebackStatus();
        var status = chargeback.getStatus();
        switch (status) {
            case PENDING:
                chargebackStatus.setPending(new ChargebackPending());
                break;
            case ACCEPTED:
                ChargebackAccepted accepted = new ChargebackAccepted();
                accepted.setBodyAmount(chargeback.getBodyAmount());
                accepted.setLevyAmount(chargeback.getLevyAmount());
                chargebackStatus.setAccepted(accepted);
                break;
            case CANCELLED:
                chargebackStatus.setCancelled(new ChargebackCancelled());
                break;
            case REJECTED:
                ChargebackRejected rejected = new ChargebackRejected();
                rejected.setLevyAmount(chargeback.getLevyAmount());
                rejected.setBodyAmount(chargeback.getBodyAmount());
                chargebackStatus.setRejected(rejected);
                break;
            default:
                throw new UnsupportedStatusException();
        }
        statusChangeEvent.setStatus(chargebackStatus);

        statusChangeEvent.setCreatedAt(chargeback.getCreatedAt().toInstant(ZoneOffset.UTC).toString());
        statusChangeEvent.setDateOfDecision(chargeback.getDateOfDecision().toInstant(ZoneOffset.UTC).toString());
        event.setStatusChangeEvent(statusChangeEvent);
        return event;
    }

    public static ChargebackEvent transformToChargebackHoldStatusChangeEvent(ChargebackHoldState chargeback) {
        ChargebackEvent event = new ChargebackEvent();
        ChargebackHoldStatusChangeEvent holdStatusChangeEvent = new ChargebackHoldStatusChangeEvent();
        holdStatusChangeEvent.setInvoiceId(chargeback.getInvoiceId());
        holdStatusChangeEvent.setPaymentId(chargeback.getPaymentId());
        holdStatusChangeEvent.setCreatedAt(chargeback.getCreatedAt().toInstant(ZoneOffset.UTC).toString());
        holdStatusChangeEvent.setWillHoldFromMerchant(chargeback.getWillHoldFromMerchant());
        holdStatusChangeEvent.setWasHoldFromMerchant(chargeback.getWasHoldFromMerchant());
        holdStatusChangeEvent.setHoldFromUs(chargeback.getHoldFromUs());
        event.setHoldStatusChangeEvent(holdStatusChangeEvent);
        return event;
    }

}
