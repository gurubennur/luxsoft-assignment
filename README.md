# Banking Transaction Processor

A Java 17 / Spring Boot 3 REST API for accounts, deposits, withdrawals, transfers, and per-account transaction history. The PDF task asks for the banking kata; this project keeps the core operations small and makes the ledger the source of transaction history.

## Run locally

Requirements: JDK 17+, Maven 3.6+, and Docker Compose.

1. Start MySQL: `docker compose up -d mysql`
2. Start the API: `mvn spring-boot:run`
3. Open Swagger UI at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

The default local database credentials are `root` / `root`. Override them with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Flyway applies the SQL schema on startup; Hibernate validates it rather than changing the schema. Do not use the sample credentials outside local development.

## API

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/v1/accounts` | Create an account with holder, account number, branch, IFSC, postal code, and opening balance details (zero is allowed) |
| GET | `/api/v1/accounts/{accountNumber}` | Read account balance and metadata |
| POST | `/api/v1/accounts/{accountNumber}/deposits` | Deposit a positive amount |
| POST | `/api/v1/accounts/{accountNumber}/withdrawals` | Withdraw while leaving a positive balance |
| POST | `/api/v1/transfers` | Atomically transfer between distinct accounts |
| GET | `/api/v1/accounts/{accountNumber}/transactions?page=0&size=20` | Read paginated newest-first ledger |

The account number is the account primary key and appears in account paths and ledger rows; there is no separate account ID. Amounts use decimal currency with up to two fractional digits. The account open date is assigned on creation. `pinCode` is the postal code and is stored as account address metadata. IFSC values are normalized to uppercase. Withdrawals and transfers must leave a positive source balance; rejected operations do not change balances or write ledger entries. `updatedAt` refreshes on account changes; `modifiedBy` and `updatedBy` default to `system` because authentication is outside this kata. Transfers produce two ledger entries sharing a reference ID. Updates lock account rows; transfers acquire both locks in account-number order to reduce deadlocks when opposite transfers happen concurrently. Each entry stores the account balance immediately after that entry.

Example requests:

```json
{
  "accountNumber": "123456789012345",
  "accountHolderName": "Asha Sharma",
  "pinCode": "560001",
  "ifsc": "ABCD0123456",
  "branch": "Central Branch",
  "openingBalance": 250.00
}
```

```json
{ "amount": 25.50 }
```

```json
{
  "sourceAccountNumber": "123456789012345",
  "destinationAccountNumber": "987654321098765",
  "amount": 20.00
}
```

## MySQL schema and useful queries

The schema is versioned in `src/main/resources/db/migration/`. V1 creates the original schema; V2 migrates existing account and ledger rows so account number becomes the primary key, then adds account audit columns. V3 renames the ledger timestamp to `created_at` and adds `updated_at`. The account table includes holder name, open date, postal code, IFSC, branch, balance, and audit metadata. It uses InnoDB, decimal balances, foreign keys, non-negative balance and positive transaction amount constraints, and indexes for account history and transfer references.

```sql
-- Account details and current balance
SELECT account_number, account_holder_name, account_open_date,
       pin_code, ifsc, branch, balance, updated_at, modified_by, updated_by
FROM accounts
WHERE account_number = ?;

-- Newest ledger entries for an account (use keyset pagination for large histories)
SELECT id, account_number, type, amount, balance_after, created_at, updated_at,
       BIN_TO_UUID(reference_id) AS reference_id
FROM ledger_entries
WHERE account_number = ?
ORDER BY created_at DESC, id DESC
LIMIT ? OFFSET ?;

-- Both sides of a transfer
SELECT account_number, type, amount, created_at, updated_at
FROM ledger_entries
WHERE reference_id = UUID_TO_BIN(?)
ORDER BY id;
```

Account numbers are the primary keys and are stored as strings. Ledger reference IDs remain UUIDs stored as MySQL `BINARY(16)`.

## Design notes and limits

- Transaction boundaries live in the service layer. Pessimistic row locks serialize writes to the same account; a transfer locks accounts in deterministic order and writes both ledger sides in one database transaction.
- The API controller depends on the `AccountService` interface; `AccountServiceImpl` contains the business logic and persistence coordination.
- ModelMapper configuration is centralized in `ModelMapperConfiguration`; it maps account creation requests to entities and account/ledger entities to response models. The public JSON field remains `openingBalance`.
- The ledger records the post-operation balance for quick auditing. For a regulated system, ledger corrections should be compensating entries rather than updates or deletes.
- Authentication, authorization, idempotency keys, currency selection, and reconciliation are intentionally outside this kata. A client retry after a network timeout can repeat a request; production APIs should accept idempotency keys.
- Flyway migrations are the schema authority. A non-zero opening balance is recorded as an `OPENING_BALANCE` ledger entry; a zero opening balance creates no entry.

## Tests

`AccountServiceTest` documents deposit and overdraft behavior. Run with `mvn test`. Integration tests against a real MySQL instance would be a useful next step to exercise database locks, migrations, and rollback.
