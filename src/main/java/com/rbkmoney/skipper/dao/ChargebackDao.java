package com.rbkmoney.skipper.dao;

import com.rbkmoney.reporter.domain.tables.pojos.Chargeback;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackHoldState;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackState;
import com.rbkmoney.skipper.model.SearchFilter;

import java.util.List;

public interface ChargebackDao {

    long saveChargeback(Chargeback chargeback);

    Chargeback getChargeback(long chargebackId);

    Chargeback getChargeback(String invoiceId, String paymentId, boolean isRetrieval);

    List<Chargeback> getChargebacks(SearchFilter searchFilter);

    void saveChargebackState(ChargebackState state);

    List<ChargebackState> getChargebackStates(long chargebackId);

    List<ChargebackState> getChargebackStates(String invoiceId, String paymentId);

    void saveChargebackHoldState(ChargebackHoldState holdState);

    List<ChargebackHoldState> getChargebackHoldStates(long chargebackId);

    List<ChargebackHoldState> getChargebackHoldStates(String invoiceId, String paymentId);

}
