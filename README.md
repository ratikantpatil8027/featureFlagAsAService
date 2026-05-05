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

---

## Original Specification

### Statement

You are building a Feature Flag as a Service backend in Java Spring Boot. Clients integrate via an SDK that calls your REST API. Each SDK call includes a feature flag name and a map of attributes such as user_id, region, account_id. Your service evaluates and returns true or false. The service supports three modes: simple toggle where flag is on or off globally, conditional rules where flag returns true only when a formula over attributes is satisfied, and percentage rollout where flag is enabled for a deterministic sticky percentage of attribute combinations. All three modes can combine. The service stores state in two tables feature_flag and feature_flag_rollout_key.

### Requirements

- **System capabilities:** Java 17, Spring Boot 3.x, Maven, PostgreSQL via Spring Data JPA and Hibernate, mvel2 for formula evaluation, Jackson for JSON, UUID for ID generation, JUnit 5 with Mockito and H2 for testing.
- **feature_flag table:** feature_flag_id UUID primary key auto generated, client String not null, name String not null, attributes JSONB storing attribute schema as key to type map example `{"user_id":"num","region":"string"}`, formula_string String nullable example `"user_id NOT IN (1,2,3) AND region != 'Canada'"`, rollout int default 0, enabled boolean default false. Unique constraint on client and name pair.
- **feature_flag_rollout_key table:** id UUID primary key auto generated, feature_flag_id UUID foreign key referencing feature_flag, rollout_key String storing hex encoded SHA-256 of attribute values, threshold int default 0.
- **Rollout key computation:** sort attribute keys alphabetically, concatenate values as strings in that order, SHA-256 hash the result, take absolute value mod 100 to get integer 0 to 99. Same attribute values always produce same hash. This guarantees sticky rollout.
- **API surface:** POST /flags to create, PUT /flags/{id} to update, DELETE /flags/{id} to delete, PATCH /flags/{id}/toggle to flip enabled on simple flags only, PUT /flags/{id}/rollout to set rollout percentage, POST /flags/evaluate as the SDK endpoint accepting client and name and attributes map returning enabled boolean.
- **Evaluate flow:** fetch flag by client and name, return false if enabled is false, if rollout greater than 0 compute hash of request attributes and compare to threshold and return false if hash is greater than or equal to threshold and persist new rollout key row if one does not exist, if formula_string is present evaluate it against attributes and return false if it evaluates to false, otherwise return true.

### Data Flow

**CREATE FLAG**
```
request {client, name, attributes, formula_string, rollout=0}
  -> validate attributes schema
  -> validate formula_string via dry-run mvel2 parse
  -> persist feature_flag row (enabled=false, rollout=0)
  -> persist feature_flag_rollout_key row (rollout_key=hash of empty context, threshold=0)
  -> return feature_flag_id UUID
```

**UPDATE FLAG**
```
request {feature_flag_id, updatable fields}
  -> fetch flag or throw FlagNotFoundException
  -> re-validate formula if changed
  -> update and persist
  -> return updated FlagResponse
```

**DELETE FLAG**
```
request {feature_flag_id}
  -> fetch flag or throw FlagNotFoundException
  -> delete rollout key rows
  -> delete flag row
```

**TOGGLE FLAG**
```
request {feature_flag_id}
  -> fetch flag or throw FlagNotFoundException
  -> reject if formula_string is present with InvalidFlagOperationException
  -> flip enabled boolean
  -> persist and return updated FlagResponse
```

**SET ROLLOUT**
```
request {feature_flag_id, percentage 0 to 100}
  -> validate percentage range
  -> update feature_flag.rollout
  -> update feature_flag_rollout_key.threshold
  -> persist both
```

**EVALUATE FLAG (SDK call)**
```
request {client, name, attributes map}
  -> fetch feature_flag by (client, name) or 404
  -> if enabled=false return false
  -> if rollout > 0
       compute hashVal = SHA256(sorted attr values) mod 100
       compute key = hex SHA256 string
       fetch rollout key row for flag
       if row exists and hashVal >= threshold return false
       if row does not exist persist new row with current threshold
  -> if formula_string present
       evaluate formula against attributes via mvel2
       if false return false
  -> return true
```

**RELATIONS**
```
feature_flag 1 --- N feature_flag_rollout_key via feature_flag_id
each flag stores its attribute schema
each unique attribute combination gets its own rollout_key row
```

### Code Structure and Implementation Prompts

**Prompt 1 - Project Scaffold**

Create a Spring Boot 3.x Maven project named flagr-service.

Dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, postgresql runtime, jackson-databind, mvel2, spring-boot-starter-validation, lombok, h2 test scope, spring-boot-starter-test.

Package structure under com.flagr: controller, service, repository, model, dto, util, exception

Create application.properties with placeholder PostgreSQL datasource config. Create application-test.properties using H2 in-memory datasource with ddl-auto=create-drop.

**Prompt 2 - Models and Repositories**

In com.flagr.model create two JPA entities.

FeatureFlag fields: feature_flag_id UUID @Id @GeneratedValue, client String @NotNull, name String @NotNull, attributes String @Column(columnDefinition="jsonb"), formula_string String nullable, rollout int default 0, enabled boolean default false. Add @Table unique constraint on (client, name).

FeatureFlagRolloutKey fields: id UUID @Id @GeneratedValue, featureFlag @ManyToOne @JoinColumn feature_flag_id not null, rollout_key String not null, threshold int default 0.

In com.flagr.repository create FeatureFlagRepository extending JpaRepository with method `Optional<FeatureFlag> findByClientAndName(String client, String name)`. Create FeatureFlagRolloutKeyRepository extending JpaRepository with method `Optional<FeatureFlagRolloutKey> findByFeatureFlag(FeatureFlag flag)`.

**Prompt 3 - DTOs and Exception Handling**

In com.flagr.dto create:
- CreateFlagRequest: client String, name String, attributes Map<String,String>, formula_string String nullable, rollout int default 0.
- UpdateFlagRequest: same fields all nullable.
- RolloutRequest: percentage int.
- EvaluateRequest: client String, name String, attributes Map<String,Object>.
- EvaluateResponse: enabled boolean.
- FlagResponse: mirroring all FeatureFlag entity fields including feature_flag_id.

In com.flagr.exception create FlagNotFoundException and InvalidFlagOperationException extending RuntimeException.

Create GlobalExceptionHandler with @RestControllerAdvice mapping FlagNotFoundException to 404 and InvalidFlagOperationException to 400.

**Prompt 4 - RolloutKeyUtil**

computeRolloutHash(Map<String,Object> attributes) returns int 0 to 99: sort attribute keys alphabetically, concatenate values as strings in sorted key order, SHA-256 hash, take absolute value of first four bytes mod 100.

generateRolloutKey(Map<String,Object> attributes) returns String: same SHA-256 pipeline, return full hex-encoded hash string.

Sticky guarantee: identical attribute values always produce the same hash and key regardless of call order or time.

**Prompt 5 - FormulaEvaluatorUtil**

validate(String formulaString, Map<String,String> attributeSchema): build dummy MVEL context from schema, call MVEL.eval, throw InvalidFlagOperationException on failure.

evaluate(String formulaString, Map<String,Object> attributeValues): populate MVEL context from attributeValues, return Boolean result, throw InvalidFlagOperationException on error.

mvel2 supports IN, NOT IN, ==, !=, &&, ||, >, <, >=, <= natively.

**Prompt 6 - FeatureFlagService**

- createFlag: validate formula, save flag (enabled=false, rollout=0), save rollout key (hash of empty map, threshold=0).
- updateFlag: fetch or throw, re-validate formula if changed, update non-null fields.
- deleteFlag: fetch or throw, delete rollout keys, delete flag.
- toggleFlag: fetch or throw, reject if formula present, flip enabled.

**Prompt 7 - FeatureFlagRolloutService**

setRollout: validate 0-100 range, fetch flag, set flag.rollout, fetch rollout key row, set threshold.

**Prompt 8 - FlagEvaluationService**

- Step 1: fetch by (client, name), throw 404 if absent.
- Step 2: if disabled return false.
- Step 3: if rollout > 0, compute hash, compare to threshold, persist new key row if absent.
- Step 4: if formula present, evaluate, return false if false.
- Step 5: return true.

**Prompt 9 - Controllers**

FeatureFlagController: POST / (201), PUT /{id} (200), DELETE /{id} (204), PATCH /{id}/toggle (200), PUT /{id}/rollout (200).

FlagEvaluationController: POST /evaluate (200).

Use @Valid on all @RequestBody parameters.

### Verification Prompts

**Prompt 10 - Unit Tests for Utilities**

RolloutKeyUtil: assert same attributes return same hash across 3 calls, 5 distinct maps return 5 values between 0-99, different insertion order returns same hash, empty map returns valid int.

FormulaEvaluatorUtil: validate passes for valid formula, throws for malformed, evaluate returns true/false for IN/NOT IN, combined conditions, region != 'Canada' with matching/non-matching attributes.

**Prompt 11 - Unit Tests for Services**

FeatureFlagService: createFlag persists and returns non-null id, invalid formula throws before save, toggle flips on simple flag, toggle throws on conditional flag, delete throws for unknown UUID.

FlagEvaluationService: returns false when disabled, returns false when hash >= threshold, returns true when hash < threshold, creates new rollout key when absent, returns false on formula false, returns true on all clear, identical attributes return same result (sticky).

**Prompt 12 - Integration Tests**

Flag management: POST returns 201 with feature_flag_id, PUT with updated formula returns 200, DELETE returns 204 and evaluate returns 404, toggle simple flag returns 200, toggle conditional flag returns 400.

Rollout and sticky: set rollout to 50, evaluate 100 times with random user_id, true count between 35-65, identical attributes return same result.

Sticky on rollout increase: record true maps at 20%, increase to 60%, re-evaluate and assert all previously true maps still return true.

Conditional: formula "region != 'Canada'" evaluates true for US and false for Canada. Formula "user_id IN {1,2,3}" evaluates true for 2 and false for 5.
