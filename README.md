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
# 5. Job Lifecycle

The job lifecycle is designed around explicit state transitions.
``` mermaid
stateDiagram-v2
    direction LR

    [*] --> CREATED
    CREATED --> QUEUED
    QUEUED --> PROCESSING

    PROCESSING --> COMPLETED
    PROCESSING --> FAILED

    FAILED --> RETRYING
    RETRYING --> QUEUED
    RETRYING --> DEAD

    COMPLETED --> [*]
    DEAD --> [*]


```

The exact transition rules are controlled by the JobLifecycleService.

This prevents services from directly changing job status without checking whether the transition is valid

# 6. Job Execution Lifecycle

A job and a job execution are treated as two different concepts.

A Job represents the logical task submitted by the client.

A JobExecution represents one attempt to execute that job.

For example:
```
Job
 |
 +-- Execution #1 -> FAILED
 |
 +-- Execution #2 -> FAILED
 |
 +-- Execution #3 -> COMPLETED

```

This allows the system to maintain execution history instead of overwriting information from previous attempts.


# 7. Core Architecture Components

## Job Service

Responsible for:

- Accepting job creation requests
- Validating job information
- Persisting jobs
- Persisting job payloads
- Creating outbox events
- Providing job query APIs
- Maintaining job lifecycle rules

## PostgreSQL

PostgreSQL acts as the source of truth for the platform.

Important data stored in PostgreSQL includes:

- Jobs
- Job payloads
- Job executions
- Execution history
- Outbox events

The database is intentionally treated as the primary source of truth instead of relying only on Kafka.

## Apache Kafka

Kafka is used as the asynchronous messaging layer.

The main responsibility of Kafka is to transport job events from the producer side to worker services.

Example event:
``` 
{
  "eventId": "01M055MD015T4JM466JFT0BG3M",
  "jobId": "01M055MCV3MEX6V5PKASWZKV5N",
  "eventType": "JOB_CREATED"
}
```

Kafka allows multiple worker instances to process jobs using a consumer group.

## Worker Service

Workers consume job events from Kafka and execute jobs.


# 8. Transactional Outbox Pattern

The project uses the Transactional Outbox Pattern to avoid the dual-write problem.

Instead of doing:

```
Save Job
   |
   +--> Publish Kafka Event
```

the system performs:

Database Transaction
```
    Save Job
       +
    Save Payload
       +
    Save Outbox Event
        |
        v
     COMMIT
``` 
A separate process then publishes the pending outbox event to Kafka.

``` mermaid
flowchart LR

    API["Job API"]

    DB[("PostgreSQL")]

    OutboxTable["Outbox Events"]

    Publisher["Outbox Publisher"]

    Kafka["Kafka"]

    API --> DB

    DB --> OutboxTable

    Publisher --> OutboxTable

    Publisher --> Kafka
```

This prevents a situation where the job is successfully stored but the Kafka event is lost because Kafka was temporarily unavailable.

# 9. Idempotency

Duplicate Kafka delivery is expected in distributed systems.

The platform therefore uses an eventId to identify a specific event.

The job_executions table contains the event identifier and protects against duplicate execution attempts.

Conceptually:

```
Kafka Event
     |
     v
eventId
     |
     v
Already processed?
     |
   /   \
 Yes    No
 |       |
Ignore   Create Execution
         |
         v
       Execute
```
The database is used to enforce uniqueness rather than relying only on application-level checks.

# 10. Concurrent Job Claiming

Multiple workers may receive messages for jobs at the same time.

The worker therefore does not simply change the job status blindly.

Instead, it attempts to claim the job using a conditional update.

Conceptually:
```sql
UPDATE jobs
SET status = 'PROCESSING'
WHERE job_id = ?
AND status = 'QUEUED';
```
The important part is:

``` WHERE status = QUEUED ```

Only one worker should successfully change the job from QUEUED to PROCESSING.

This prevents multiple workers from processing the same job simultaneously.

# 11. Job Execution Tracking

Each execution attempt is stored separately.

The execution record contains information such as:
```
executionId
jobId
eventId
status
workerId
startedAt
completedAt
errorMessage
``` 
This makes it possible to answer questions such as:

Which worker processed the job?
When did execution start?
When did it finish?
How many attempts were made?
Why did an execution fail?

# 12. Retry Mechanism

When a worker fails to execute a job, the job is not immediately considered permanently failed.

Instead:
```
PROCESSING
     |
     v
FAILED
     |
     v
RETRYING
     |
     v
Next Retry Time
     |
     v
QUEUED
     |
     v
PROCESSING
```
The job maintains retry-related information such as:
```
retryCount
nextRetryAt
```
The retry scheduler looks for jobs whose retry time has arrived and makes them available for processing again.

# 13. Dead Letter Queue

The Dead Letter Queue is intended for jobs that cannot be successfully processed after the configured retry limit.

Conceptually:
``` mermaid
flowchart LR

    Processing["Processing"] --> Success["Success"]
    Processing --> Failure["Failure"]

    Failure --> Retry["Retry"] --> Processing
    Retry --> Limit{"Retry Limit Reached?"}

    Limit -->|No| Processing
    Limit -->|Yes| DLQ["Dead Letter Queue"] --> Manual["Manual Investigation"]
```
The DLQ allows permanently failed jobs to be retained instead of silently losing them.

# 14. Features Implemented

| **Job Management**              | **Event-Driven Processing**       | **Reliability**                   |
|---------------------------------|-----------------------------------|-----------------------------------|
| ✔️ Job creation API              | ✔️ Kafka integration              | ✔️ PostgreSQL as source of truth  |
| ✔️ Job persistence               | ✔️ Kafka producer                 | ✔️ Transactional job creation     |
| ✔️ Job payload persistence       | ✔️ Kafka consumer                 | ✔️ Transactional Outbox Pattern   |
| ✔️ Job status management         | ✔️ Job-created events             | ✔️ Outbox event persistence       |
| ✔️ Job priority                  | ✔️ Consumer group worker processing | ✔️ Outbox event publishing        |
| ✔️ Retry count tracking          | ✔️ Asynchronous job execution     | ✔️ Duplicate event detection      |
| ✔️ Next retry time tracking      |                                   | ✔️ Execution tracking             |
| ✔️ Job query APIs                |                                   | ✔️ Worker identification          |
|                                   |                                   | ✔️ Job claiming via conditional update |

| **Execution Management**         | **Retry Handling**                 | **Infrastructure**                 |
|----------------------------------|-------------------------------------|-----------------------------------|
| ✔️ Job execution records         | ✔️ Retry count                     | ✔️ Docker setup                   |
| ✔️ Execution status              | ✔️ Retry scheduling information    | ✔️ Docker Compose                 |
| ✔️ Execution start time          | ✔️ Retry scheduler                 | ✔️ PostgreSQL container           |
| ✔️ Execution completion time     | ✔️ Retry state                     | ✔️ Kafka container                |
| ✔️ Error message storage         | ✔️ Failure recording               | ✔️ Kafka UI                       |
| ✔️ Execution history             |                                    | ✔️ Local development environment  |
| ✔️ Execution detail API          |                                    |                                    |



# 15. Features Currently Being Worked On

| **In Progress**                  | **Testing**                      | **Failure Handling**              |
|----------------------------------|----------------------------------|-----------------------------------|
| ✔️ Finalize job lifecycle transitions | ✔️ Complete retry flow testing   | ✔️ Dead Letter Queue integration  |
| ✔️ Finalize retry state transitions | ✔️ End-to-end failure testing    | ✔️ Failed job recovery            |
| ✔️ Concurrent worker testing      | ✔️ Retry limit enforcement        | ✔️ Better Kafka error handling    |
| ✔️ Manual DLQ processing          |                                  | ✔️ Better worker failure handling |

---

# 16. Features Yet to Implement

| **Reliability**                  | **Kafka**                        | **Job Processing**                |
|----------------------------------|----------------------------------|-----------------------------------|
| ⬜ Better transactional boundaries | ⬜ Production-ready retry topics  | ⬜ Multiple job types              |
| ⬜ Improved failure recovery      | ⬜ Dead Letter Topic              | ⬜ Job timeout configuration       |
| ⬜ Worker crash recovery          | ⬜ Better partitioning strategy   | ⬜ Job cancellation                |
| ⬜ Stuck execution detection      | ⬜ Consumer lag monitoring        | ⬜ Job pause/resume                |
| ⬜ Job timeout handling           | ⬜ Retry metadata headers         | ⬜ Scheduled jobs                  |
| ⬜ Execution heartbeat            | ⬜ Improved producer error handling | ⬜ Delayed jobs                   |
| ⬜ Graceful worker shutdown       | ⬜ Kafka security configuration   | ⬜ Job dependency support          |

| **Retry System**                 | **Dead Letter Queue**            | **Security**                      |
|----------------------------------|----------------------------------|-----------------------------------|
| ⬜ Configurable maximum retries   | ⬜ DLQ topic                      | ⬜ Authentication                  |
| ⬜ Exponential backoff            | ⬜ DLQ database tracking          | ⬜ Authorization                   |
| ⬜ Fixed backoff configuration    | ⬜ Failed job inspection          | ⬜ Role-based access control       |
| ⬜ Retry policies per job type    | ⬜ Manual replay                  | ⬜ API security                    |
| ⬜ Retry history                  | ⬜ Manual discard                 | ⬜ Kafka authentication            |
| ⬜ Manual retry                   | ⬜ DLQ monitoring                 | ⬜ Database credential management  |
| ⬜ Retry monitoring               |                                  |                                   |

| **Observability**                | **Testing**                      | **Deployment**                    |
|----------------------------------|----------------------------------|-----------------------------------|
| ⬜ Structured logging             | ⬜ Unit tests                     | ⬜ Production Docker images        |
| ⬜ Correlation IDs                | ⬜ Repository tests               | ⬜ Kubernetes deployment           |
| ⬜ Distributed tracing            | ⬜ Service tests                  | ⬜ Kubernetes ConfigMaps           |
| ⬜ Metrics                        | ⬜ Kafka integration tests        | ⬜ Kubernetes Secrets              |
| ⬜ Prometheus                     | ⬜ End-to-end tests               | ⬜ Horizontal worker scaling       |
| ⬜ Grafana                        | ⬜ Concurrent worker tests        | ⬜ Health checks                   |
| ⬜ Kafka consumer metrics         | ⬜ Failure scenario tests          | ⬜ Readiness probes                |
| ⬜ Job processing metrics         | ⬜ Retry scenario tests            | ⬜ Liveness probes                 |
|                                  | ⬜ DLQ tests                      | ⬜ CI/CD pipeline                  |
|                                  | ⬜ Testcontainers integration     |                                   |


# 17. Important Distributed System Problems Being Solved

This project is being used to understand real backend engineering problems rather than only framework features.

## Problem 1: What if the database succeeds but Kafka fails?

**Solution: Outbox Pattern**

```mermaid
flowchart LR
    Job --> OutboxEvent --> Transaction --> COMMIT --> Publisher --> Kafka
``` 

## Problem 2: What if Kafka delivers the same event twice?

**Solution: Deduplication with Unique Constraint**
``` mermaid
flowchart LR
    EventId --> Constraint["Unique Constraint"] --> Duplicate["Duplicate Event"] --> Ignore
``` 
## Problem 3: What if two workers receive the same job?

**Solution: Conditional Job Claim**
``` mermaid
flowchart LR
    WorkerA --> Claim["Conditional Job Claim"]
    WorkerB --> Claim
    Claim --> Success["Only one succeeds"]
```
## Problem 4: What if job execution fails?
 
**Solution: Retry Flow**
``` mermaid
flowchart LR
    FAILED --> RETRYING --> Scheduler["Retry Scheduler"] --> QUEUED
```

## Problem 5: What if the job keeps failing?

**Solution: Dead Letter Queue (DLQ)**
``` mermaid
flowchart LR
    Retry1 --> Retry2 --> Retry3 --> Limit["Maximum Retry Count"] --> DLQ["Dead Letter Queue"]
```
# 18. Design Principles

The project follows these principles wherever practical:

| **Principle**                  | **Description** |
|--------------------------------|-----------------|
| **Single Responsibility**      | Services are separated based on their responsibilities. |
| **Database as Source of Truth**| Important job state is persisted in PostgreSQL. |
| **Idempotency**                | The system safely handles duplicate events. |
| **Explicit State Transitions** | Job states cannot be changed arbitrarily. |
| **Failure Should Be Visible**  | Failures are recorded rather than silently ignored. |
| **Asynchronous Processing**    | Long-running job execution should not block API requests. |
| **Loose Coupling**             | Kafka is used to decouple job creation from job processing. |
| **Observability**              | Execution history and error information are stored for investigation. |

# 19. Learning Outcomes

By completing this project, I aim to gain practical understanding of:

| **Area**                          | **Learning Outcome** |
|-----------------------------------|-----------------------|
| Distributed Job Queues            | How distributed job queues work |
| Kafka in Backend Systems          | How Kafka is used in real backend systems |
| Asynchronous Task Processing      | How workers process asynchronous tasks |
| Idempotency                       | Why idempotency is important |
| Transactional Outbox              | How transactional outbox solves dual-write problems |
| Database Coordination             | How databases help coordinate distributed workers |
| Retry Design                      | How retries should be designed |
| Dead Letter Queues                | How Dead Letter Queues are used |
| Execution History                 | How job execution history is maintained |
| Failure Handling                  | How failures are handled in distributed systems |
| Explicit State Machines           | How to design explicit state machines |
| Concurrency                       | How to reason about concurrency |
| Production-Ready Spring Boot      | How to build production-oriented Spring Boot systems |
