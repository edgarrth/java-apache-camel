package com.poc.pocapachecamel.handler;

import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//@RestController
//@RequestMapping("/evento")
public class EventoController {

   // @Autowired
    private ProducerTemplate producerTemplate;

    //@PostMapping
    public ResponseEntity<String> enviar(@RequestBody String body) {
        producerTemplate.sendBody("direct:enviar", body);
        return ResponseEntity.ok("✅ Evento enviado exitosamente");
    }
}
