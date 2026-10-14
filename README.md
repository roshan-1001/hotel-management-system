# Hotel Management System

A Spring Boot REST API for hotel room bookings, focused on transactional consistency, concurrent booking prevention, idempotent requests, and JWT-based authorization.

## Features

- **Booking lifecycle management:** Pending payment, confirmed, checked in, checked out, cancelled, and expired states.
- **Concurrent booking protection:** Database locking and availability checks prevent overlapping bookings for the same room and dates.
- **Idempotent booking creation:** Repeated requests with the same idempotency key avoid duplicate bookings; reusing a key with a different request payload is rejected.
- **Booking expiration:** Pending bookings expire after a configured five-minute window.
- **JWT authentication and authorization:** Stateless authentication with role-based access control for administrators and receptionists.
- **Database migrations:** Flyway manages schema changes, while Hibernate validates the schema.
- **Containerized deployment:** Docker Compose runs the application and PostgreSQL.
- **Automated tests:** Unit, repository, and concurrency integration tests.

## Tech Stack

- Java 21
- Spring Boot
- Spring Security and JWT
- Spring Data JPA and Hibernate
- PostgreSQL
- Flyway
- Maven
- Docker and Docker Compose
- JUnit

## Architecture

The application separates HTTP handling, business logic, persistence, domain models, and API DTOs.

- **Controllers:** Expose REST endpoints.
- **Services:** Enforce booking rules and transactional business operations.
- **Repositories:** Handle database access through Spring Data JPA.
- **Database:** PostgreSQL stores application data, with Flyway migrations managing the schema.
- **Security:** Spring Security validates JWTs and enforces endpoint and method-level authorization.

## Running Locally

### Prerequisites

- Java 21
- Maven
- Docker with Docker Compose

### Configure environment variables

Create a local `.env` file containing the variables required by `compose.yaml`:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `JWT_SECRET`

Use a strong local database password and a Base64-encoded secret containing at least 32 random bytes for `JWT_SECRET`. Never commit `.env` or real secrets to version control.

### Start the application

From the project root:

```bash
docker compose up --build
```

The application should be available at `http://localhost:8080` once the containers are healthy.

To stop the services:

```bash
docker compose down
```

To remove the local database volume as well, use `docker compose down -v`. **This deletes the persisted database data.**

## API Examples

### Authenticate

`POST /auth/login`

Example request:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

The response includes an access token. Use it as a Bearer token when calling protected endpoints.

The credentials above are development/demo credentials only. They must not be used in a production deployment.

### Access bookings

`GET /bookings`

Send the token in the request header:

```text
Authorization: Bearer <access-token>
```

### Check application health

`GET /actuator/health`

This endpoint provides an application health response.

## Testing

Run the complete Maven test suite:

```bash
mvn clean test
```

The test suite covers service behavior, repository operations, and concurrent booking attempts. The current verified result is **23 tests passing, with zero failures or errors**.

## Engineering Decisions

- Use database transactions to keep booking operations consistent.
- Use database locking to coordinate concurrent requests for the same room.
- Use idempotency keys to protect against duplicate request retries.
- Use Flyway migrations instead of relying on Hibernate to create or modify the production schema.
- Use JWT authentication and role-based authorization for protected operations.

## Scope

This project is an interview-oriented backend implementation. Production deployment would require additional work around credential management, payment-provider integration, monitoring, and operational security.
