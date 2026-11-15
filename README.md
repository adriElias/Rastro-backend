# Rastro Backend

Spring Boot backend for RASTRO, targeting Java 21. Local development uses PostgreSQL and Adminer through Docker Compose.

## Local development stack

- Java 21
- Spring Boot 4.1.1
- PostgreSQL 16
- Adminer 4
- Docker Compose
- Flyway-managed database migrations
- JPA schema validation

## Prerequisites

- Java 21
- Docker with Docker Compose

## Environment configuration

Create the ignored local environment file from the committed template:

```bash
cp .env.example .env
```

Replace the password and JWT secret placeholders before starting the application. Never commit `.env` or use the example JWT secret outside local development.

Required variables:

| Variable | Purpose |
| --- | --- |
| `COMPOSE_PROJECT_NAME` | Docker Compose project name. |
| `POSTGRES_DB` | PostgreSQL database name. |
| `POSTGRES_USER` | PostgreSQL user. |
| `POSTGRES_PASSWORD` | PostgreSQL password. |
| `POSTGRES_PORT` | Host port mapped to PostgreSQL. |
| `ADMINER_PORT` | Host port mapped to Adminer. |
| `JWT_SECRET` | Base64-encoded cryptographically random secret; the decoded key must contain at least 32 bytes. || `JWT_ISSUER` | Non-blank issuer used to create and validate JWTs. |
| `JWT_ACCESS_TOKEN_TTL_SECONDS` | Positive access-token lifetime in seconds; the template uses `900`. |

The values in `.env.example` are local examples, not application defaults. Docker Compose reads `.env` automatically, but Spring Boot does not; export the variables before starting the backend.

The repository `.gitignore` excludes `.env` and local environment variants to prevent accidental secret commits.

## Docker services

`docker-compose.yml` defines:

- `postgres`: `postgres:16-alpine`, exposed through `POSTGRES_PORT`, with data stored in the named volume `rastro_postgres_data`.
- `adminer`: `adminer:4`, exposed through `ADMINER_PORT`, with `postgres` as its default database server.
- Private bridge network: `rastro_internal_net`.

## Start PostgreSQL and Adminer

From `Rastro-backend`:

```bash
docker compose up -d
docker compose ps
```

`docker compose ps` displays the current container status so you can confirm that PostgreSQL and Adminer are running.

## Run the backend

Export the local environment variables and start Spring Boot with the Maven wrapper:

```bash
set -a && source .env && set +a
bash ./mvnw spring-boot:run
```

The backend starts at `http://localhost:8081`.

## Flyway

Flyway is enabled and applies migrations from:

```text
src/main/resources/db/migration
```

JPA uses `ddl-auto=validate`, so the database schema must match the Flyway-managed schema. Pending migrations are applied when the application starts.

## Safe shutdown

Stop the containers without removing containers, networks, or persisted data:

```bash
docker compose stop
```

Remove the Compose containers and networks while preserving named volumes:

```bash
docker compose down
```

**Warning:** the following command also deletes the persisted local database volume. Use it only when an intentional data reset has been explicitly approved:

```bash
docker compose down -v
```

## How to access Adminer

Open:

`http://localhost:8080`

Use:
- **System**: PostgreSQL
- **Server**: `postgres`
- **Username**: value from `POSTGRES_USER` in `.env`
- **Password**: value from `POSTGRES_PASSWORD` in `.env`
- **Database**: value from `POSTGRES_DB` in `.env`

## Connect to PostgreSQL from IntelliJ IDEA

1. Open **View > Tool Windows > Database**.
2. Click **+ > Data Source > PostgreSQL**.
3. Configure:
   - Host: `localhost`
   - Port: value from `POSTGRES_PORT` in `.env`
   - Database: value from `POSTGRES_DB` in `.env`
   - User: value from `POSTGRES_USER` in `.env`
   - Password: from `.env`
4. Download driver if prompted.
5. Click **Test Connection**, then **OK**.

## Connect to PostgreSQL from DBeaver

1. Click **Database > New Database Connection**.
2. Select **PostgreSQL**.
3. Configure:
   - Host: `localhost`
   - Port: value from `POSTGRES_PORT` in `.env`
   - Database: value from `POSTGRES_DB` in `.env`
   - Username: value from `POSTGRES_USER` in `.env`
   - Password: from `.env`
4. Click **Test Connection**.
5. Click **Finish**.
