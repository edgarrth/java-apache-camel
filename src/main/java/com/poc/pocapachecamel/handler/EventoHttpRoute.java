package com.poc.pocapachecamel.handler;


import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class EventoHttpRoute extends RouteBuilder {
    @Override
    public void configure() {

        // Ruta HTTP que reemplaza al @PostMapping("/evento")
        from("platform-http:/evento?httpMethodRestrict=POST")
                .routeId("http-evento")
                .routeConfigurationId("common-config")
                .log("📥 [HTTP] Evento recibido: ${body}")
                .to("direct:enviar");
    }
}

