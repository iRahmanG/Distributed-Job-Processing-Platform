<div align="center">

# Distributed Job Processing Platform

A distributed job processing platform built using **Java**, **Spring Boot**, **Apache Kafka**, **PostgreSQL**, **Docker**, and **Spring Data JPA**.

The goal of this project is to understand how production-inspired distributed job processing systems are designed and implemented. Instead of building a simple background-task application, this project focuses on important backend engineering problems such as **reliable job creation, asynchronous processing, idempotency, execution tracking, retries, failure handling, Dead Letter Queues, transactional consistency, and event-driven architecture**.

The project is being developed incrementally, with each feature implemented and tested before moving towards the next part of the system.

![Java](https://img.shields.io/badge/Java-21-red)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3-green)
![Kafka](https://img.shields.io/badge/Apache_Kafka-Event--Driven-black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue)
![Docker](https://img.shields.io/badge/Docker-Containerized-blue)
![Maven](https://img.shields.io/badge/Maven-Build-orange)

</div>

---

# Distributed Job Processing Platform

## 1. Project Overview

The Distributed Job Processing Platform is a backend system designed to accept jobs through REST APIs, persist them in PostgreSQL, publish job events through Apache Kafka, and process those jobs asynchronously using worker services.

The main purpose of this project is not only to execute jobs, but to understand what happens when distributed systems face problems such as:

- Kafka failures
- Worker crashes
- Duplicate messages
- Job execution failures
- Retry requirements
- Database consistency problems
- Multiple workers processing the same job
- Permanently failed jobs
- Need for execution history and debugging

The platform follows an **event-driven architecture** where Kafka acts as the messaging layer between job creation and job processing.

---

# 2. Main Goals

The project is being built to understand and implement:

- Event-driven architecture
- Asynchronous job processing
- Distributed workers
- Kafka producers and consumers
- Database-first job persistence
- Transactional Outbox Pattern
- Idempotent processing
- Job execution tracking
- Retry mechanisms
- Dead Letter Queue
- Failure handling
- Job lifecycle management
- Concurrent job claiming
- PostgreSQL transactions
- Docker-based local infrastructure
- Production-oriented backend design

---

# 3. High-Level Architecture

```mermaid
flowchart LR

    Client["Client / REST API"]

    JobService["Job Service"]

    PostgreSQL[("PostgreSQL")]

    Outbox["Outbox Processor"]

    Kafka["Apache Kafka"]

    Worker1["Worker Service 1"]
    Worker2["Worker Service 2"]
    WorkerN["Worker Service N"]

    Executor["Job Executor"]

    Retry["Retry Scheduler"]

    DLQ["Dead Letter Queue"]

    Client -->|POST /jobs| JobService

    JobService -->|Save Job| PostgreSQL
    JobService -->|Save Outbox Event| PostgreSQL

    PostgreSQL -->|Pending Events| Outbox

    Outbox -->|Publish JOB_CREATED| Kafka

    Kafka --> Worker1
    Kafka --> Worker2
    Kafka --> WorkerN

    Worker1 --> Executor
    Worker2 --> Executor
    WorkerN --> Executor

    Worker1 -->|Execution Result| PostgreSQL
    Worker2 -->|Execution Result| PostgreSQL
    WorkerN -->|Execution Result| PostgreSQL

    PostgreSQL --> Retry
    Retry -->|Retry Job| Kafka

    Kafka -->|Failed After Retry Limit| DLQ

```

# 4. Job Processing Flow

The basic processing flow is:

```mermaid
sequenceDiagram

    participant C as Client
    participant API as Job Service
    participant DB as PostgreSQL
    participant O as Outbox Processor
    participant K as Kafka
    participant W as Worker
    participant E as Job Executor

    C->>API: POST /jobs
    API->>DB: Save Job
    API->>DB: Save Job Payload
    API->>DB: Save Outbox Event
    API-->>C: 201 Created

    O->>DB: Find pending outbox events
    O->>K: Publish JOB_CREATED event

    K->>W: Deliver job event

    W->>DB: Load Job
    W->>DB: Load Payload

    W->>DB: Claim Job

    W->>DB: Create JobExecution

    W->>E: Execute Job

    alt Execution Successful
        E-->>W: Success
        W->>DB: Mark Execution COMPLETED
        W->>DB: Mark Job COMPLETED
    else Execution Failed
        E-->>W: Failure
        W->>DB: Mark Execution FAILED
        W->>DB: Schedule Retry
    end

```
