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
import com.rbkmoney.skipper.model.SearchFilter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static com.rbkmoney.reporter.domain.enums.ChargebackCategory.*;
import static com.rbkmoney.reporter.domain.enums.ChargebackStage.*;
import static com.rbkmoney.reporter.domain.enums.ChargebackStatus.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MapperUtils {

    public static Chargeback mapToChargeback(ChargebackGeneralData creationData) {
        Chargeback chargeback = new Chargeback();
        chargeback.setInvoiceId(creationData.getInvoiceId());
        chargeback.setPaymentId(creationData.getPaymentId());
        chargeback.setChargebackId(creationData.getChargebackId());
        chargeback.setRetrievalRequest(creationData.isRetrievalRequest());
        chargeback.setPretensionDate(TypeUtil.stringToLocalDateTime(creationData.getPretensionDate()));
        chargeback.setOperationDate(TypeUtil.stringToLocalDateTime(creationData.getOperationDate()));
        chargeback.setLevyAmount(creationData.getLevyAmount());
        if (creationData.isSetBodyAmount()) {
            chargeback.setBodyAmount(creationData.getBodyAmount());
        }
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
        var content = creationData.getContent();
        if (content != null) {
            chargeback.setContextType(content.getType());
            chargeback.setContext(content.getData());
        }
        return chargeback;
    }

    public static ChargebackState mapToChargebackState(ChargebackStatusChangeEvent event,
                                                       ChargebackState prevState,
                                                       long extId) {
        ChargebackState state = new ChargebackState();
        state.setExtId(extId);
        state.setInvoiceId(event.getInvoiceId());
        state.setPaymentId(event.getPaymentId());
        state.setChargebackId(event.getChargebackId());
        state.setStage(prevState.getStage());
        //state.setBodyAmount(prevState.getBodyAmount());
        //state.setLevyAmount(prevState.getLevyAmount());
        ChargebackStatus status = event.getStatus();
        if (status.isSetPending()) {
            state.setStatus(PENDING);
        } else if (status.isSetAccepted()) {
            state.setStatus(ACCEPTED);
            ChargebackAccepted accepted = status.getAccepted();
            if (accepted.isSetLevyAmount()) {
                state.setLevyAmount(accepted.getLevyAmount());
            }
            if (accepted.isSetBodyAmount()) {
                state.setBodyAmount(accepted.getBodyAmount());
            }
        } else if (status.isSetRejected()) {
            state.setStatus(REJECTED);
            ChargebackRejected rejected = status.getRejected();
            if (rejected.isSetLevyAmount()) {
                state.setLevyAmount(rejected.getLevyAmount());
            }
        } else if (status.isSetCancelled()) {
            state.setStatus(CANCELLED);
        } else {
            throw new UnsupportedStatusException();
        }

        state.setCreatedAt(TypeUtil.stringToLocalDateTime(event.getCreatedAt()));
        if (event.getDateOfDecision() != null) {
            state.setDateOfDecision(TypeUtil.stringToLocalDateTime(event.getDateOfDecision()));
        }
        return state;
    }

    public static ChargebackState transformReopenToChargebackState(ChargebackReopenEvent event,
                                                                   ChargebackState prevState,
                                                                   long extId) {
        ChargebackState state = new ChargebackState();
        state.setExtId(extId);
        state.setInvoiceId(event.getInvoiceId());
        state.setPaymentId(event.getPaymentId());
        state.setChargebackId(event.getChargebackId());
        state.setStatus(PENDING);
        state.setCreatedAt(TypeUtil.stringToLocalDateTime(event.getCreatedAt()));

        if (event.isSetLevyAmount()) {
            state.setLevyAmount(event.getLevyAmount());
        } else {
            state.setLevyAmount(prevState.getLevyAmount());
        }
        if (event.isSetBodyAmount()) {
            state.setBodyAmount(event.getBodyAmount());
        } else {
            state.setBodyAmount(prevState.getBodyAmount());
        }
        if (!event.isSetReopenStage()) {
            var chargebackStage = prevState.getStage();
            if (chargebackStage == CHARGEBACK) {
                state.setStage(PRE_ARBITRATION);
            } else {
                state.setStage(ARBITRATION);
            }
        } else {
            ChargebackStage reopenStage = event.getReopenStage();
            if (reopenStage.isSetPreArbitration()) {
                state.setStage(PRE_ARBITRATION);
            } else if (reopenStage.isSetArbitration()) {
                state.setStage(ARBITRATION);
            } else {
                throw new UnsupportedStageException();
            }
        }

        return state;
    }

    public static ChargebackHoldState mapToChargebackHoldState(ChargebackHoldStatusChangeEvent event,
                                                               long extId) {
        ChargebackHoldState holdState = new ChargebackHoldState();
        holdState.setExtId(extId);
        holdState.setInvoiceId(event.getInvoiceId());
        holdState.setPaymentId(event.getPaymentId());
        holdState.setChargebackId(event.getChargebackId());
        holdState.setCreatedAt(TypeUtil.stringToLocalDateTime(event.getCreatedAt()));
        ChargebackHoldStatus holdStatus = event.getHoldStatus();
        holdState.setWillHoldFromMerchant(holdStatus.isWillHoldFromMerchant());
        holdState.setWasHoldFromMerchant(holdStatus.isWasHoldFromMerchant());
        holdState.setHoldFromUs(holdStatus.isHoldFromUs());
        return holdState;
    }

    public static InvoicePaymentChargebackParams mapToInvoicePaymentChargebackParams(
            ChargebackGeneralData creationData
    ) {
        InvoicePaymentChargebackParams params = new InvoicePaymentChargebackParams();
        params.setId(creationData.getChargebackId());
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

    public static ChargebackData mapToChargebackData(Chargeback chargeback,
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

    public static ChargebackEvent transformToGeneralDataEvent(Chargeback chargeback) {
        ChargebackEvent event = new ChargebackEvent();
        ChargebackCreateEvent createEvent = new ChargebackCreateEvent();
        ChargebackGeneralData generalData = new ChargebackGeneralData();

        generalData.setPretensionDate(localDateTimeToString(chargeback.getPretensionDate()));
        generalData.setProviderId(chargeback.getProviderId());
        generalData.setOperationDate(localDateTimeToString(chargeback.getOperationDate()));
        generalData.setInvoiceId(chargeback.getInvoiceId());
        generalData.setPaymentId(chargeback.getPaymentId());
        generalData.setChargebackId(chargeback.getChargebackId());
        generalData.setRrn(chargeback.getRrn());
        generalData.setMaskedPan(chargeback.getMaskedPan());
        generalData.setLevyAmount(chargeback.getLevyAmount());
        if (chargeback.getBodyAmount() != null) {
            generalData.setBodyAmount(chargeback.getBodyAmount());
        }
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
        generalData.setContent(content.getType() == null ? null : content);
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
        statusChangeEvent.setChargebackId(chargeback.getChargebackId());
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
                if (chargeback.getLevyAmount() != null) {
                    accepted.setLevyAmount(chargeback.getLevyAmount());
                }
                if (chargeback.getBodyAmount() != null) {
                    accepted.setBodyAmount(chargeback.getBodyAmount());
                }
                chargebackStatus.setAccepted(accepted);
                break;
            case CANCELLED:
                chargebackStatus.setCancelled(new ChargebackCancelled());
                break;
            case REJECTED:
                ChargebackRejected rejected = new ChargebackRejected();
                if (chargeback.getLevyAmount() != null) {
                    rejected.setLevyAmount(chargeback.getLevyAmount());
                }
                if (chargeback.getBodyAmount() != null) {
                    rejected.setBodyAmount(chargeback.getBodyAmount());
                }
                chargebackStatus.setRejected(rejected);
                break;
            default:
                throw new UnsupportedStatusException();
        }
        statusChangeEvent.setStatus(chargebackStatus);

        statusChangeEvent.setCreatedAt(localDateTimeToString(chargeback.getCreatedAt()));
        if (chargeback.getDateOfDecision() != null) {
            statusChangeEvent.setDateOfDecision(localDateTimeToString(chargeback.getDateOfDecision()));
        }
        event.setStatusChangeEvent(statusChangeEvent);
        return event;
    }

    public static ChargebackEvent transformToChargebackHoldStatusChangeEvent(ChargebackHoldState chargeback) {
        ChargebackEvent event = new ChargebackEvent();
        ChargebackHoldStatusChangeEvent holdStatusChangeEvent = new ChargebackHoldStatusChangeEvent();
        holdStatusChangeEvent.setInvoiceId(chargeback.getInvoiceId());
        holdStatusChangeEvent.setPaymentId(chargeback.getPaymentId());
        holdStatusChangeEvent.setChargebackId(chargeback.getChargebackId());
        holdStatusChangeEvent.setCreatedAt(localDateTimeToString(chargeback.getCreatedAt()));
        ChargebackHoldStatus holdStatus = new ChargebackHoldStatus();
        holdStatus.setWillHoldFromMerchant(chargeback.getWillHoldFromMerchant());
        holdStatus.setWasHoldFromMerchant(chargeback.getWasHoldFromMerchant());
        holdStatus.setHoldFromUs(chargeback.getHoldFromUs());
        holdStatusChangeEvent.setHoldStatus(holdStatus);
        event.setHoldStatusChangeEvent(holdStatusChangeEvent);
        return event;
    }

    public static List<com.rbkmoney.reporter.domain.enums.ChargebackStage> transformChargebackStages(
            List<ChargebackStage> stages
    ) {
        List<com.rbkmoney.reporter.domain.enums.ChargebackStage> chargebackStageList = new ArrayList<>();
        for (ChargebackStage stage : stages) {
            chargebackStageList.add(transformChargebackStage(stage));
        }
        return chargebackStageList;
    }

    public static com.rbkmoney.reporter.domain.enums.ChargebackStage transformChargebackStage(ChargebackStage stage) {
        if (stage.isSetChargeback()) {
            return CHARGEBACK;
        } else if (stage.isSetPreArbitration()) {
            return PRE_ARBITRATION;
        } else if (stage.isSetArbitration()) {
            return ARBITRATION;
        } else {
            throw new UnsupportedStageException();
        }
    }

    public static List<com.rbkmoney.reporter.domain.enums.ChargebackStatus> transformChargebackStatuses(
            List<ChargebackStatus> statuses
    ) {
        List<com.rbkmoney.reporter.domain.enums.ChargebackStatus> chargebackStatuses = new ArrayList<>();
        for (ChargebackStatus status : statuses) {
            chargebackStatuses.add(transformChargebackStatus(status));
        }
        return chargebackStatuses;
    }

    public static com.rbkmoney.reporter.domain.enums.ChargebackStatus transformChargebackStatus(
            ChargebackStatus status
    ) {
        if (status.isSetAccepted()) {
            return ACCEPTED;
        } else if (status.isSetRejected()) {
            return REJECTED;
        } else if (status.isSetCancelled()) {
            return CANCELLED;
        } else if (status.isSetPending()) {
            return PENDING;
        } else {
            throw new UnsupportedStatusException();
        }
    }

    public static SearchFilter mapToSearchFilter(ChargebackFilter chargebackFilter) {
        SearchFilter searchFilter = new SearchFilter();
        searchFilter.setDateFrom(TypeUtil.stringToLocalDateTime(chargebackFilter.getDateFrom()));
        searchFilter.setDateTo(chargebackFilter.getDateTo() == null ?
                null : TypeUtil.stringToLocalDateTime(chargebackFilter.getDateTo()));
        searchFilter.setProviderId(chargebackFilter.getProviderId());
        searchFilter.setStatuses(chargebackFilter.getStatuses() == null ?
                null : transformChargebackStatuses(chargebackFilter.getStatuses()));
        searchFilter.setStages(chargebackFilter.getStages() == null ?
                null : transformChargebackStages(chargebackFilter.getStages()));
        return searchFilter;
    }

    public static String localDateTimeToString(LocalDateTime dateTime) {
        return dateTime.toInstant(ZoneOffset.UTC).toString();
    }

}
