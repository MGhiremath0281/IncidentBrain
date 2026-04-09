# IncidentBrain

AI-powered incident management platform built on Spring Boot microservices. IncidentBrain automatically ingests alerts, correlates them into incidents, enriches each incident with logs and deployment context, generates root cause analysis using an LLM, and produces structured postmortems — all in real time through an event-driven pipeline.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Microservices](#microservices)
- [Kafka Event Flow](#kafka-event-flow)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration Reference](#configuration-reference)
- [API Reference](#api-reference)
- [Development Roadmap](#development-roadmap)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

Modern engineering teams deal with a flood of alerts during incidents. Triaging, correlating, and writing postmortems is slow, manual, and error-prone. IncidentBrain solves this by building a fully automated pipeline:

1. Alerts arrive via REST and are published to Kafka
2. The Correlation Service groups related alerts into incidents using a sliding time window
3. The Context Service enriches each incident with relevant logs from Elasticsearch
4. The AI Service (Python/FastAPI) performs root cause analysis using an LLM with Retrieval-Augmented Generation
5. The Postmortem Service compiles a structured postmortem document automatically
6. Engineers interact with the system through a Copilot chat interface powered by the same AI backend

All services register with Eureka and communicate through the API Gateway. The entire stack is designed to run locally via Docker Compose or be deployed to cloud platforms.

---

## Architecture

```
                        +---------------------------+
                        |     React / Next.js UI    |
                        |   Dashboard + Copilot     |
                        +-------------+-------------+
                                      |
                                   REST / WS
                                      |
                        +-------------v-------------+
                        |       API Gateway         |
                        |   Spring Cloud Gateway    |
                        |         :8080             |
                        +----+--------+--------+----+
                             |        |        |
              +--------------+   +----+   +----+-----------+
              |                  |                          |
  +-----------v-----+  +---------v-------+  +--------------v-----+
  |  Alert Service  |  | Copilot Service |  | Postmortem Service |
  |    :8081        |  |     :8084       |  |      :8085         |
  +-----------+-----+  +---------+-------+  +--------------+-----+
              |                  |                          |
              |         +--------v--------+                |
              |         |   AI Service    |                |
              |         |  Python/FastAPI |                |
              |         |     :8090       |                |
              |         +--------+--------+                |
              |                  |                         |
    +---------v------------------v-------------------------v---------+
    |                        Apache Kafka                            |
    |   alerts.raw | incidents.created | context.ready | postmortem |
    +---------+--------------------+------------------+--------------+
              |                   |                  |
  +-----------v-----+  +----------v------+  +--------v-----------+
  | Correlation Svc |  | Context Service |  |  Notification Svc  |
  |    :8082        |  |     :8083       |  |      :8086         |
  +-----------------+  +-----------------+  +--------------------+

  +----------------+  +---------------+  +----------+  +-----------+
  |  PostgreSQL    |  | Elasticsearch |  |  Redis   |  | Weaviate  |
  |  :5432         |  |    :9200      |  |  :6379   |  |  :8085    |
  +----------------+  +---------------+  +----------+  +-----------+

  +---------------------------------------------------------------+
  |                    Eureka Server  :8761                       |
  |              Service Registry for all Spring services         |
  +---------------------------------------------------------------+
```

---

## Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| Service Registry | Spring Cloud Netflix Eureka | Service discovery and health monitoring |
| API Gateway | Spring Cloud Gateway | Single entry point, routing, load balancing |
| Microservices | Spring Boot 3, Java 17 | Alert, Correlation, Context, Copilot, Postmortem |
| Event Streaming | Apache Kafka | Async event pipeline between services |
| AI Engine | Python 3.10, FastAPI | LLM inference, RAG, embedding generation |
| LLM | OpenAI API or Ollama (local) | Root cause analysis and natural language generation |
| Relational DB | PostgreSQL | Persistent storage for incidents and postmortems |
| Search and Logs | Elasticsearch | Log indexing and full-text search |
| Cache | Redis | AI response caching and session data |
| Vector DB | Weaviate | Semantic embeddings for RAG retrieval |
| Frontend | React, Next.js | Dashboard and Copilot chat interface |
| Containerization | Docker, Docker Compose | Local orchestration of all services |

---

## Microservices

### Eureka Server — port 8761

The service registry. All Spring Boot microservices register on startup and discover each other by name. The API Gateway uses Eureka's discovery locator to route requests without hardcoded URLs.

### API Gateway — port 8080

The single entry point for all external traffic. Routes requests to the correct downstream service using Spring Cloud Gateway and Eureka discovery. Handles cross-cutting concerns such as logging and fallback responses.

Key routes:
- `/alerts/**` → alert-service
- `/copilot/**` → copilot-service
- `/postmortems/**` → postmortem-service
- `/incidents/**` → correlation-service

### Alert Service — port 8081

Receives raw alerts via REST and publishes them to the `alerts.raw` Kafka topic. Also persists alert records to PostgreSQL for audit and replay purposes.

Endpoints:
- `POST /alerts` — ingest a new alert
- `GET /alerts` — list all alerts

### Correlation Service — port 8082

Consumes `alerts.raw` and groups alerts into incidents using a 5-minute sliding time window, keyed by service name and severity. Publishes new incidents to `incidents.created` and persists them to PostgreSQL.

### Context Service — port 8083

Consumes `incidents.created` and enriches each incident by fetching related logs from Elasticsearch and optional deployment metadata. Publishes the enriched payload to `context.ready` for downstream AI processing.

### AI Service — port 8090

Python/FastAPI service. Consumes `context.ready` events, builds a prompt from the enriched context, queries the LLM for root cause analysis and remediation suggestions, retrieves similar historical incidents from Weaviate using RAG, and caches results in Redis. Also exposes a synchronous REST endpoint used by the Copilot Service.

Endpoints:
- `POST /analyze` — accepts context payload, returns AI analysis

### Copilot Service — port 8084

Bridges the frontend chat interface and the AI Service. Accepts user questions via WebSocket or REST, forwards them to the AI Service with relevant incident context, and streams responses back to the UI. Recent answers are cached in Redis.

Endpoints:
- `POST /copilot/query` — submit a chat query
- `WS /copilot/ws` — WebSocket connection for streaming responses

### Postmortem Service — port 8085

Consumes `context.ready` events, calls the AI Service for a structured summary, compiles a full postmortem document including timeline, root cause, impact, and action items, and persists it to PostgreSQL. Stores embeddings in Weaviate for future RAG retrieval.

Endpoints:
- `GET /postmortems` — list all postmortems
- `GET /postmortems/{incidentId}` — get postmortem for a specific incident

### Frontend UI — port 3000

Next.js application with three main sections:

- **Dashboard** — real-time list of active and resolved incidents with severity and status
- **Postmortem Viewer** — structured postmortem pages including AI-generated root cause and timeline
- **Copilot Chat** — chat interface to query the AI about any incident

---

## Kafka Event Flow

```
Alert Service
    |
    | publishes
    v
alerts.raw
    |
    | consumed by
    v
Correlation Service ------> incidents.created
                                    |
                                    | consumed by
                                    v
                            Context Service ------> context.ready
                                                          |
                                         +----------------+----------------+
                                         |                                 |
                                         v                                 v
                                    AI Service                   Postmortem Service
                                         |
                                         v
                                  postmortem.events
```

### Topic Reference

| Topic | Producer | Consumer(s) | Key Fields |
|---|---|---|---|
| `alerts.raw` | Alert Service | Correlation Service | alertId, service, severity, timestamp, message |
| `incidents.created` | Correlation Service | Context Service, Postmortem Service | incidentId, alerts[], startTime, affectedService |
| `context.ready` | Context Service | AI Service, Postmortem Service | incidentId, logs[], metrics, deploymentInfo |
| `postmortem.events` | Postmortem Service | Notification Service, UI | postmortemId, incidentId, summary, actions[] |

---

## Project Structure

```
incidentbrain/
|
|-- eureka-server/
|   |-- src/main/java/com/incidentbrain/eureka/
|   |-- src/main/resources/application.yml
|   `-- pom.xml
|
|-- api-gateway/
|   |-- src/main/java/com/incidentbrain/gateway/
|   |-- src/main/resources/application.yml
|   `-- pom.xml
|
|-- alert-service/
|   |-- src/main/java/com/incidentbrain/alert/
|   |   |-- controller/AlertController.java
|   |   |-- service/AlertService.java
|   |   |-- kafka/AlertProducer.java
|   |   |-- model/Alert.java
|   |   `-- repository/AlertRepository.java
|   `-- pom.xml
|
|-- correlation-service/
|-- context-service/
|-- copilot-service/
|-- postmortem-service/
|
|-- ai-service/                  # Python / FastAPI
|   |-- main.py
|   |-- kafka_consumer.py
|   |-- rag/
|   |   |-- embedder.py
|   |   `-- retriever.py
|   |-- llm/
|   |   `-- analyzer.py
|   |-- cache/redis_client.py
|   `-- requirements.txt
|
|-- incidentbrain-ui/            # Next.js
|   |-- pages/
|   |-- components/
|   `-- package.json
|
`-- docker-compose.yml
```

---

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8 or higher
- Python 3.10 or higher
- Node.js 18 or higher
- Docker and Docker Compose

### 1. Clone the repository

```bash
git clone https://github.com/your-username/incidentbrain.git
cd incidentbrain
```

### 2. Start infrastructure with Docker Compose

```bash
docker-compose up -d kafka zookeeper postgres redis elasticsearch weaviate
```

Wait for all containers to report healthy before proceeding.

### 3. Start the Eureka Server

```bash
cd eureka-server
mvn spring-boot:run
```

Verify the registry is running at `http://localhost:8761`.

### 4. Start the API Gateway

```bash
cd api-gateway
mvn spring-boot:run
```

### 5. Start each Spring Boot microservice

Run each of the following in a separate terminal:

```bash
cd alert-service && mvn spring-boot:run
cd correlation-service && mvn spring-boot:run
cd context-service && mvn spring-boot:run
cd copilot-service && mvn spring-boot:run
cd postmortem-service && mvn spring-boot:run
```

Verify all five services appear in the Eureka dashboard at `http://localhost:8761`.

### 6. Start the AI Service

```bash
cd ai-service
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8090 --reload
```

### 7. Start the Frontend

```bash
cd incidentbrain-ui
npm install
npm run dev
```

The UI is accessible at `http://localhost:3000`.

### 8. Send a test alert

```bash
curl -X POST http://localhost:8080/alerts \
  -H "Content-Type: application/json" \
  -d '{
    "service": "payment-service",
    "severity": "HIGH",
    "message": "Response time exceeded 5000ms",
    "timestamp": "2025-04-09T10:00:00Z"
  }'
```

Watch the Kafka pipeline propagate: alert → incident → context → AI analysis → postmortem.

---

## Configuration Reference

### Eureka Server — `application.yml`

```yaml
server:
  port: 8761

spring:
  application:
    name: eureka-server

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
  server:
    wait-time-in-ms-when-sync-empty: 0
```

### Eureka Client (all microservices)

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

### API Gateway — routes

```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        - id: alert-service
          uri: lb://alert-service
          predicates:
            - Path=/alerts/**
        - id: copilot-service
          uri: lb://copilot-service
          predicates:
            - Path=/copilot/**
        - id: postmortem-service
          uri: lb://postmortem-service
          predicates:
            - Path=/postmortems/**
```

### Kafka — producer and consumer

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: incidentbrain-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
```

### Service Port Reference

| Service | Port | application.name |
|---|---|---|
| Eureka Server | 8761 | eureka-server |
| API Gateway | 8080 | api-gateway |
| Alert Service | 8081 | alert-service |
| Correlation Service | 8082 | correlation-service |
| Context Service | 8083 | context-service |
| Copilot Service | 8084 | copilot-service |
| Postmortem Service | 8085 | postmortem-service |
| AI Service | 8090 | — (Python, not registered) |
| Frontend UI | 3000 | — |

---

## API Reference

### Alert Service

```
POST   /alerts              Ingest a new alert
GET    /alerts              List all alerts
GET    /alerts/{id}         Get alert by ID
```

Request body for `POST /alerts`:

```json
{
  "service": "string",
  "severity": "LOW | MEDIUM | HIGH | CRITICAL",
  "message": "string",
  "timestamp": "ISO-8601"
}
```

### Copilot Service

```
POST   /copilot/query       Submit a question about an incident
WS     /copilot/ws          WebSocket endpoint for streaming chat
```

Request body for `POST /copilot/query`:

```json
{
  "incidentId": "string",
  "question": "string"
}
```

### Postmortem Service

```
GET    /postmortems                   List all postmortems
GET    /postmortems/{incidentId}      Get postmortem for a specific incident
```

### AI Service (internal)

```
POST   /analyze             Accepts enriched context, returns AI analysis
```

---

## Development Roadmap

| Version | Phase | Description | Status |
|---|---|---|---|
| v0.1 | Phase 0 | Environment setup — Kafka, PostgreSQL, Redis, Elasticsearch, Weaviate | Planned |
| v0.2 | Phase 1 | Eureka Server — service registry | Planned |
| v0.3 | Phase 2 | API Gateway — routing and discovery | Planned |
| v0.4 | Phase 3 | Alert Service — ingest and Kafka producer | Planned |
| v0.5 | Phase 4 | Correlation Service — incident grouping | Planned |
| v0.6 | Phase 5 | Context Service — log enrichment | Planned |
| v0.7 | Phase 6 | AI Service — LLM + RAG pipeline | Planned |
| v0.8 | Phase 7 | Copilot Service — chat interface | Planned |
| v0.9 | Phase 8 | Postmortem Service — auto-generation | Planned |
| v1.0-beta | Phase 9 | Frontend UI — dashboard and chat | Planned |
| v1.0-rc | Phase 10 | Integration testing and Docker | Planned |
| v1.0 | Phase 11 | Production deployment — Render + Vercel | Planned |

---

## Contributing

Contributions are welcome. To contribute:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Commit your changes: `git commit -m "feat: description of change"`
4. Push to your branch: `git push origin feature/your-feature-name`
5. Open a pull request against `main`

Please follow the existing code style. Each service is independently buildable and testable — keep cross-service changes minimal and document any new Kafka topic contracts in this README.

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
