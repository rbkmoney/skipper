package com.rbkmoney.skipper.dao;

import com.rbkmoney.reporter.domain.tables.pojos.Chargeback;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackHoldState;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackState;
import com.rbkmoney.skipper.model.SearchFilter;

import java.util.List;

public interface ChargebackDao {

    long saveChargeback(Chargeback chargeback);

    Chargeback getChargeback(long id);

    Chargeback getChargeback(String invoiceId, String paymentId, String chargebackId, boolean isRetrieval);

    List<Chargeback> getChargebacks(SearchFilter searchFilter);

    void saveChargebackState(ChargebackState state);

    List<ChargebackState> getChargebackStates(long extId);

    List<ChargebackState> getChargebackStates(String invoiceId, String paymentId, String chargebackId);

    void saveChargebackHoldState(ChargebackHoldState holdState);

    List<ChargebackHoldState> getChargebackHoldStates(long extId);

    List<ChargebackHoldState> getChargebackHoldStates(String invoiceId, String paymentId, String chargebackId);

}
