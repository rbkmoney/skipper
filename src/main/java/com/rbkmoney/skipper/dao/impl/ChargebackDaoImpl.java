package com.rbkmoney.skipper.dao.impl;

import com.rbkmoney.reporter.domain.enums.ChargebackCategory;
import com.rbkmoney.reporter.domain.enums.ChargebackStage;
import com.rbkmoney.reporter.domain.enums.ChargebackStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Chargeback;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackHoldState;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackState;
import com.rbkmoney.reporter.domain.tables.records.ChargebackHoldStateRecord;
import com.rbkmoney.reporter.domain.tables.records.ChargebackRecord;
import com.rbkmoney.reporter.domain.tables.records.ChargebackStateRecord;
import com.rbkmoney.skipper.dao.AbstractGenericDao;
import com.rbkmoney.skipper.dao.ChargebackDao;
import com.rbkmoney.skipper.dao.RecordRowMapper;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Query;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
                .set(record);

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
    public Chargeback getChargeback(String invoiceId, String paymentId) {
        Query query = getDslContext()
                .selectFrom(CHARGEBACK)
                .where(CHARGEBACK.INVOICE_ID.eq(invoiceId))
                .and(CHARGEBACK.PAYMENT_ID.eq(paymentId))
                .and(CHARGEBACK.RETRIEVAL_REQUEST.eq(false));
        return fetchOne(query, chargebackRowMapper);
    }

    @Override
    public List<Chargeback> getChargebacksByDate(LocalDateTime dateFrom, LocalDateTime dateTo) {
        Query query = getDslContext()
                .selectFrom(CHARGEBACK)
                .where(CHARGEBACK.PRETENSION_DATE.greaterOrEqual(dateFrom))
                .and(CHARGEBACK.PRETENSION_DATE.lessOrEqual(dateTo));
        return fetch(query, chargebackRowMapper);
    }

    @Override
    public List<Chargeback> getChargebacksByProvider(String providerId,
                                                     LocalDateTime dateFrom,
                                                     LocalDateTime dateTo) {
        Query query = getDslContext()
                .selectFrom(CHARGEBACK)
                .where(CHARGEBACK.PRETENSION_DATE.greaterOrEqual(dateFrom))
                .and(CHARGEBACK.PRETENSION_DATE.lessOrEqual(dateTo))
                .and(CHARGEBACK.PROVIDER_ID.eq(providerId));
        return fetch(query, chargebackRowMapper);
    }

    @Override
    public List<Chargeback> getChargebacksByCategory(List<ChargebackCategory> categories,
                                                     LocalDateTime dateFrom,
                                                     LocalDateTime dateTo) {
        Query query = getDslContext()
                .selectFrom(CHARGEBACK)
                .where(CHARGEBACK.PRETENSION_DATE.greaterOrEqual(dateFrom))
                .and(CHARGEBACK.PRETENSION_DATE.lessOrEqual(dateTo))
                .and(CHARGEBACK.CHARGEBACK_CATEGORY.in(categories));
        return fetch(query, chargebackRowMapper);
    }

    @Override
    public List<Chargeback> getChargebacksByStep(LocalDateTime dateFrom,
                                                 LocalDateTime dateTo,
                                                 ChargebackStage stage,
                                                 ChargebackStatus status) {
        Query query = getDslContext()
                .select()
                .from(CHARGEBACK)
                .join(CHARGEBACK_STATE).on(CHARGEBACK_STATE.CHARGEBACK_ID.eq(CHARGEBACK.ID))
                    .and(CHARGEBACK_STATE.STAGE.eq(stage))
                    .and(CHARGEBACK_STATE.STATUS.eq(status))
                .where(CHARGEBACK.PRETENSION_DATE.greaterOrEqual(dateFrom))
                .and(CHARGEBACK.PRETENSION_DATE.lessOrEqual(dateTo));
        return fetch(query, chargebackRowMapper);
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
                .and(CHARGEBACK_STATE.PAYMENT_ID.eq(paymentId));
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
