package com.poc.pocapachecamel.config;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.RouteConfigurationBuilder;
import org.apache.camel.model.OnExceptionDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
//@Order(1)
public class CommonRouteConfiguration extends RouteConfigurationBuilder {

        @Value("${camel.logging.enabled:true}")
        private boolean loggingEnabled;

        @Value("${camel.logging.level:INFO}")
        private String logLevel;

        @Value("${camel.error.max-redeliveries:3}")
        private int maxRedeliveries;

        @Value("${camel.error.redelivery-delay:2000}")
        private int redeliveryDelay;

    @Override
    public void configuration() {
        routeConfiguration("common-config")
                .intercept()
                .process(exchange -> {
                    String endpointUri = exchange.getFromEndpoint().getEndpointUri();
                    exchange.setProperty("endpointUri", endpointUri);
                })
                .log(LoggingLevel.valueOf(logLevel),
                        "🧭 RUTA=${routeId} - ENTRADA=${body}  - ENDPOINT=${exchangeProperty.endpointUri}" );

        OnExceptionDefinition errorDef = onException(Exception.class)
                .handled(true)
                .log(LoggingLevel.ERROR, "## ERROR en ruta ${routeId}: ${exception.message}")
                .setHeader("error", constant(true))
                .setBody(simple("Error procesando mensaje: ${exception.message}"));

    }

}
