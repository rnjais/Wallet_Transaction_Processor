# Technical Decisions

## 1. H2 In-Memory Database

I used H2 as the database because the assignment requires a zero-configuration setup.

This means the reviewer does not need to install MySQL or configure an external database before running the project.

The database is created automatically when the application starts and is cleared when the application stops.

[//]: # (```properties)

[//]: # (spring.datasource.url=jdbc:h2:mem:walletdb)

[//]: # (spring.jpa.hibernate.ddl-auto=create-drop)

[//]: # (```)

---

## 2. BigDecimal for Money

I used `BigDecimal` for wallet balances and transaction amounts.

Using `double` or `float` for money can cause precision problems, so `BigDecimal` is safer for financial calculations.

For example:

```text
₹500.00
₹250.00
₹100.00
```

are stored and calculated accurately.

---

## 3. UUID for IDs

I used `UUID` for:

* `userId`
* `transactionId`
* Entity IDs

The assignment already defines `userId` and `transactionId` as UUIDs, so the application follows the same format.

UUIDs also provide unique identifiers without depending on sequential numbers.

---

## 4. Transaction ID as the Idempotency Key

The `transactionId` is treated as the unique identifier for a transaction.

I added a database-level unique constraint on `transaction_id`.

[//]: # (```java)

[//]: # (@UniqueConstraint&#40;)

[//]: # (    name = "uk_transaction_id",)

[//]: # (    columnNames = "transaction_id")

[//]: # (&#41;)

[//]: # (```)

The service also checks whether the transaction already exists before processing it.

This is important because the same webhook may be received more than once.

If the same transaction is received again, the application returns:

```text
409 Conflict
```

instead of deducting the money again.

---

## 5. Pessimistic Lock for Wallet Updates

The main concurrency problem is multiple requests trying to update the same wallet at the same time.

For this reason, I used a pessimistic write lock:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Wallet> findByUserId(UUID userId);
```

Hibernate generates a database query using:

```sql
FOR UPDATE
```

This makes requests updating the same wallet wait for each other instead of modifying the balance at the same time.

For example, if the wallet has ₹500 and ten requests try to debit ₹100 simultaneously, the requests are processed one at a time.

The result is:

```text
5 successful requests
5 insufficient-funds requests
Final balance: ₹0
```

---

## 6. Check the Balance After Locking the Wallet

The wallet is locked before checking its balance.

The processing flow is:

```text
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

This is important because the balance check and balance update happen while the wallet is locked.

Otherwise, multiple requests could read the same balance and all think that enough money is available.

---

## 7. Use @Transactional for Transaction Processing

The main transaction-processing method uses:

[//]: # (```java)

[//]: # (@Transactional)

[//]: # (```)

Wallet balance updates and transaction creation need to happen as one database operation.

If something goes wrong during processing, the database transaction can roll back instead of leaving the wallet and transaction record in an inconsistent state.

---

## 8. Return 409 for Duplicate Transactions

When a transaction ID has already been processed, the application throws:

[//]: # (```java)

[//]: # (DuplicateTransactionException)

[//]: # (```)

and the global exception handler returns:

[//]: # (```text)

[//]: # (HTTP 409 Conflict)

[//]: # (```)

I chose this approach instead of returning the original response because the assignment explicitly allows `409 Conflict` for duplicate requests.

---

## 9. Return 400 for Insufficient Funds

If the wallet does not have enough money for a debit, the application throws:

[//]: # ()
[//]: # (```java)

[//]: # (InsufficientFundsException)

[//]: # (```)

[//]: # ()
[//]: # (The API returns:)

[//]: # ()
[//]: # (```text)

[//]: # (HTTP 400 Bad Request)

[//]: # (```)

The wallet balance is not changed in this case.

---

## 10. Global Exception Handling

I used `@RestControllerAdvice` for handling application exceptions.

This keeps exception handling in one place instead of adding try-catch logic to the controller.

Currently it handles:

* Wallet not found
* Duplicate transaction
* Insufficient funds

---

## 11. Keep Business Logic in the Service Layer

The controller only handles the API request and passes it to the service.

The actual transaction logic is inside `TransactionService`.

The service handles:

* Finding the wallet
* Locking the wallet
* Checking duplicate transactions
* Checking the balance
* Updating the balance
* Creating the transaction record

This keeps the controller simple and makes the business logic easier to test.

---

## 12. Integration Tests with MockMvc

I used Spring Boot integration tests with MockMvc instead of depending on Postman.

This allows the reviewer to simply run the test class from IntelliJ.

The tests use the actual application layers:

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

No external database or manual API testing is required.

---

## 13. Testing Concurrent Requests

For the concurrency tests, I used:

* `ExecutorService`
* `Callable`
* `CountDownLatch`
* `Future`

`CountDownLatch` is used so that all worker threads can wait until they are ready and then start their requests together.

This helps reproduce the concurrent-request scenarios required by the assignment.

---

## 14. Test Results

### Test 1 — Single Debit

The test starts with:

```text
Initial balance: ₹500
Debit: ₹100
```

Result:

```text
Final balance: ₹400
```

---

### Test 2 — Duplicate Transaction

Three requests use the same transaction ID.

Result:

```text
Total requests: 3
Successful requests: 1
Duplicate requests: 2
Final balance: ₹750
```

This confirms that the same transaction is not processed more than once.

---

### Test 3 — Concurrent Debits

Ten requests try to debit ₹100 from a wallet containing ₹500.

Result:

```text
Total requests: 10
Successful requests: 5
Insufficient funds: 5
Final balance: ₹0
```

This confirms that concurrent requests cannot make the wallet balance negative.

---

## 15. Keep the Project Focused

I intentionally did not add features such as:

* JWT authentication
* Frontend
* MySQL
* Redis
* Message queues
* External services

These are not required for this assignment.

The implementation focuses on the required areas:

```text
Transaction processing
Idempotency
Concurrency
Database locking
Integration testing
```

This keeps the project simple enough to review while still addressing the main technical requirements.
