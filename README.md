# Feature Flag As A Service

A backend service for managing and evaluating feature flags, built with Java Spring Boot. Clients integrate via an SDK that calls the REST API, passing a feature flag name and a map of attributes. The service evaluates and returns `true` or `false`.

## Features

- **Simple Toggle**: Flag is on or off globally
- **Conditional Rules**: Flag returns `true` only when a formula over attributes is satisfied
- **Percentage Rollout**: Flag is enabled for a deterministic, sticky percentage of attribute combinations
- **Combined Modes**: All three modes can be combined

## Tech Stack

- Java 17
- Spring Boot 3.x
- Maven
- PostgreSQL via Spring Data JPA & Hibernate
- mvel2 for formula evaluation
- Jackson for JSON
- UUID for ID generation
- JUnit 5 with Mockito and H2 for testing

## Project Structure

```
src/
├── main/java/com/flagr/
│   ├── controller/          # REST endpoints
│   ├── service/             # Business logic
│   ├── repository/          # Data access layer
│   ├── model/               # JPA entities
│   ├── dto/                 # Request/Response DTOs
│   ├── exception/           # Custom exceptions & global handler
│   └── util/                # Utility classes (rollout hash, formula evaluation)
├── main/resources/
│   └── application.properties
└── test/
    ├── java/com/flagr/
    │   ├── service/         # Unit tests for services
    │   ├── util/            # Unit tests for utilities
    │   └── integration/     # Spring Boot integration tests
    └── resources/
        └── application-test.properties
```

## Data Model

### `feature_flag` Table

| Column          | Type      | Constraints                     | Description                                      |
|-----------------|-----------|---------------------------------|--------------------------------------------------|
| feature_flag_id | UUID      | PK, auto-generated              | Unique identifier                                |
| client          | String    | NOT NULL                        | Client identifier                                |
| name            | String    | NOT NULL                        | Flag name                                        |
| attributes      | JSONB     |                                 | Attribute schema: `{"user_id":"num","region":"string"}` |
| formula_string  | String    | Nullable                        | Formula for conditional evaluation               |
| rollout         | int       | Default 0                       | Rollout percentage (0-100)                       |
| enabled         | boolean   | Default false                   | Global toggle state                              |

Unique constraint on `(client, name)`.

### `feature_flag_rollout_key` Table

| Column          | Type      | Constraints                     | Description                                      |
|-----------------|-----------|---------------------------------|--------------------------------------------------|
| id              | UUID      | PK, auto-generated              | Unique identifier                                |
| feature_flag_id | UUID      | FK → feature_flag, NOT NULL     | Reference to parent flag                         |
| rollout_key     | String    | NOT NULL                        | Hex-encoded SHA-256 of attribute values          |
| threshold       | int       | Default 0                       | Rollout threshold percentage                     |

**Relationship**: `feature_flag` 1 — N `feature_flag_rollout_key`

## Rollout Key Computation

Sticky rollout is guaranteed by a deterministic hash:

1. Sort attribute keys alphabetically
2. Concatenate values as strings in sorted key order
3. SHA-256 hash the result
4. Take absolute value mod 100 → integer 0–99

Identical attribute values always produce the same hash, ensuring consistent (sticky) behavior.

## API Endpoints

### Flag Management

| Method   | Endpoint               | Description                        | Status Code |
|----------|------------------------|------------------------------------|-------------|
| POST     | `/flags`               | Create a new flag                  | 201         |
| PUT      | `/flags/{id}`          | Update an existing flag            | 200         |
| DELETE   | `/flags/{id}`          | Delete a flag                      | 204         |
| PATCH    | `/flags/{id}/toggle`   | Toggle enabled (simple flags only) | 200         |
| PUT      | `/flags/{id}/rollout`  | Set rollout percentage             | 200         |

### SDK Evaluation

| Method   | Endpoint          | Description                     | Status Code |
|----------|-------------------|---------------------------------|-------------|
| POST     | `/flags/evaluate` | Evaluate a flag for given attrs | 200         |

## Request/Response Examples

### Create Flag

```json
POST /flags
{
  "client": "my-app",
  "name": "dark-mode",
  "attributes": {
    "user_id": "num",
    "region": "string"
  },
  "formula_string": "user_id NOT IN (1,2,3) AND region != 'Canada'",
  "rollout": 50
}
```

### Evaluate Flag (SDK Call)

```json
POST /flags/evaluate
{
  "client": "my-app",
  "name": "dark-mode",
  "attributes": {
    "user_id": 42,
    "region": "US"
  }
}
```

Response:

```json
{
  "enabled": true
}
```

### Evaluate Flow

1. Fetch flag by `(client, name)` → 404 if absent
2. If `enabled = false` → return `false`
3. If `rollout > 0`:
   - Compute hash of request attributes
   - If hash >= threshold → return `false`
   - Persist new rollout key row if one does not exist
4. If `formula_string` is present:
   - Evaluate formula against attributes
   - If `false` → return `false`
5. Return `true`

## Supported Formula Operators (mvel2)

`IN`, `NOT IN`, `==`, `!=`, `&&`, `||`, `>`, `<`, `>=`, `<=`

## Configuration

### Production (`application.properties`)

Configure PostgreSQL datasource:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/flagr
spring.datasource.username=your_user
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

### Test (`application-test.properties`)

Uses H2 in-memory database with `ddl-auto=create-drop`.

## Running the Project

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Run tests
mvn test
```

## Error Handling

| Exception                   | HTTP Status | Description                        |
|-----------------------------|-------------|------------------------------------|
| `FlagNotFoundException`     | 404         | Flag does not exist                |
| `InvalidFlagOperationException` | 400     | Invalid operation (e.g., toggle on conditional flag) |

Response body:

```json
{
  "error": "Description of the error"
}
```
