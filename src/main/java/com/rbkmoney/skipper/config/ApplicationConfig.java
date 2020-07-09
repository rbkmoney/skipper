package com.rbkmoney.skipper.config;

import com.rbkmoney.damsel.payment_processing.InvoicingSrv;
import com.rbkmoney.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class ApplicationConfig {

    @Bean
    public InvoicingSrv.Iface invoicingThriftClient(
            @Value("${hellgate.invoicing.url}") Resource resource,
            @Value("${hellgate.invoicing.timeout}") int timeout
    )
            throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(resource.getURI())
                .withNetworkTimeout(timeout)
                .build(InvoicingSrv.Iface.class);
    }

}
