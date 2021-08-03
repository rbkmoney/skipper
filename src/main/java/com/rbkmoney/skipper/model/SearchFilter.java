package com.rbkmoney.skipper.model;

import com.rbkmoney.reporter.domain.enums.ChargebackStage;
import com.rbkmoney.reporter.domain.enums.ChargebackStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SearchFilter {

    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private String providerId;
    private List<ChargebackStatus> statuses;
    private List<ChargebackStage> stages;

}
