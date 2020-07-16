package com.rbkmoney.skipper.service;

import com.rbkmoney.damsel.skipper.ChargebackData;
import com.rbkmoney.damsel.skipper.ChargebackEvent;
import com.rbkmoney.damsel.skipper.ChargebackFilter;
import com.rbkmoney.damsel.skipper.SkipperSrv;
import com.rbkmoney.reporter.domain.tables.pojos.Chargeback;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackHoldState;
import com.rbkmoney.reporter.domain.tables.pojos.ChargebackState;
import com.rbkmoney.skipper.dao.ChargebackDao;
import com.rbkmoney.skipper.exception.DaoException;
import com.rbkmoney.skipper.exception.NotFoundException;
import com.rbkmoney.skipper.handler.EventHandler;
import com.rbkmoney.skipper.model.SearchFilter;
import com.rbkmoney.skipper.util.MapperUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.rbkmoney.skipper.util.MapperUtils.mapToSearchFilter;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkipperService implements SkipperSrv.Iface {

    private final ChargebackDao chargebackDao;
    private final EventHandler createChargebackEventHandler;
    private final EventHandler chargebackStatusChangeEventHandler;
    private final EventHandler chargebackHoldStatusChangeEventHandler;
    private final EventHandler reopenChargebackEventHandler;

    @SneakyThrows
    @Override
    public void processChargebackData(ChargebackEvent event) throws TException {
        try {
            if (event.isSetCreateEvent()) {
                createChargebackEventHandler.handle(event);
            } else if (event.isSetStatusChangeEvent()) {
                chargebackStatusChangeEventHandler.handle(event);
            } else if (event.isSetHoldStatusChangeEvent()) {
                chargebackHoldStatusChangeEventHandler.handle(event);
            } else if (event.isSetReopenEvent()) {
                reopenChargebackEventHandler.handle(event);
            } else {
                throw new UnsupportedOperationException("Events with type '" + event.getSetField().getFieldName() +
                        "' unsupported");
            }
        } catch (DaoException ex) {
            log.error("Error received when saving data to the database", ex);
            throw ex;
        } catch (NotFoundException ex) {
            log.error("Not found source data", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Error received when processing event data", ex);
            throw ex;
        }
    }

    @Override
    public ChargebackData getChargebackData(String invoiceId,
                                            String paymentId,
                                            String chargebackId) throws TException {
        Chargeback chargeback = chargebackDao.getChargeback(invoiceId, paymentId, chargebackId, false);
        return getChargebackDataByChargeback(chargeback);
    }

    @Override
    public ChargebackData getRetrievalRequestData(String invoiceId,
                                                  String paymentId,
                                                  String chargebackId) throws TException {
        Chargeback chargeback = chargebackDao.getChargeback(invoiceId, paymentId, chargebackId, true);
        return getChargebackDataByChargeback(chargeback);
    }

    @Override
    public List<ChargebackData> getChargebacks(ChargebackFilter chargebackFilter) throws TException {
        SearchFilter searchFilter = mapToSearchFilter(chargebackFilter);
        List<Chargeback> chargebacks = chargebackDao.getChargebacks(searchFilter);
        return getChargebackDataList(chargebacks);
    }

    private List<ChargebackData> getChargebackDataList(List<Chargeback> chargebacks) {
        List<ChargebackData> chargebackDataList = new ArrayList<>();
        for (Chargeback chargeback : chargebacks) {
            chargebackDataList.add(getChargebackDataByChargeback(chargeback));
        }
        return chargebackDataList;
    }

    private ChargebackData getChargebackDataByChargeback(Chargeback chargeback) {
        Long chargebackId = chargeback.getId();
        List<ChargebackState> chargebackStates = chargebackDao.getChargebackStates(chargebackId);
        List<ChargebackHoldState> chargebackHoldStates = chargebackDao.getChargebackHoldStates(chargebackId);
        return MapperUtils.mapToChargebackData(chargeback, chargebackStates, chargebackHoldStates);
    }
}
