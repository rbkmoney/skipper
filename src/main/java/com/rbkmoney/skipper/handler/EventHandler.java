package com.rbkmoney.skipper.handler;

import com.rbkmoney.damsel.skipper.ChargebackEvent;

public interface EventHandler {

    void handle(ChargebackEvent event) throws Exception;

}
