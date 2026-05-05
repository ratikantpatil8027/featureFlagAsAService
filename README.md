# Flagr Service - Feature Flag as a Service

A Spring Boot 3.x backend for managing and evaluating feature flags with support for simple toggles, conditional rules, and percentage rollouts.

## Tech Stack

- **Java 17** / Spring Boot 3.2.0
- **Maven** for dependency management
- **PostgreSQL** via Spring Data JPA & Hibernate (H2 for testing)
- **MVEL2** for formula evaluation
- **Jackson** for JSON processing

## Project Structure

```
flagr-service/
├── pom.xml
├── src/main/
│   ├── java/com/flagr/
│   │   ├── FlagrServiceApplication.java
│   │   ├── controller/
│   │   │   ├── FeatureFlagController.java
│   │   │   └── FlagEvaluationController.java
│   │   ├── service/
│   │   │   ├── FeatureFlagService.java
│   │   │   ├── FeatureFlagRolloutService.java
│   │   │   └── FlagEvaluationService.java
│   │   ├── repository/
│   │   │   ├── FeatureFlagRepository.java
│   │   │   └── FeatureFlagRolloutKeyRepository.java
│   │   ├── model/
│   │   │   ├── FeatureFlag.java
│   │   │   └── FeatureFlagRolloutKey.java
│   │   ├── dto/
│   │   │   ├── CreateFlagRequest.java
│   │   │   ├── UpdateFlagRequest.java
│   │   │   ├── RolloutRequest.java
│   │   │   ├── EvaluateRequest.java
│   │   │   ├── EvaluateResponse.java
│   │   │   └── FlagResponse.java
│   │   ├── util/
│   │   │   ├── RolloutKeyUtil.java
│   │   │   └── FormulaEvaluatorUtil.java
│   │   └── exception/
│   │       ├── FlagNotFoundException.java
│   │       ├── InvalidFlagOperationException.java
│   │       └── GlobalExceptionHandler.java
│   └── resources/
│       └── application.properties
└── src/test/
    ├── java/com/flagr/
    │   ├── util/
    │   │   ├── RolloutKeyUtilTest.java
    │   │   └── FormulaEvaluatorUtilTest.java
    │   ├── service/
    │   │   ├── FeatureFlagServiceTest.java
    │   │   ├── FeatureFlagRolloutServiceTest.java
    │   │   └── FlagEvaluationServiceTest.java
    │   └── integration/
    │       └── FlagIntegrationTest.java
    └── resources/
        ├── application-test.properties
        └── mockito-extensions/org.mockito.plugins.MockMaker
```

## API Documentation

### Flag Management

#### Create Flag

```
POST /flags
```

**Request Body:**
```json
{
  "client": "my-app",
  "name": "dark-mode",
  "attributes": {
    "user_id": "num",
    "region": "string"
  },
  "formulaString": "region != 'Canada'",
  "rollout": 0
}
```

- `client` (string, required): Client identifier
- `name` (string, required): Flag name (unique per client)
- `attributes` (object, required): Schema mapping attribute names to types (`"num"` or `"string"`)
- `formulaString` (string, optional): MVEL2 formula for conditional evaluation
- `rollout` (int, optional): Default 0

**Response:** `201 Created`
```json
{
  "featureFlagId": "uuid-string",
  "client": "my-app",
  "name": "dark-mode",
  "attributes": "{\"user_id\":\"num\",\"region\":\"string\"}",
  "formulaString": "region != 'Canada'",
  "rollout": 0,
  "enabled": true
}
```

#### Update Flag

```
PUT /flags/{id}
```

**Request Body:** (all fields optional)
```json
{
  "client": "my-app",
  "name": "dark-mode-updated",
  "attributes": {
    "user_id": "num"
  },
  "formulaString": "user_id > 10",
  "rollout": 50
}
```

**Response:** `200 OK` with updated `FlagResponse`

#### Delete Flag

```
DELETE /flags/{id}
```

**Response:** `204 No Content`

#### Toggle Flag

```
PATCH /flags/{id}/toggle
```

Flips the `enabled` state. Only works on simple flags (no `formulaString`).

**Response:** `200 OK` with updated `FlagResponse`

**Error:** `400 Bad Request` if flag has a formula (use evaluation endpoint instead)

#### Set Rollout Percentage

```
PUT /flags/{id}/rollout
```

**Request Body:**
```json
{
  "percentage": 50
}
```

- `percentage` (int, required): Value between 0 and 100

**Response:** `200 OK`

### SDK Evaluation Endpoint

#### Evaluate Flag

```
POST /flags/evaluate
```

**Request Body:**
```json
{
  "client": "my-app",
  "name": "dark-mode",
  "attributes": {
    "user_id": 42,
    "region": "US"
  }
}
```

- `client` (string, required): Client identifier
- `name` (string, required): Flag name
- `attributes` (object, required): Actual attribute values

**Response:** `200 OK`
```json
{
  "enabled": true
}
```

**Error:** `404 Not Found` if flag does not exist

### Evaluation Flow

1. Fetch flag by `(client, name)` - return 404 if not found
2. If `enabled = false`, return `{"enabled": false}`
3. If `rollout > 0`:
   - Compute SHA-256 hash of sorted attribute values, mod 100
   - If hash >= threshold, return `{"enabled": false}`
   - Persist new rollout key row if one doesn't exist
4. If `formulaString` is present:
   - Evaluate formula against attributes via MVEL2
   - If false, return `{"enabled": false}`
5. Return `{"enabled": true}`

### Formula Syntax (MVEL2)

Supported operators:

| Operator | Example |
|----------|---------|
| `==` | `region == 'US'` |
| `!=` | `region != 'Canada'` |
| `>` | `user_id > 10` |
| `<` | `user_id < 100` |
| `>=` | `user_id >= 18` |
| `<=` | `user_id <= 65` |
| `&&` | `user_id > 10 && region == 'US'` |
| `\|\|` | `region == 'US' \|\| region == 'CA'` |
| `in` | `user_id in {1,2,3}` |
| `not in` | `user_id not in {1,2,3}` |

### Rollout Key Computation

1. Sort attribute keys alphabetically
2. Concatenate values as strings in sorted key order
3. SHA-256 hash the concatenated string
4. Take first 4 bytes as integer, `Math.abs() % 100`

Same attribute values always produce the same hash, guaranteeing sticky rollout behavior.

## Running

```bash
# Build and run all tests
mvn clean test

# Run with PostgreSQL
mvn spring-boot:run -Dspring.profiles.active=default
```

## Configuration

### Production (`application.properties`)

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/flagr_db
spring.datasource.username=flagr_user
spring.datasource.password=flagr_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

### Test (`application-test.properties`)

```properties
spring.datasource.url=jdbc:h2:mem:flagrdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
```

## Database Schema

### feature_flag

| Column | Type | Constraints |
|--------|------|-------------|
| feature_flag_id | UUID | PK, auto-generated |
| client | VARCHAR | NOT NULL |
| name | VARCHAR | NOT NULL |
| attributes | JSONB | Attribute schema |
| formula_string | VARCHAR | Nullable |
| rollout | INT | Default 0 |
| enabled | BOOLEAN | Default false |

Unique constraint on `(client, name)`.

### feature_flag_rollout_key

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK, auto-generated |
| feature_flag_id | UUID | FK to feature_flag, NOT NULL |
| rollout_key | VARCHAR | NOT NULL (hex SHA-256) |
| threshold | INT | Default 0 |
