# Login and JWT Authentication Specification

## 1. Purpose and Scope

This document specifies JWT-based login authentication for Rastro's backend. JWT is mandatory for this project and must not be replaced with session-based authentication.

This specification covers:

- Login with email and password.
- Password verification against a secure stored hash.
- JWT issuance and validation.
- Authentication and role-based authorization.
- Active and blocked account behavior.
- REST error responses.
- The ordered TDD implementation plan.

The specification does not implement any source code, dependency, configuration, or database change.

## 2. Current Repository State

This section describes the backend state after US-01 User Registration (#47) was merged into `develop`.

### 2.1 Architecture

Rastro-backend uses Java 21, Spring Boot, Maven, Hexagonal Architecture, and Domain-Driven Design.

The user feature is organized into domain, application, and infrastructure layers.

US-02 Login must reuse existing components without duplicating registration functionality.

### 2.2 User Domain

The following domain types are already implemented:

- `User`
- `UserId`
- `Email`
- `Password`
- `PasswordHash`
- `UserRole`
- `AccountStatus`

The domain must remain independent of Spring Security, JWT libraries, JPA, and HTTP.

### 2.3 Registration

US-01 User Registration (#47) has been completed and merged.

Existing functionality includes:

- User registration through a REST endpoint.
- Email normalization and validation.
- Duplicate email detection.
- Password validation and BCrypt hashing.
- Default `USER` role and `ACTIVE` account status.
- PostgreSQL persistence.
- Unit and integration tests.

Login must not modify registration behavior unless an explicitly approved integration requirement makes a change necessary.

### 2.4 Persistence

The existing application provides:

- `UserLookupPort`
- `UserCreationPort`
- JPA persistence entities and repositories.
- Domain-to-persistence mapping.
- PostgreSQL integration through Spring Data JPA.

Login shall reuse `UserLookupPort.findByEmail(Email)`.

### 2.5 Database and Migrations

The database uses PostgreSQL and Flyway.

The canonical initial migration is:

`src/main/resources/db/migration/V1__initial_schema.sql`

The existing schema supports UUID user identifiers, password hashes, user roles, and account statuses.

US-02 does not currently require a new database migration.

### 2.6 Password Security

Registration already uses BCrypt through the `PasswordHasher` abstraction.

Login shall introduce a separate `PasswordVerifier` port and a BCrypt-based infrastructure adapter.

Password verification must be compatible with hashes generated during registration.

### 2.7 Testing

US-01 completed its verification with 65 passing tests before merging.

Existing test infrastructure includes JUnit, Mockito, Spring Boot integration testing, and Testcontainers.

US-02 must add focused authentication tests and preserve the existing registration test suite.

### 2.8 Authentication Components Pending Implementation

The following components are not yet implemented in the current feature branch:

- `LoginUserUseCase`
- `LoginResult`
- `AuthenticationFailedException`
- `PasswordVerifier`
- `TokenIssuer`
- BCrypt password verification adapter
- JWT issuance and validation
- Spring Security configuration
- Login REST controller
- Login unit and integration tests

These components may be adapted selectively from the preserved backup after specification approval.

## 3. Functional Requirements

### FR-01 Login endpoint

The backend shall expose:

```text
POST /api/v1/auth/login
```

The endpoint shall accept an email and raw password, authenticate the account, and return a signed JWT after successful authentication.

### FR-02 Email normalization

The login email shall use the same normalization rules as the domain `Email` value object: trim surrounding whitespace and compare the normalized lowercase value.

### FR-03 Credential verification

The backend shall retrieve the user by email and verify the submitted raw password against the stored password hash through an abstraction. Raw passwords must never be persisted or returned.

### FR-04 Active accounts

An active user with valid credentials shall receive an access token.

### FR-05 Blocked accounts

A blocked account shall not receive an access token, even when the submitted credentials are correct.

### FR-06 Generic authentication failure

Unknown email, incorrect password, and blocked account shall return HTTP 401 with error code `AUTHENTICATION_FAILED`.

The response must not reveal whether an email exists.

### FR-07 Request validation

Missing, blank, malformed, or invalid request fields shall produce a client validation error and no token.

### FR-08 JWT claims

An issued JWT shall contain, at minimum:

- `sub`, containing the user's UUID.
- `role`, containing the user's role.
- `iss`, containing the issuer.
- `iat`, containing the issued-at timestamp.
- `exp`, containing the expiration timestamp.

The final claim names must be consistent across issuance and validation.

### FR-09 Protected endpoints

Protected endpoints shall require a valid, unexpired, correctly signed JWT.

Missing, malformed, expired, tampered, or otherwise invalid tokens shall be rejected.

### FR-10 Authorization

Authentication identifies the user. Authorization determines whether the authenticated user may perform an operation.

The `USER` and `ADMIN` roles shall be available for endpoint authorization. A `USER` must not access an `ADMIN`-only endpoint, while an `ADMIN` may access it.

### FR-11 Stateless authentication

The JWT authentication flow shall be stateless. The backend shall not use server-side HTTP sessions as the primary authentication mechanism.

### FR-12 Error confidentiality

Authentication and token errors shall not expose:

- Passwords.
- Password hashes.
- Signing keys.
- Stack traces.
- Whether a user email exists.

## 4. Security Requirements

### SR-01 Password hashing

Passwords shall be hashed with a dedicated password hashing implementation. Manual cryptographic hashing must not be introduced.

The recommended implementation is BCrypt through Spring Security. The concrete encoder belongs to infrastructure, while the application layer depends on a password verification abstraction.

### SR-02 JWT signature validation

The backend shall sign tokens with HS256 and validate the JWT signature before trusting claims. It shall explicitly accept only HS256 and must not accept an algorithm selected arbitrarily by the token header.

### SR-03 JWT time validation

The backend shall validate `issued-at` and expiration semantics according to the configured clock and allowed clock skew.

### SR-04 Issuer and Audience Validation

The backend shall validate that every accepted JWT contains the issuer (`iss`) value `rastro-api`.

Tokens with a missing or different issuer must be rejected.

JWT audience (`aud`) validation is not required for the MVP.

If future integrations require audience validation, this behavior must be introduced through a separately approved specification.

### SR-05 Secret management

Signing secrets or keys shall be supplied through environment-based configuration or a secret manager. They must not be committed to source control or hard-coded in Java classes.

The application should fail to start when mandatory production signing configuration is missing.

### SR-06 Token lifetime

The access-token lifetime is 15 minutes. The value shall be configurable outside the source code.

### SR-07 Account status and claims

The user status and role shall be checked during login. A token issued before a later role change or account block may remain technically valid until expiration unless a revocation strategy is introduced.

### SR-08 Token transport

The approved MVP transport is to return the access token in the JSON response body and have the frontend send it through:

```http
Authorization: Bearer <token>
```

The specification does not prescribe `localStorage` as a storage mechanism. The frontend integration must choose a safe storage strategy.

An HttpOnly, Secure, SameSite cookie is an alternative outside the scope of this MVP. It would require an explicit CSRF design.

### SR-09 Logout and revocation

The initial MVP will not provide server-side immediate JWT revocation or a token blacklist. Client-side logout removes the locally held token.

Refresh tokens, token rotation, and immediate revocation are outside the initial scope.

## 5. REST API Contract

### 5.1 Login request

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "password": "Valid123"
}
```

### 5.2 Successful response

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "accessToken": "<signed-jwt>",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

`expiresIn` is expressed in seconds.

The response must not include the raw password or stored password hash.

### 5.3 Authentication failure

```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json
```

```json
{
  "code": "AUTHENTICATION_FAILED",
  "message": "Authentication failed"
}
```

This response is used for unknown email, incorrect password, and blocked account under the recommended anti-enumeration policy.

### 5.4 Request validation failure

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json
```

```json
{
  "code": "INVALID_REQUEST",
  "message": "The login request is invalid",
  "fieldErrors": [
    {
      "field": "email",
      "message": "must not be blank"
    }
  ]
}
```

The exact validation message format may follow the project-wide error contract if one is introduced later.

### 5.5 Protected endpoint errors

- Missing, malformed, expired, or invalid token: `401 Unauthorized`.
- Valid token with insufficient role: `403 Forbidden`.

## 6. Proposed Architecture

The design keeps the domain independent from Spring, JPA, HTTP, and JWT libraries.

### 6.1 Application ports and use case

Location:

`src/main/java/cat/itacademy/rastro/Rastro_backend/user/application`

Existing component to reuse:

- `UserLookupPort`: already implemented during US-01 Register. Login shall reuse `findByEmail(Email)`.

New components for US-02:

- `LoginUserUseCase`: coordinates user authentication.
- `PasswordVerifier`: verifies a raw password against an existing `PasswordHash`.
- `TokenIssuer`: issues an access token for an authenticated user.
- `LoginResult`: represents the successful authentication result.
- `AuthenticationFailedException`: represents a generic authentication failure.

The application layer shall depend on abstractions rather than concrete Spring Security, JWT, or persistence implementations.

`LoginUserUseCase` shall:

1. Receive an already validated and normalized `Email` value object from the input adapter.
2. Load the user through `UserLookupPort`.
3. Use the same generic failure policy when the user does not exist.
4. Reject blocked accounts without disclosing account existence.
5. Verify the password through `PasswordVerifier`.
6. Issue a token through `TokenIssuer` only after successful verification.

The use case must not depend on `HttpServletRequest`, `SecurityContext`, `JwtEncoder`, JPA entities, or concrete Spring adapters.

### 6.2 Existing Persistence Adapter

The persistence infrastructure was implemented during US-01 Register.

Existing components include:

- JPA persistence entities.
- Spring Data JPA repositories.
- Domain-to-persistence mappers.
- Persistence adapters implementing application ports.
- PostgreSQL persistence configured with Flyway migrations.

US-02 Login shall reuse the existing persistence implementation through `UserLookupPort.findByEmail(Email)`.

No duplicate user repository, JPA entity, or persistence mapper shall be introduced for Login.

Additional persistence changes are permitted only if an approved authentication requirement cannot be satisfied by the existing implementation.

### 6.3 Password security adapter

Proposed location:

`src/main/java/cat/itacademy/rastro/Rastro_backend/user/infrastructure/security`

Responsibilities:

- Implement `PasswordVerifier` with a secure encoder.
- Keep Spring Security and BCrypt-specific code outside the domain.
- Never expose the stored hash through the REST API.

Registration already uses the existing PasswordHasher port and its BCrypt infrastructure adapter. Login shall reuse the compatible BCrypt encoding mechanism through a separate PasswordVerifier port.

### 6.4 JWT adapter

Proposed location:

`src/main/java/cat/itacademy/rastro/Rastro_backend/shared/infrastructure/security`

Responsibilities:

- Implement `TokenIssuer`.
- Configure the signing algorithm and key material.
- Add `sub` with the user's UUID, `role`, issuer, issued-at, and expiration claims.
- Validate issuer, expiration, signature, and allowed algorithm.
- Convert validated claims into the Spring Security authenticated identity.

The application layer must not depend on the concrete JWT library.

### 6.5 Spring Security Configuration

Proposed location:

`src/main/java/cat/itacademy/rastro/Rastro_backend/shared/infrastructure/security`

Responsibilities:

- Configure a stateless `SecurityFilterChain`.
- Permit `POST /api/v1/auth/login` without authentication.
- Preserve public access to the existing US-01 registration endpoint, using the exact route defined in `RegisterController`.
- Protect endpoints that require authentication.
- Configure JWT authentication using Bearer tokens.
- Validate JWT signatures, expiration, issuer (`rastro-api`), and the explicitly allowed HS256 algorithm.
- Map authentication failures to HTTP `401 Unauthorized`.
- Map authorization failures to HTTP `403 Forbidden`.
- Support role-based authorization for `USER` and `ADMIN`.
- Configure CORS explicitly when the frontend origin is known.
- Configure CSRF protection appropriately for stateless Bearer-token authentication.
- Ensure Spring Security integration does not break existing registration functionality or tests.

Security requirements:

- Authentication must remain stateless (`SessionCreationPolicy.STATELESS`).
- Login and registration endpoints must remain publicly accessible.
- Protected endpoints must reject missing, invalid, expired, or tampered JWTs.
- Authorization must distinguish between authenticated users and users with sufficient permissions.
- Security configuration must not expose sensitive authentication details.
- Existing US-01 registration tests must continue to pass after introducing Spring Security.

The security configuration shall reuse existing application and persistence components whenever possible, without introducing duplicate authentication or user persistence logic.

### 6.6 REST adapter

Proposed location:

`src/main/java/cat/itacademy/rastro/Rastro_backend/user/infrastructure/web`

Responsibilities:

- Accept and validate login request DTOs.
- Invoke `LoginUserUseCase`.
- Return the login response DTO.
- Avoid embedding authentication logic in the controller.
- Avoid returning domain exceptions or infrastructure details directly.

### 6.7 Error handling

A shared web error handler should provide stable responses for:

- Invalid request data.
- Invalid credentials.
- Blocked accounts.
- Missing or invalid JWTs.
- Insufficient permissions.
- Unexpected server failures.

Authentication responses must remain generic enough to prevent account enumeration.

## 7. Dependencies and Configuration

### 7.1 Existing Dependencies

The current backend already provides the dependencies and infrastructure required for US-01 User Registration, including:

- Spring MVC for REST endpoints.
- Spring Data JPA for persistence.
- PostgreSQL database connectivity.
- Flyway database migrations.
- Spring Boot testing infrastructure.
- JUnit and Mockito for unit testing.
- Testcontainers for integration testing.
- BCrypt password hashing through the existing registration infrastructure.

US-02 Login shall reuse the existing BCrypt implementation and compatible password encoding configuration.

The current `pom.xml` must be inspected before adding new dependencies to avoid duplication.

### 7.2 Proposed dependencies

The implementation will likely require:

- `spring-boot-starter-security`
- `spring-security-oauth2-resource-server`
- `spring-security-oauth2-jose`

These dependencies should be added only during the approved implementation phase and after verifying compatibility with the configured Spring Boot version.

Using Spring Security's resource-server and JOSE support is preferred over introducing an unrelated JWT library, provided the selected Spring Boot version supports the required APIs. The implementation must configure HS256 explicitly.

### 7.3 JWT Configuration

The JWT implementation shall use the following configuration:

| Property | Value | Requirement |
|---|---|---|
| `JWT_ISSUER` | `rastro-api` | Approved issuer |
| `JWT_SECRET` | Base64-encoded cryptographically random secret | Decoded key must contain at least 32 bytes (256 bits) for HS256 |
| `JWT_ACCESS_TOKEN_TTL_SECONDS` | `900` | 15-minute access token lifetime |

The JWT signing algorithm shall be HS256.

JWT validation shall enforce:

- A valid cryptographic signature.
- The explicitly configured HS256 algorithm.
- The issuer `rastro-api`.
- Token expiration.
- Required authentication claims.

Audience validation is not required for the MVP.

The signing secret must never be hard-coded, committed to Git, or exposed through API responses or application logs.

JWT configuration must be externalized through environment variables or an equivalent secure configuration mechanism.

All mandatory JWT configuration properties must be validated at application startup.

The `JWT_SECRET` environment variable must contain a Base64-encoded cryptographically secure random key.

The application shall decode the configured Base64 value before using it for HS256 signing and validation.

The decoded signing key must contain at least 32 bytes (256 bits).

The application must fail to start if `JWT_SECRET` is missing, cannot be decoded, or produces a key shorter than 32 bytes.

JWT signing and validation must use the same decoded key material.

## 8. Acceptance Criteria

### AC-01 Successful login

Given an active user with valid credentials, when the client posts to `/api/v1/auth/login`, the backend returns `200`, a Bearer access token, and the configured expiration.

### AC-02 Unknown email

Given no user exists for an email, login returns `401 AUTHENTICATION_FAILED` without revealing whether the email exists.

### AC-03 Incorrect password

Given an existing active user, an incorrect password returns the same generic authentication failure response as an unknown email.

### AC-04 Blocked account

Given a blocked user, even valid credentials return no token and the generic authentication failure response.

### AC-05 Request validation

Missing or invalid email/password fields return `400 INVALID_REQUEST` and no token.

### AC-06 JWT integrity

The backend rejects tokens that are expired, tampered with, signed with an untrusted key, issued by an invalid issuer, or use an unsupported algorithm.

### AC-07 Protected endpoint

A protected endpoint accepts a valid token and rejects a missing or invalid token with `401`.

### AC-08 Role authorization

An authenticated `USER` receives `403` for an `ADMIN`-only operation, while an authenticated `ADMIN` is accepted.

### AC-09 Secret handling

No password, password hash, signing key, or stack trace is returned in an API response or committed to source control.

## 9. Gherkin Scenarios

```gherkin
Feature: User login with JWT

  Scenario: Login succeeds with valid active user credentials
    Given an active user exists with email "user@example.com"
    And the stored password hash matches the password "Valid123"
    When the client sends a POST request to "/api/v1/auth/login"
    With email "user@example.com" and password "Valid123"
    Then the response status is 200
    And the response contains an access token
    And the token type is "Bearer"
    And the token subject contains the user's UUID
    And the token contains the user's role
    And the token has a valid expiration time

  Scenario: Login fails for an unknown email
    Given no user exists with email "unknown@example.com"
    When the client sends a login request with email "unknown@example.com"
    And password "Invalid123"
    Then the response status is 401
    And the error code is "AUTHENTICATION_FAILED"
    And the response does not reveal whether the email exists

  Scenario: Login fails for an incorrect password
    Given an active user exists with email "user@example.com"
    When the client sends a login request with email "user@example.com"
    And an incorrect password
    Then the response status is 401
    And the error code is "AUTHENTICATION_FAILED"
    And the response does not reveal whether the password or email was incorrect

  Scenario: Login fails for a blocked account
    Given a blocked user exists with email "blocked@example.com"
    When the client sends valid credentials for that user
    Then the response status is 401
    And the error code is "AUTHENTICATION_FAILED"
    And no access token is returned

  Scenario: Login fails when the email is missing
    When the client sends a login request without an email
    Then the response status is 400
    And the response contains a validation error
    And no access token is returned

  Scenario: Login fails when the password is missing
    When the client sends a login request without a password
    Then the response status is 400
    And the response contains a validation error
    And no access token is returned

  Scenario: A protected endpoint accepts a valid JWT
    Given the client has a valid unexpired JWT
    When the client requests a protected endpoint with the JWT
    Then the request is authenticated

  Scenario: A protected endpoint rejects an expired JWT
    Given the client has an expired JWT
    When the client requests a protected endpoint with the JWT
    Then the response status is 401

  Scenario: A protected endpoint rejects a tampered JWT
    Given the client has a JWT with an invalid signature
    When the client requests a protected endpoint with the JWT
    Then the response status is 401

  Scenario: A protected endpoint rejects a missing JWT
    When the client requests a protected endpoint without a token
    Then the response status is 401

  Scenario: A USER cannot access an ADMIN-only endpoint
    # Authorization-infrastructure test using a representative future ADMIN-only endpoint.
    # No such endpoint currently exists in the repository.
    Given the client has a valid JWT with role "USER"
    When the client requests a representative future ADMIN-only endpoint
    Then the response status is 403

  Scenario: An ADMIN can access an ADMIN-only endpoint
    # Authorization-infrastructure test using a representative future ADMIN-only endpoint.
    # No such endpoint currently exists in the repository.
    Given the client has a valid JWT with role "ADMIN"
    When the client requests a representative future ADMIN-only endpoint
    Then the response is accepted

  Scenario: A registered user can log in with their credentials
    Given a user has successfully registered with email "registered@example.com" and password "Valid123"
    And the user account status is "ACTIVE"
    And the password has been stored using BCrypt
    When the user sends a POST request to "/api/v1/auth/login"
    With email "registered@example.com" and password "Valid123"
    Then the response status is 200
    And the response contains a valid access token
    And the token type is "Bearer"
    And the token subject matches the registered user's UUID
    And the token contains the role "USER"
```

## 10. TDD Implementation Plan

US-02 Login shall follow the RED → GREEN → REFACTOR cycle.

Implementation must reuse the existing components delivered by US-01 Register (#47).

Tests preserved in the backup may be used as references, but they must be reviewed, adapted to the current repository, and executed before being considered valid.

### Phase 1: Login Application Unit Tests

**RED**

Write focused unit tests for `LoginUserUseCase` covering:

- Successful authentication of an active user.
- Authentication failure for an unknown email.
- Authentication failure for an incorrect password.
- Authentication failure for a blocked account.
- No access token issued when authentication fails.
- Correct interaction with `UserLookupPort`, `PasswordVerifier`, and `TokenIssuer`.

**GREEN**

- Implement `LoginUserUseCase`.
- Introduce `PasswordVerifier` and `TokenIssuer` application ports.
- Introduce `LoginResult` and `AuthenticationFailedException`.
- Reuse the existing `UserLookupPort`.

**REFACTOR**

- Remove duplicated test setup.
- Preserve generic authentication failures.
- Keep application logic independent of Spring and JWT libraries.

### Phase 2: BCrypt Password Verification Tests

**RED**

Test that:

- A valid raw password matches a BCrypt hash generated during registration.
- An incorrect password is rejected.
- Password verification does not expose stored hashes.
- Login verification remains compatible with the existing registration hashing mechanism.

**GREEN**

- Implement the BCrypt-based `PasswordVerifier` adapter.
- Reuse the existing BCrypt encoding mechanism where appropriate.
- Preserve the existing `PasswordHasher` used by registration.

**REFACTOR**

- Keep cryptographic implementation details in infrastructure.
- Avoid duplicating password encoding configuration.

### Phase 3: JWT Issuance and Validation Tests

**RED**

Test:

- JWT issuance with the user's UUID as `sub`.
- Inclusion of the `role` claim.
- Issuer set to `rastro-api`.
- Access-token lifetime of 900 seconds.
- Valid JWT signature verification.
- Rejection of expired tokens.
- Rejection of tampered tokens.
- Rejection of unsupported signing algorithms.
- Rejection of tokens with an invalid issuer.
- Rejection of missing or invalid required authentication claims.
- Application startup fails when `JWT_SECRET` is missing.
- Application startup fails when `JWT_SECRET` is not valid Base64.
- Application startup fails when the decoded signing key is shorter than 32 bytes.
- Application startup succeeds with a valid Base64-encoded signing key of at least 32 bytes, provided the remaining required configuration is valid.
- JWT validation rejects tokens with a missing or incorrect issuer.
- JWT signing and validation use the same decoded key material.

**GREEN**

- Implement `TokenIssuer` using Spring Security JWT support.
- Configure HS256 signing and validation.
- Externalize the signing secret.
- Configure the approved issuer and token lifetime.
- Do not require audience validation for the MVP.
- Validate mandatory JWT configuration at application startup.
- Decode `JWT_SECRET` from Base64 and enforce a minimum decoded key length of 32 bytes.

**REFACTOR**

- Centralize JWT configuration and claim names.
- Ensure secrets never appear in logs or API responses.
- Keep JWT infrastructure independent of domain entities.

### Phase 4: Existing Persistence Integration Tests

**RED**

Review existing US-01 persistence tests and add only missing Login-specific coverage:

- Find an existing user by normalized email.
- Return no user when the email does not exist.
- Preserve the stored BCrypt password hash.
- Correctly map UUID, role, and account status.

**GREEN**

- Reuse the existing JPA repository, mapper, and persistence adapter.
- Reuse `UserLookupPort.findByEmail(Email)`.
- Make only the minimal changes required by failing Login-specific tests.

**REFACTOR**

- Avoid duplicate persistence implementations.
- Preserve existing database mappings.
- Do not introduce unnecessary Flyway migrations.

### Phase 5: Login REST API Tests

**RED**

Test:

- Successful login returns HTTP 200.
- Response contains `accessToken`, `tokenType`, and `expiresIn`.
- Missing or invalid email returns HTTP 400.
- Missing or blank password returns HTTP 400.
- Malformed JSON returns HTTP 400.
- Invalid credentials return HTTP 401.
- Blocked accounts return HTTP 401.
- Authentication errors do not reveal account existence.
- Responses never expose passwords or password hashes.

**GREEN**

- Implement the login request and response DTOs.
- Implement `LoginController`.
- Integrate the login use case.
- Configure the required HTTP error handling.

**REFACTOR**

- Keep controllers free from authentication business logic.
- Maintain consistent API error responses.
- Reuse existing web infrastructure where appropriate.

### Phase 6: Spring Security Integration Tests

**RED**

Test:

- Login remains publicly accessible.
- Existing registration remains publicly accessible.
- Registration behavior remains unchanged.
- Protected endpoints reject missing JWTs with HTTP 401.
- Protected endpoints accept valid JWTs.
- Expired or tampered JWTs are rejected.
- A `USER` cannot access an `ADMIN`-only operation.
- An `ADMIN` can access an `ADMIN`-only operation.

Authorization tests may use test-only endpoints until real protected endpoints are introduced.

**GREEN**

- Configure a stateless `SecurityFilterChain`.
- Configure Bearer-token authentication.
- Implement role-based authorization.
- Preserve public access to the existing registration endpoint.
- Configure HTTP 401 and 403 handling.

**REFACTOR**

- Centralize security configuration.
- Review CORS and CSRF configuration.
- Ensure security changes do not introduce registration regressions.

### Phase 7: Full Verification and Regression Testing

Run focused Login tests first.

Then execute the complete backend test suite:

```bash
./mvnw clean test
```

Verify:

- All existing US-01 Register tests still pass.
- All new US-02 Login tests pass.
- Register-to-Login integration works.
- No authentication secrets are committed.
- No unrelated functionality is modified.
- The implementation matches the approved SDD specification.

Test results must be reported from actual executions.

### Implementation Rules

- Follow RED → GREEN → REFACTOR for each implementation increment.
- Do not restore the entire backup into the feature branch.
- Recover or adapt previous code only when required by an approved test.
- Keep commits focused on coherent implementation increments.
- Do not merge US-02 until the complete test suite and CI checks pass.

## 11. Prerequisites and Implementation Order

US-02 Login builds upon the existing infrastructure delivered by US-01 User Registration (#47).

The implementation must reuse existing domain, persistence, and password-hashing components rather than recreating them.

### 11.1 Prerequisite Status

| Prerequisite | Current State | Action for US-02 |
|---|---|---|
| UUID user identifiers | Implemented | Reuse `UserId` |
| Email normalization | Implemented | Reuse `Email` |
| User domain model | Implemented | Reuse `User` |
| User roles | Implemented | Reuse `UserRole` |
| Account statuses | Implemented | Reuse `AccountStatus` |
| Password validation | Implemented | Reuse existing domain rules |
| BCrypt password hashing | Implemented | Reuse existing hashing mechanism |
| User lookup by email | Implemented | Reuse `UserLookupPort` |
| PostgreSQL persistence | Implemented | Reuse existing JPA infrastructure |
| Flyway migrations | Implemented | Preserve existing schema |
| Testcontainers integration | Implemented | Reuse existing test infrastructure |
| Password verification | Pending | Implement `PasswordVerifier` |
| Login application use case | Pending | Implement `LoginUserUseCase` |
| JWT issuance and validation | Pending | Implement JWT infrastructure |
| JWT configuration | Pending | Configure issuer, secret, and TTL |
| Spring Security authentication | Pending | Configure stateless authentication |
| Role-based authorization | Pending | Configure `USER` and `ADMIN` permissions |
| Login REST endpoint | Pending | Implement `LoginController` |
| Login integration tests | Pending | Implement and execute |

### 11.2 Implementation Order

The implementation shall follow the approved TDD plan in Section 10.

Recommended order:

1. Implement and test `LoginUserUseCase` using the existing `UserLookupPort`.
2. Implement and test BCrypt password verification.
3. Implement and test JWT issuance and validation.
4. Verify integration with existing user persistence.
5. Implement and test the Login REST endpoint.
6. Configure and test Spring Security authentication and authorization.
7. Verify Register-to-Login integration.
8. Execute the complete backend test suite and CI checks.

Each implementation increment must follow RED → GREEN → REFACTOR.

### 11.3 Existing Registration Compatibility

US-01 Register is a completed feature and must remain operational.

US-02 shall:

- Preserve the existing registration endpoint.
- Preserve BCrypt compatibility between registration and login.
- Reuse existing user persistence and domain models.
- Avoid introducing duplicate repositories, entities, or migrations.
- Ensure Spring Security does not block public registration.
- Preserve existing registration tests and behavior.

### 11.4 Implementation Constraints

- Do not restore the entire backup into the feature branch.
- Do not modify unrelated functionality.
- Do not introduce new database migrations unless required by an approved change.
- Do not introduce JWT secrets into source control.
- Keep application and domain layers independent of Spring Security and JWT libraries.
- Do not begin implementation until the updated SDD specification is approved.

## 12. Approved Decisions

The following decisions are approved for US-02 Login (#48).

### 12.1 Authentication

- Authentication endpoint: `POST /api/v1/auth/login`.
- Authentication uses email and password.
- Email normalization must remain consistent with US-01 Register.
- Password verification uses BCrypt.
- Only users with `ACTIVE` account status may authenticate.
- Unknown users, incorrect passwords, and blocked accounts must produce the same generic authentication failure.

### 12.2 JWT

- Signing algorithm: HS256.
- JWT issuer (`iss`): `rastro-api`.
- JWT audience validation is not required for the MVP.
- Access token lifetime: 900 seconds (15 minutes).
- JWT subject (`sub`): user UUID.
- JWT role claim: `USER` or `ADMIN`.
- JWT must include `iat` and `exp`.
- Signing secrets must be externalized and contain at least 256 bits of key material.
- JWT validation must verify signature, algorithm, issuer, expiration, and required authentication claims.

### 12.3 Token Transport

- The access token is returned in the JSON response body.
- Clients authenticate using the `Authorization: Bearer <token>` header.
- Authentication is stateless.
- Refresh tokens are excluded from the MVP.

### 12.4 API Responses

Successful authentication returns HTTP 200:

```json
{
  "accessToken": "<signed-jwt>",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

Invalid credentials or blocked accounts return HTTP 401:

```json
{
  "code": "AUTHENTICATION_FAILED",
  "message": "Authentication failed"
}
```

Malformed or missing request fields return HTTP 400 with an `INVALID_REQUEST` error.

Protected endpoints return HTTP 401 for missing or invalid authentication and HTTP 403 for insufficient permissions.

### 12.5 Existing Registration Compatibility

- US-01 Register (#47) is already implemented and merged into `develop`.
- The existing registration endpoint must remain publicly accessible.
- Login must reuse the existing user domain model, lookup port, persistence infrastructure, and compatible BCrypt hashing mechanism.
- Existing registration behavior and tests must remain unchanged.

### 12.6 Implementation Approach

- Follow the approved SDD specification before implementation.
- Apply RED → GREEN → REFACTOR throughout development.
- Reuse existing components instead of duplicating them.
- Use backup code only as reviewed reference material.
- Implement US-02 incrementally in its dedicated feature branch.
- Execute the complete backend test suite before merging.

## 13. Pending Design Decisions

The core authentication and JWT design decisions for US-02 Login (#48) have been approved and are documented in Section 12.

No additional design decisions are required before beginning the approved MVP authentication implementation.

### 13.1 Future Endpoint Authorization

The exact permissions for future application endpoints remain to be defined as those features are implemented.

Future specifications shall determine:

- Which endpoints require authentication.
- Which operations are available to `USER`.
- Which operations are restricted to `ADMIN`.
- Whether additional authorization rules are needed for resource ownership.

These decisions do not block US-02 implementation.

For US-02, role-based authorization may be verified using test-only endpoints without introducing new production business endpoints.

### 13.2 Future Authentication Enhancements

The following capabilities are outside the current MVP scope and require separate specifications if introduced:

- Refresh tokens.
- Token revocation.
- Multi-factor authentication.
- Password reset and recovery.
- Additional JWT audiences or external token consumers.

These capabilities must not be implemented as part of US-02 unless explicitly approved through a future specification.

## 14. Pending Technical Work

The following technical work remains for US-02 Login (#48).

Implementation shall follow the approved TDD plan in Section 10 and the implementation order in Section 11.

### 14.1 Application Layer

- Implement `LoginUserUseCase`.
- Introduce the `PasswordVerifier` application port.
- Introduce the `TokenIssuer` application port.
- Implement `LoginResult`.
- Implement `AuthenticationFailedException`.
- Reuse the existing `UserLookupPort.findByEmail(Email)`.

### 14.2 Password Verification

- Implement the BCrypt password verification adapter.
- Ensure compatibility with password hashes generated by US-01 Register.
- Avoid duplicating existing password hashing infrastructure.

### 14.3 JWT Infrastructure

- Add the required Spring Security JWT dependencies.
- Implement HS256 JWT issuance and validation.
- Configure the approved issuer `rastro-api`.
- Configure the 900-second access-token lifetime.
- Externalize JWT signing configuration.
- Validate mandatory JWT configuration at application startup.
- Implement tests for valid, expired, tampered, and invalid JWTs.

### 14.4 REST API

- Implement `POST /api/v1/auth/login`.
- Create login request and response DTOs.
- Implement request validation.
- Return the approved authentication response.
- Map invalid credentials and blocked accounts to generic HTTP 401 responses.
- Map malformed requests to HTTP 400 responses.

### 14.5 Spring Security

- Configure stateless authentication.
- Configure JWT Bearer-token authentication.
- Preserve public access to Login and the existing Register endpoint.
- Protect authenticated endpoints.
- Implement role-based authorization for `USER` and `ADMIN`.
- Configure HTTP 401 and 403 error handling.
- Review CORS and CSRF configuration for the selected authentication model.

### 14.6 Testing and Verification

- Implement unit tests following RED → GREEN → REFACTOR.
- Implement JWT issuance and validation tests.
- Implement Login REST API tests.
- Implement Spring Security integration tests.
- Verify compatibility with existing persistence and BCrypt infrastructure.
- Verify Register-to-Login integration.
- Execute the complete backend test suite.
- Confirm that existing US-01 tests continue to pass.
- Verify that no secrets are committed to Git.
- Verify that CI checks pass before merging.

### 14.7 Documentation

- Update `.env.example` with required JWT configuration variable names and safe example values.
- Document authentication configuration without exposing secrets.
- Document the Login API contract.
- Keep implementation aligned with the approved SDD specification.

## 15. Out of Scope

Unless the assignment later requires them, the following are explicitly out of scope:

- Refresh tokens and rotation.
- Password reset.
- Email verification.
- Multi-factor authentication.
- Social login.
- Account recovery.
- Persistent JWT blacklist.
- Distributed token revocation.
- Complete administrative user management.
- Password change.
- Password history.
- Advanced rate limiting.
- Credential-stuffing detection.
- Full authentication event auditing.
- External KMS integration.
- SSO or external OAuth2 provider integration.

## 16. Proposal Summary

US-02 Login (#48) introduces stateless JWT authentication into the existing Rastro backend.

The implementation builds upon the completed US-01 Register (#47) functionality and follows the project's hexagonal architecture, Domain-Driven Design principles, and Test-Driven Development workflow.

### 16.1 Existing Components to Reuse

The following components are already available and must be reused:

- User domain model and value objects.
- UUID-based user identification.
- Email normalization and validation.
- User roles and account statuses.
- `UserLookupPort.findByEmail(Email)`.
- Existing JPA persistence infrastructure.
- PostgreSQL and Flyway migrations.
- BCrypt password hashing infrastructure.
- Existing unit and integration testing infrastructure.

### 16.2 New Components

US-02 shall introduce:

- `LoginUserUseCase`.
- `PasswordVerifier` and `TokenIssuer` application ports.
- `LoginResult` and `AuthenticationFailedException`.
- BCrypt password verification adapter.
- JWT issuance and validation infrastructure.
- Login REST controller and DTOs.
- Stateless Spring Security configuration.
- Authentication and authorization error handling.
- Unit, integration, and security tests.

### 16.3 Approved Authentication Contract

- Endpoint: `POST /api/v1/auth/login`.
- Credentials: email and password.
- Only `ACTIVE` users may authenticate.
- Password verification uses BCrypt.
- Successful authentication returns a signed JWT.
- Invalid credentials and blocked accounts produce a generic HTTP 401 response.
- Authentication uses the Bearer-token transport mechanism.

### 16.4 Approved JWT Configuration

- Algorithm: HS256.
- Issuer: `rastro-api`.
- Access-token lifetime: 900 seconds.
- Subject: user UUID.
- Role claim: `USER` or `ADMIN`.
- Required timestamps: `iat` and `exp`.
- Signing secret: externally configured, with at least 256 bits of key material.
- Audience validation: not required for the MVP.

### 16.5 Implementation and Verification

Implementation shall begin only after approval of this specification.

Development shall follow RED → GREEN → REFACTOR, with focused tests for each increment.

The implementation must preserve existing US-01 Register functionality and avoid unnecessary changes to domain models, persistence, and database migrations.

Before merging US-02, the complete backend test suite and CI checks must pass.

The completed implementation must conform to the approved functional requirements, security requirements, REST API contract, and acceptance criteria documented in this specification.