Yes. Replace your **entire `README.md`** with this version. It does **not mention internship** anywhere.

# Wallet Transaction Processor

A Spring Boot backend service for processing wallet transactions safely under concurrent requests.

The project focuses on **idempotency, database-level locking, transaction management, concurrency safety, and integration testing**.

---

## Overview

The service processes wallet debit transactions through a REST API and ensures that:

* The same transaction cannot be processed more than once.
* Concurrent debit requests are handled safely.
* Wallet balances cannot become negative.
* Transaction processing is atomic.
* The required behavior is verified using automated integration tests.

---

## Features

* Process wallet debit transactions
* Prevent duplicate transaction processing
* Prevent negative wallet balances
* Database-level pessimistic locking
* Transaction management using `@Transactional`
* H2 in-memory database
* Request validation
* Global exception handling
* Concurrent integration tests
* Zero external database configuration required

---

## Tech Stack

* **Java 17+**
* **Spring Boot**
* **Spring Web MVC**
* **Spring Data JPA**
* **H2 Database**
* **Maven**
* **JUnit 5**
* **MockMvc**

The project is configured for Java 21 and was tested using Java 22.0.2.

---

## Project Structure

```text
wallet-transaction-processor
│
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com.assignment.wallet_transaction_processor
│   │   │       │
│   │   │       ├── controller
│   │   │       │   └── TransactionController.java
│   │   │       │
│   │   │       ├── dto
│   │   │       │   ├── ProcessTransactionRequest.java
│   │   │       │   └── TransactionResponse.java
│   │   │       │
│   │   │       ├── entity
│   │   │       │   ├── Wallet.java
│   │   │       │   └── Transaction.java
│   │   │       │
│   │   │       ├── enums
│   │   │       │   ├── TransactionType.java
│   │   │       │   └── TransactionStatus.java
│   │   │       │
│   │   │       ├── exception
│   │   │       │   ├── WalletNotFoundException.java
│   │   │       │   ├── InsufficientFundsException.java
│   │   │       │   ├── DuplicateTransactionException.java
│   │   │       │   └── GlobalExceptionHandler.java
│   │   │       │
│   │   │       ├── repository
│   │   │       │   ├── WalletRepository.java
│   │   │       │   └── TransactionRepository.java
│   │   │       │
│   │   │       └── service
│   │   │           └── TransactionService.java
│   │   │
│   │   └── resources
│   │       └── application.properties
│   │
│   └── test
│       └── java
│           └── com.assignment.wallet_transaction_processor
│               └── TransactionProcessingIntegrationTest.java
│
├── DECISIONS.md
├── pom.xml
├── mvnw
└── mvnw.cmd
```

---

# API

## Process Transaction

```http
POST /api/v1/transactions/process
```

### Request Body

```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "amount": 250.00,
  "type": "DEBIT"
}
```

### Fields

| Field           | Type       | Description                           |
| --------------- | ---------- | ------------------------------------- |
| `transactionId` | UUID       | Unique identifier for the transaction |
| `userId`        | UUID       | Identifier of the wallet owner        |
| `amount`        | BigDecimal | Amount to debit                       |
| `type`          | String     | Transaction type                      |

---

## Successful Response

```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "amount": 250.00,
  "type": "DEBIT",
  "status": "SUCCESS",
  "balance": 750.00,
  "message": "Transaction processed successfully"
}
```

HTTP status:

```text
200 OK
```

---

# Error Responses

## Duplicate Transaction

If the same `transactionId` has already been processed:

```text
409 Conflict
```

Response:

```json
{
  "error": "Transaction already processed"
}
```

---

## Insufficient Funds

If the wallet does not have enough balance:

```text
400 Bad Request
```

Response:

```json
{
  "error": "Insufficient funds"
}
```

---

## Wallet Not Found

If no wallet exists for the supplied `userId`:

```text
404 Not Found
```

Response:

```json
{
  "error": "Wallet not found"
}
```

---

# Idempotency

The `transactionId` is used as the idempotency key.

Before processing a transaction, the application checks whether the transaction ID has already been processed.

The database also has a unique constraint on `transaction_id`.

This provides two levels of protection:

```text
Application check
       +
Database unique constraint
```

If the same transaction is received multiple times, the wallet should only be deducted once.

For example:

```text
3 identical requests
        ↓
1 successful request
2 duplicate requests
        ↓
Balance deducted only once
```

Duplicate requests return:

```text
409 Conflict
```

---

# Concurrency Handling

The application uses a database-level pessimistic write lock when retrieving a wallet.

The repository contains:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Wallet> findByUserId(UUID userId);
```

This prevents multiple transactions from modifying the same wallet balance at the same time.

Conceptually, the processing flow is:

```text
Request
   ↓
Find and lock wallet
   ↓
Check duplicate transaction
   ↓
Check available balance
   ↓
Deduct amount
   ↓
Save transaction
   ↓
Commit
```

This ensures that the balance check and balance update are performed safely under concurrent requests.

---

# Transaction Management

The transaction-processing service uses:

[//]: # (```java)

[//]: # (@Transactional)

[//]: # (```)

Wallet balance modification and transaction creation are therefore handled inside the same database transaction.

The intended flow is:

```text
Begin transaction
       ↓
Lock wallet
       ↓
Validate transaction
       ↓
Update wallet balance
       ↓
Create transaction record
       ↓
Commit
```

If processing fails before the transaction is committed, the database changes can be rolled back.

---

# Money Handling

The application uses `BigDecimal` for:

* Wallet balance
* Transaction amount

This avoids the precision problems that can occur when using `float` or `double` for monetary calculations.

The database fields use:

```text
precision = 19
scale = 2
```

---

# Database

The project uses an H2 in-memory database.

Database URL:

```text
jdbc:h2:mem:walletdb
```

Configuration:

```properties
spring.datasource.url=jdbc:h2:mem:walletdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=create-drop
```

No external database installation is required.

The database is automatically created when the application starts and removed when it shuts down.

---

# Integration Tests

The project contains three required integration tests.

The tests use:

* JUnit 5
* Spring Boot Test
* MockMvc
* H2
* ExecutorService
* CountDownLatch
* Multiple concurrent threads

The tests exercise the actual application flow:

```text
MockMvc
   ↓
Controller
   ↓
Service
   ↓
Repository
   ↓
H2 Database
```

No Postman or external database is required to run the tests.

---

# Test 1 — Single Valid Debit

### Test

```text
Processes a single valid debit transaction successfully.
```

### Scenario

```text
Initial balance: ₹500
Debit amount:    ₹100
```

### Expected result

```text
Final balance: ₹400
```

The test verifies that a normal debit transaction is processed successfully.

---

# Test 2 — Idempotency

### Test

```text
Sends 3 identical transactionIDs simultaneously.
Ensures the balance is only deducted once.
```

### Scenario

```text
Initial balance: ₹1000
Requests:        3
Debit amount:    ₹250
Same transactionId used by all requests
```

### Result

```text
Successful requests: 1
Duplicate requests:  2
Final balance:       ₹750
```

This verifies that the same transaction cannot deduct the wallet balance multiple times.

---

# Test 3 — Concurrent Debits

### Test

```text
Sends 10 concurrent debit requests of ₹100 for a wallet with a ₹500 balance. Ensures the final balance is exactly ₹0 and 5 requests fail with insufficient funds.
```

### Scenario

```text
Initial balance: ₹500
Requests:        10
Amount/request:  ₹100
```

### Result

```text
Successful requests: 5
Insufficient funds:  5
Final balance:       ₹0
```

This verifies that concurrent debit requests cannot make the wallet balance negative.

---

# Concurrency Test Implementation

The concurrent tests use `ExecutorService` to create multiple worker threads.

`CountDownLatch` is used to make the requests start together.

Simplified flow:

```text
Create worker threads
        ↓
All workers become ready
        ↓
Release all workers
        ↓
Requests execute concurrently
        ↓
Database locking controls wallet access
        ↓
Verify final balance and results
```

---

# Running the Application

## Requirements

Install:

* Java 17 or higher
* IntelliJ IDEA or another Java IDE

Maven can be used through the included Maven Wrapper.

No external database is required.

---

## Run Using IntelliJ

Open the project in IntelliJ IDEA.

Run:

```text
WalletTransactionProcessorApplication
```

The application starts on:

```text
http://localhost:8080
```

---

## Run Using Maven

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

### macOS / Linux

```bash
./mvnw spring-boot:run
```

---

# Running the Tests

## Windows

```powershell
.\mvnw.cmd clean test
```

## macOS / Linux

```bash
./mvnw clean test
```

The main integration test class is:

```text
TransactionProcessingIntegrationTest
```

---

# Test Results

The complete required integration test suite was successfully executed.

### Single Transaction

```text
Initial balance: ₹500
Debit amount:    ₹100
Final balance:   ₹400
```

### Duplicate Transactions

```text
Total requests:      3
Successful requests: 1
Duplicate requests:  2
Final balance:       ₹750
```

### Concurrent Debits

```text
Total requests:      10
Successful requests: 5
Insufficient funds:  5
Final balance:       ₹0
```

The test execution completed successfully with exit code `0`.

---

# Design Decisions

The main technical decisions and the reasoning behind them are documented in:

```text
DECISIONS.md
```

The document covers:

* H2 database
* BigDecimal for monetary values
* UUID identifiers
* Idempotency
* Pessimistic locking
* Transaction management
* Exception handling
* Integration testing
* Concurrent request handling

---

# Assignment Requirements Covered

| Requirement                      | Implementation         |
| -------------------------------- | ---------------------- |
| Java 17+                         | Java 21 configuration  |
| Spring Boot                      | Yes                    |
| H2 in-memory database            | Yes                    |
| Transaction processing endpoint  | Yes                    |
| Idempotency                      | Yes                    |
| Duplicate transaction protection | Yes                    |
| Database-level locking           | Pessimistic write lock |
| Prevent negative balance         | Yes                    |
| Concurrent requests              | Yes                    |
| JUnit 5 tests                    | Yes                    |
| Zero-configuration tests         | Yes                    |
| MockMvc                          | Yes                    |
| README.md                        | Yes                    |
| DECISIONS.md                     | Yes                    |

---

# Notes

The project focuses on transaction processing, idempotency, and concurrency safety.

Wallet records are created directly in the integration tests because the required API only covers transaction processing. A separate wallet creation endpoint is not required for the implemented functionality.

The project intentionally avoids unrelated infrastructure such as:

* Authentication
* JWT
* Frontend
* MySQL
* Redis
* Message queues
* External services

This keeps the implementation focused on the core transaction-processing requirements.
