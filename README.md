# PoC Apache Camel

PoC de integración utilizando **Apache Camel 4**, **Spring Boot 3** y **Java 21**.

El objetivo del proyecto es demostrar cómo desacoplar la recepción de eventos de su mecanismo de publicación utilizando rutas Camel y configuración externa.

Actualmente la aplicación:

1. Expone un endpoint HTTP `POST /evento`.
2. Recibe un payload arbitrario.
3. Envía el mensaje a una ruta interna Camel (`direct:enviar`).
4. Publica dinámicamente el mensaje hacia un broker configurado mediante propiedades.
5. Incluye configuración común para logging, trazabilidad y manejo de errores.

La implementación actual utiliza **Kafka** como destino por defecto, aunque puede cambiarse a cualquier broker sin modificar código.

---

# Arquitectura

## Flujo de Mensajes

```text
┌───────────────┐
│ Cliente HTTP  │
└───────┬───────┘
        │ POST /evento
        ▼
┌─────────────────────────┐
│ EventoHttpRoute         │
│ platform-http:/evento   │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ direct:enviar          │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ DynamicPublishRoute     │
│ toD(destinoFinal)       │
└───────────┬─────────────┘
            │
            ▼
 ┌───────────────────────┐
 │ Kafka / Google PubSub │
 └───────────────────────┘
```

---

# Estructura del Proyecto

```text
src
└── main
    ├── java
    │   └── com.poc.pocapachecamel
    │       ├── config
    │       │   └── CommonRouteConfiguration.java
    │       │
    │       ├── handler
    │       │   ├── EventoHttpRoute.java
    │       │   └── EventoController.java
    │       │
    │       ├── routes
    │       │   └── DynamicPublishRoute.java
    │       │
    │       └── PoCApacheCamelApplication.java
    │
    └── resources
        └── application.yml
```

---

# Componentes Principales

## PoCApacheCamelApplication

Clase principal de Spring Boot.

```java
@SpringBootApplication
public class PoCApacheCamelApplication {
    public static void main(String[] args) {
        SpringApplication.run(PoCApacheCamelApplication.class, args);
    }
}
```

Responsabilidades:

* Inicializar Spring Boot.
* Inicializar Camel Context.
* Registrar automáticamente las rutas Camel.

---

## EventoHttpRoute

Archivo:

```text
handler/EventoHttpRoute.java
```

Define el endpoint HTTP utilizando el componente `platform-http`.

```java
from("platform-http:/evento?httpMethodRestrict=POST")
```

Flujo:

1. Recibe solicitudes HTTP POST.
2. Registra el payload recibido.
3. Envía el mensaje a la ruta interna `direct:enviar`.

---

## DynamicPublishRoute

Archivo:

```text
routes/DynamicPublishRoute.java
```

Responsable de publicar el mensaje al destino configurado.

```java
from("direct:enviar")
    .toD(destinoFinal);
```

La publicación utiliza `toD()` (To Dynamic), permitiendo cambiar el destino únicamente mediante configuración.

Por defecto:

```yaml
camel:
  route:
    output: kafka:{{camel.topic}}?brokers={{camel.kafka.brokers}}
```

---

## CommonRouteConfiguration

Archivo:

```text
config/CommonRouteConfiguration.java
```

Configuración común reutilizada por todas las rutas.

Funcionalidades:

### Logging de entrada

Registra:

* Route Id
* Payload recibido
* Endpoint origen

Ejemplo:

```text
🧭 RUTA=http-evento
ENTRADA={"id":1}
ENDPOINT=platform-http:/evento
```

### Manejo de errores

Captura excepciones globalmente:

```java
onException(Exception.class)
```

Características:

* Manejo centralizado.
* Respuesta controlada.
* Logging de errores.
* Preparado para reintentos configurables.

---

## EventoController

Archivo:

```text
handler/EventoController.java
```

Actualmente no se utiliza.

Se observa una implementación alternativa basada en Spring MVC:

```java
@RestController
@RequestMapping("/evento")
```

pero se encuentra comentada porque la PoC utiliza Apache Camel Platform HTTP.

---

# Configuración

Archivo:

```text
src/main/resources/application.yml
```

## Puerto HTTP

```yaml
server:
  port: 9090
```

---

## Kafka

```yaml
camel:
  kafka:
    brokers: localhost:9092
```

---

## Topic

```yaml
camel:
  topic: mi-topico
```

---

## Destino de Publicación

```yaml
camel:
  route:
    output: kafka:{{camel.topic}}?brokers={{camel.kafka.brokers}}
```

---

## Configuración de Errores

```yaml
camel:
  error:
    max-redeliveries: 3
    redelivery-delay: 2000
```

---

# Infraestructura Requerida

## Kafka

La PoC requiere únicamente Kafka.

No utiliza:

* Base de datos
* Redis
* RabbitMQ
* MongoDB

---

## Levantar Kafka con Docker

Crear archivo:

```yaml
services:

  kafka:
    image: bitnami/kafka:latest

    ports:
      - "9092:9092"

    environment:
      - KAFKA_CFG_NODE_ID=1
      - KAFKA_CFG_PROCESS_ROLES=broker,controller
      - KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=1@kafka:9093
      - KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093
      - KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092
      - KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER
      - KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
```

Levantar:

```bash
docker compose up -d
```

Verificar:

```bash
docker ps
```

---

# Compilación

```bash
mvn clean install
```

---

# Ejecución

## Desde Maven

```bash
mvn spring-boot:run
```

La aplicación iniciará en:

```text
http://localhost:9090
```

---

## Desde JAR

Generar artefacto:

```bash
mvn clean package
```

Ejecutar:

```bash
java -jar target/PoC-ApacheCamel-0.0.1-SNAPSHOT.jar
```

---

# Prueba de la Aplicación

Enviar evento:

```bash
curl --location 'http://localhost:9090/evento' \
--header 'Content-Type: application/json' \
--data '{
  "id": 1,
  "nombre": "evento-prueba"
}'
```

---

# Cambio de Kafka a Google Pub/Sub

La aplicación fue diseñada para soportar distintos destinos de publicación.

Para utilizar Google Pub/Sub basta con modificar:

```yaml
camel:
  route:
    output: google-pubsub:{{camel.pubsub.project-id}}:{{camel.pubsub.topic-id}}
```

Sin necesidad de realizar cambios en las rutas Camel.

---

# Dependencias Principales

* Java 21
* Spring Boot 3.5.3
* Apache Camel 4.12.0
* Camel Platform HTTP
* Camel Kafka
* Camel Google Pub/Sub
* Maven

---

# Objetivo de la PoC

Demostrar un patrón de integración desacoplado donde:

* La recepción del evento es independiente del broker.
* El destino puede cambiar mediante configuración.
* Las rutas comparten observabilidad y manejo de errores.
* Se aprovechan las capacidades de Apache Camel para implementar Enterprise Integration Patterns (EIP).
