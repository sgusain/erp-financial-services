# ERP Financial Services

A microservices-based ERP financial system built with Spring Boot 3.2.5 and Spring Cloud 2023.0.1.

## Architecture Overview

```
                          ┌──────────────────┐
                          │   Eureka Server    │  (8761)
                          │  Service Registry   │
                          └─────────▲──────────┘
                                    │ register / discover
        ┌───────────────┬──────────┼──────────┬───────────────┬─────────────────┐
        │               │          │          │               │                 │
┌───────▼──────┐ ┌───────▼──────┐  │  ┌────────▼───────┐ ┌─────▼──────────┐ ┌────▼───────────────┐
│ User Service │ │Account Service│  │  │ Budget Service │ │Transaction Svc │ │ Notification Service│
│    (8081)    │ │    (8082)     │  │  │    (8083)      │ │    (8084)      │ │       (8085)         │
└──────────────┘ └───────────────┘  │  └────────────────┘ └────────────────┘ └──────────────────────┘
        ▲               ▲          │          ▲                   ▲                     ▲
        └───────────────┴──────────┼──────────┴───────────────────┴─────────────────────┘
                                    │ lb://
                          ┌─────────┴──────────┐
                          │   API Gateway       │  (8080)
                          │  Spring Cloud Gateway│
                          └─────────────────────┘

                          ┌─────────────────────┐
                          │   Config Server       │  (8888)
                          │  native profile ->     │
                          │  D:\erp-config          │
                          └─────────────────────┘
```

Each business service (User, Account, Budget, Transaction, Notification) is a completely
independent Spring Boot application with its own PostgreSQL database, registers itself with
Eureka, and is reached externally only through the API Gateway. The Config Server serves
shared configuration from the local filesystem (`D:\erp-config`) using the native profile.

No Kafka, Spring Security/JWT, Docker/Kubernetes, Resilience4j, or inter-service messaging
is used — each service is a simple, self-contained REST + JPA + PostgreSQL application.

## Prerequisites

- Java 17
- Maven 3.8+
- PostgreSQL running on `localhost:5432` (username: `postgres`, password: `postgres`)

Create the required databases before starting the services:

```sql
CREATE DATABASE userdb;
CREATE DATABASE accountdb;
CREATE DATABASE budgetdb;
CREATE DATABASE transactiondb;
CREATE DATABASE notificationdb;
```

Tables are created/updated automatically at startup (`spring.jpa.hibernate.ddl-auto=update`).

## Build

From the project root:

```bash
mvn clean install
```

## How to Run Each Service

Start the services in this order (each in its own terminal, from the project root):

```bash
# 1. Eureka Server - service registry
cd eureka-server
mvn spring-boot:run

# 2. Config Server - centralized configuration
cd config-server
mvn spring-boot:run

# 3. API Gateway - single entry point
cd api-gateway
mvn spring-boot:run

# 4. Business services (any order, once Eureka is up)
cd user-service
mvn spring-boot:run

cd account-service
mvn spring-boot:run

cd budget-service
mvn spring-boot:run

cd transaction-service
mvn spring-boot:run

cd notification-service
mvn spring-boot:run
```

Eureka dashboard: http://localhost:8761

## Port Assignments

| Service              | Port | Application Name      |
|-----------------------|------|------------------------|
| Eureka Server          | 8761 | eureka-server          |
| Config Server          | 8888 | config-server          |
| API Gateway            | 8080 | api-gateway            |
| User Service            | 8081 | USER-SERVICE            |
| Account Service         | 8082 | ACCOUNT-SERVICE         |
| Budget Service          | 8083 | BUDGET-SERVICE          |
| Transaction Service     | 8084 | TRANSACTION-SERVICE     |
| Notification Service    | 8085 | NOTIFICATION-SERVICE    |

## API Endpoints

All endpoints below can be called directly against each service, or through the API Gateway
at `http://localhost:8080` (routes below).

### User Service — `/api/users` (direct: 8081)

| Method | Endpoint          | Description        |
|--------|--------------------|---------------------|
| GET    | /api/users          | List all users       |
| GET    | /api/users/{id}      | Get user by id       |
| POST   | /api/users           | Create a user        |
| PUT    | /api/users/{id}      | Update a user        |
| DELETE | /api/users/{id}      | Delete a user        |

### Account Service — `/api/accounts` (direct: 8082)

| Method | Endpoint             | Description          |
|--------|------------------------|-----------------------|
| GET    | /api/accounts           | List all accounts      |
| GET    | /api/accounts/{id}       | Get account by id       |
| POST   | /api/accounts            | Create an account       |
| PUT    | /api/accounts/{id}       | Update an account       |
| DELETE | /api/accounts/{id}       | Delete an account       |

### Budget Service — `/api/budgets` (direct: 8083)

| Method | Endpoint            | Description         |
|--------|-----------------------|-----------------------|
| GET    | /api/budgets            | List all budgets      |
| GET    | /api/budgets/{id}        | Get budget by id       |
| POST   | /api/budgets             | Create a budget        |
| PUT    | /api/budgets/{id}        | Update a budget        |
| DELETE | /api/budgets/{id}        | Delete a budget        |

### Transaction Service — `/api/transactions` (direct: 8084)

| Method | Endpoint                  | Description             |
|--------|------------------------------|---------------------------|
| GET    | /api/transactions              | List all transactions      |
| GET    | /api/transactions/{id}          | Get transaction by id       |
| POST   | /api/transactions               | Create a transaction        |
| PUT    | /api/transactions/{id}          | Update a transaction        |
| DELETE | /api/transactions/{id}          | Delete a transaction        |

### Notification Service — `/api/notifications` (direct: 8085)

| Method | Endpoint                    | Description               |
|--------|--------------------------------|-----------------------------|
| GET    | /api/notifications                | List all notifications      |
| GET    | /api/notifications/{id}            | Get notification by id       |
| POST   | /api/notifications                 | Create a notification        |
| PUT    | /api/notifications/{id}            | Update a notification        |
| DELETE | /api/notifications/{id}            | Delete a notification        |

## Project Structure

```
erp-financial-services/
├── pom.xml                    (parent - dependency management)
├── eureka-server/              (8761 - service registry)
├── config-server/              (8888 - centralized config, native profile -> D:\erp-config)
├── api-gateway/                 (8080 - Spring Cloud Gateway)
├── user-service/                (8081 - userdb)
├── account-service/             (8082 - accountdb)
├── budget-service/              (8083 - budgetdb)
├── transaction-service/         (8084 - transactiondb)
├── notification-service/        (8085 - notificationdb)
└── README.md
```
