package com.rbkmoney.skipper.dao;

import com.rbkmoney.reporter.domain.enums.ChargebackCategory;
import com.rbkmoney.reporter.domain.enums.ChargebackStage;
import com.rbkmoney.reporter.domain.enums.ChargebackStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Chargeback;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackHoldState;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackState;

import java.time.LocalDateTime;
import java.util.List;

public interface ChargebackDao {

    long saveChargeback(Chargeback chargeback);

    Chargeback getChargeback(long chargebackId);

    Chargeback getChargeback(String invoiceId, String paymentId);

    List<Chargeback> getChargebacksByDate(LocalDateTime dateFrom, LocalDateTime dateTo);

    List<Chargeback> getChargebacksByProvider(String providerId, LocalDateTime dateFrom, LocalDateTime dateTo);

    List<Chargeback> getChargebacksByCategory(List<ChargebackCategory> categories,
                                              LocalDateTime dateFrom,
                                              LocalDateTime dateTo);

    List<Chargeback> getChargebacksByStep(LocalDateTime dateFrom,
                                          LocalDateTime dateTo,
                                          ChargebackStage stage,
                                          ChargebackStatus status);

    void saveChargebackState(ChargebackState state);

    List<ChargebackState> getChargebackStates(long chargebackId);

    List<ChargebackState> getChargebackStates(String invoiceId, String paymentId);

    void saveChargebackHoldState(ChargebackHoldState holdState);

    List<ChargebackHoldState> getChargebackHoldStates(long chargebackId);

    List<ChargebackHoldState> getChargebackHoldStates(String invoiceId, String paymentId);

}
