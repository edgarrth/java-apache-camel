package com.poc.pocapachecamel.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class DynamicPublishRoute extends RouteBuilder {

    @Value("${camel.route.output}")
    private String destinoFinal;

    @Override
    public void configure() {

        from("direct:enviar")
                .routeId("publicador-eventos")
                .routeConfigurationId("common-config")
                .toD(destinoFinal);
    }
}
