package com.rbkmoney.skipper.dao.impl;

import com.rbkmoney.reporter.domain.tables.pojos.Chargeback;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackHoldState;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackState;
import com.rbkmoney.reporter.domain.tables.records.ChargebackHoldStateRecord;
import com.rbkmoney.reporter.domain.tables.records.ChargebackRecord;
import com.rbkmoney.reporter.domain.tables.records.ChargebackStateRecord;
import com.rbkmoney.skipper.dao.AbstractGenericDao;
import com.rbkmoney.skipper.dao.ChargebackDao;
import com.rbkmoney.skipper.dao.RecordRowMapper;
import com.rbkmoney.skipper.model.SearchFilter;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.rbkmoney.reporter.domain.Tables.*;

@Slf4j
@Component
public class ChargebackDaoImpl extends AbstractGenericDao implements ChargebackDao {

    private final RowMapper<Chargeback> chargebackRowMapper;

    private final RowMapper<ChargebackState> chargebackStateRowMapper;

    private final RowMapper<ChargebackHoldState> chargebackHoldStateRowMapper;

    public ChargebackDaoImpl(HikariDataSource dataSource) {
        super(dataSource);
        chargebackRowMapper = new RecordRowMapper<>(CHARGEBACK, Chargeback.class);
        chargebackStateRowMapper = new RecordRowMapper<>(CHARGEBACK_STATE, ChargebackState.class);
        chargebackHoldStateRowMapper = new RecordRowMapper<>(CHARGEBACK_HOLD_STATE, ChargebackHoldState.class);
    }

    @Override
    public long saveChargeback(Chargeback chargeback) {
        ChargebackRecord record = getDslContext().newRecord(CHARGEBACK, chargeback);
        Query query = getDslContext()
                .insertInto(CHARGEBACK)
                .set(record)
                .onConflict(CHARGEBACK.INVOICE_ID, CHARGEBACK.PAYMENT_ID, CHARGEBACK.RETRIEVAL_REQUEST)
                .doUpdate()
                .set(record)
                .returning(CHARGEBACK.ID);

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        executeWithReturn(query, keyHolder);
        return keyHolder.getKey().longValue();
    }

    @Override
    public Chargeback getChargeback(long chargebackId) {
        Query query = getDslContext()
                .selectFrom(CHARGEBACK)
                .where(CHARGEBACK.ID.eq(chargebackId));
        return fetchOne(query, chargebackRowMapper);
    }

    @Override
    public Chargeback getChargeback(String invoiceId, String paymentId, boolean isRetrieval) {
        Query query = getDslContext()
                .selectFrom(CHARGEBACK)
                .where(CHARGEBACK.INVOICE_ID.eq(invoiceId))
                .and(CHARGEBACK.PAYMENT_ID.eq(paymentId))
                .and(CHARGEBACK.RETRIEVAL_REQUEST.eq(isRetrieval));
        return fetchOne(query, chargebackRowMapper);
    }

    @Override
    public List<Chargeback> getChargebacks(SearchFilter searchFilter) {
        SelectConditionStep<Record> chargebackQuery;
        boolean isStagesEmpty = searchFilter.getStages() == null;
        boolean isStatusesEmpty = searchFilter.getStatuses() == null;
        if (!isStagesEmpty || !isStatusesEmpty) {
            if (!isStagesEmpty && isStatusesEmpty) {
                chargebackQuery = getDslContext()
                        .select(CHARGEBACK.fields())
                        .from(CHARGEBACK)
                        .join(CHARGEBACK_STATE).on(CHARGEBACK_STATE.CHARGEBACK_ID.eq(CHARGEBACK.ID))
                        .and(CHARGEBACK_STATE.STAGE.in(searchFilter.getStages()))
                        .where(CHARGEBACK.PRETENSION_DATE.greaterOrEqual(searchFilter.getDateFrom()));
            } else if (isStagesEmpty && !isStatusesEmpty) {
                chargebackQuery = getDslContext()
                        .select(CHARGEBACK.fields())
                        .from(CHARGEBACK)
                        .join(CHARGEBACK_STATE).on(CHARGEBACK_STATE.CHARGEBACK_ID.eq(CHARGEBACK.ID))
                        .and(CHARGEBACK_STATE.STATUS.in(searchFilter.getStatuses()))
                        .where(CHARGEBACK.PRETENSION_DATE.greaterOrEqual(searchFilter.getDateFrom()));
            } else {
                chargebackQuery = getDslContext()
                        .select(CHARGEBACK.fields())
                        .from(CHARGEBACK)
                        .join(CHARGEBACK_STATE).on(CHARGEBACK_STATE.CHARGEBACK_ID.eq(CHARGEBACK.ID))
                        .and(CHARGEBACK_STATE.STAGE.in(searchFilter.getStages()))
                        .and(CHARGEBACK_STATE.STATUS.in(searchFilter.getStatuses()))
                        .where(CHARGEBACK.PRETENSION_DATE.greaterOrEqual(searchFilter.getDateFrom()));
            }
        } else {
            chargebackQuery = getDslContext()
                    .select()
                    .from(CHARGEBACK)
                    .where(CHARGEBACK.PRETENSION_DATE.greaterOrEqual(searchFilter.getDateFrom()));
        }

        if (searchFilter.getDateTo() != null) {
            chargebackQuery.and(CHARGEBACK.PRETENSION_DATE.lessOrEqual(searchFilter.getDateTo()));
        }
        if (searchFilter.getProviderId() != null) {
            chargebackQuery.and(CHARGEBACK.PROVIDER_ID.eq(searchFilter.getProviderId()));
        }

        return fetch(chargebackQuery, chargebackRowMapper);
    }

    @Override
    public void saveChargebackState(ChargebackState state) {
        ChargebackStateRecord record = getDslContext().newRecord(CHARGEBACK_STATE, state);
        Query query = getDslContext()
                .insertInto(CHARGEBACK_STATE)
                .set(record);
        execute(query);
    }

    @Override
    public List<ChargebackState> getChargebackStates(long chargebackId) {
        Query query = getDslContext()
                .selectFrom(CHARGEBACK_STATE)
                .where(CHARGEBACK_STATE.CHARGEBACK_ID.eq(chargebackId));
        return fetch(query, chargebackStateRowMapper);
    }

    @Override
    public List<ChargebackState> getChargebackStates(String invoiceId, String paymentId) {
        Query query = getDslContext()
                .selectFrom(CHARGEBACK_STATE)
                .where(CHARGEBACK_STATE.INVOICE_ID.eq(invoiceId))
                .and(CHARGEBACK_STATE.PAYMENT_ID.eq(paymentId))
                .orderBy(CHARGEBACK_STATE.CREATED_AT.desc());
        return fetch(query, chargebackStateRowMapper);
    }

    @Override
    public void saveChargebackHoldState(ChargebackHoldState holdState) {
        ChargebackHoldStateRecord record = getDslContext().newRecord(CHARGEBACK_HOLD_STATE, holdState);
        Query query = getDslContext()
                .insertInto(CHARGEBACK_HOLD_STATE)
                .set(record);
        execute(query);
    }

    @Override
    public List<ChargebackHoldState> getChargebackHoldStates(long chargebackId) {
        Query query = getDslContext()
                .selectFrom(CHARGEBACK_HOLD_STATE)
                .where(CHARGEBACK_HOLD_STATE.CHARGEBACK_ID.eq(chargebackId));
        return fetch(query, chargebackHoldStateRowMapper);
    }

    @Override
    public List<ChargebackHoldState> getChargebackHoldStates(String invoiceId, String paymentId) {
        Query query = getDslContext()
                .selectFrom(CHARGEBACK_HOLD_STATE)
                .where(CHARGEBACK_HOLD_STATE.INVOICE_ID.eq(invoiceId))
                .and(CHARGEBACK_HOLD_STATE.PAYMENT_ID.eq(paymentId));
        return fetch(query, chargebackHoldStateRowMapper);
    }
}
