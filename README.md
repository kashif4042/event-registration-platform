# Event Registration Platform

A backend REST API for running events end to end: organisers create and publish events, attendees register (solo or as a team), seats are handed out safely under concurrent load, full events fill a waitlist that promotes automatically on cancellation, confirmed registrations get QR-ready tickets that are checked in at the door, and everyone is kept informed by email.

The interesting part of this project is not the CRUD, it is the edge cases: two people fighting for the last seat, a cancelled team seat being re-offered to a waitlisted group, a ticket scanned twice, an email provider going down mid-request.

## Highlights

| Problem | How it is solved |
|---|---|
| Overselling the last seat under concurrent requests | Registration and cancellation run in one `@Transactional` unit and lock the event row (`PESSIMISTIC_WRITE`, i.e. `SELECT ... FOR UPDATE`) before counting seats, so competing requests queue instead of racing |
| Waitlist promotion | On cancelling a confirmed registration, the oldest waitlisted groups are promoted **in the same transaction**, so one freed seat is never given out twice |
| Team bookings and partial fits | A team is promoted all-or-nothing. A group too large for the freed seats is skipped and a smaller party behind it can still be promoted |
| Duplicate registration | One active (confirmed or waitlisted) registration per user per event |
| Ticket check-in | Per-registration UUID token. Check-in is idempotent: a second scan returns `200` with the ticket still `USED`, not an error. Only the event's organiser can check people in, and not for a cancelled event |
| Auth | JWT access token (15 min) + refresh token (7 days) with rotation: a refresh token works once, and a reused old one is rejected |
| Email verification / password reset | Single-use tokens stored in Redis with a 15 minute TTL, so expiry needs no cleanup job. Password reset responds identically whether or not the email exists |
| Slow or failing email | Sent with `@Async` so requests are not blocked. Failures are saved to an `email_retry_queue` table and retried by a scheduled job (gives up after 3 attempts) |
| Event reminders | Hourly scheduled job emails confirmed attendees of events starting within 24 hours. A `reminderSent` flag prevents repeats |

## Tech stack

- Java 21, Spring Boot 4
- Spring Security with JWT (jjwt 0.11.5)
- Spring Data JPA / Hibernate, MySQL 8
- Spring Data Redis (Docker)
- Spring Mail (Gmail SMTP)
- springdoc-openapi (Swagger UI)
- JUnit 5 + Mockito

## Project structure

Package-by-feature, one package per module:

```
com.kashif.event_registration_platform
├── auth          controller, service, repository, entity (User, Role), dto
├── event         controller, service, repository, entity (Event, EventStatus), dto
├── registration  controller, service, repository, entity (Registration, TeamRegistration), dto
├── ticket        controller, service, repository, entity (Ticket, TicketStatus), dto
├── notification  service, repository, entity (EmailRetryQueue)
├── admin         controller, service, dto
└── common        exception handling, security (JWT filter/util, user details), config
```

## State machines

```
Event         DRAFT <-> PUBLISHED, DRAFT/PUBLISHED -> CANCELLED
Registration  CONFIRMED | WAITLISTED -> CANCELLED, WAITLISTED -> CONFIRMED (promotion)
Ticket        CONFIRMED -> USED | CANCELLED   (USED and CANCELLED are final)
Email         PENDING -> SENT | FAILED
```

Invalid transitions return `400` with a clear message.

## Roles

| Role | Can do |
|---|---|
| `ATTENDEE` (default on signup) | Browse events, register, cancel own registration |
| `ORGANISER` | Create, publish, unpublish and cancel own events, check in tickets, view own stats and revenue |
| `ADMIN` | Cancel any event |

Registration always creates an `ATTENDEE`; a role is never accepted from the client. Promote accounts directly in the database:

```sql
UPDATE users SET role = 'ORGANISER' WHERE id = <user id>;
```

## API overview

Interactive docs: `http://localhost:8080/swagger-ui/index.html` (use **Authorize** and paste an access token, without the `Bearer ` prefix).

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| POST | `/api/auth/refresh` | Public (refresh token in body) |
| GET | `/api/auth/verify?token=` | Public |
| POST | `/api/auth/forgot-password` | Public |
| POST | `/api/auth/reset-password` | Public |
| GET | `/api/events` | Public, published events only |
| POST | `/api/events` | Organiser |
| PATCH | `/api/events/{id}/publish` | Organiser (own event) |
| PATCH | `/api/events/{id}/unpublish` | Organiser (own event) |
| PATCH | `/api/events/{id}/cancel` | Organiser (own event), notifies registrants |
| POST | `/api/events/{id}/register` | Authenticated. Body `{ "teamSize": 3 }` is optional |
| DELETE | `/api/registrations/{id}` | Owner of the registration, may promote from the waitlist |
| POST | `/api/tickets/{token}/checkin` | The event's organiser, idempotent |
| GET | `/api/admin/events/{id}/stats` | The event's organiser |
| GET | `/api/admin/revenue-summary` | Organiser, scoped to their own events |
| PATCH | `/api/admin/events/{id}/cancel` | Admin override |

## Running locally

### Prerequisites

Java 21, Maven, MySQL 8, Docker, and a Gmail account with an [App Password](https://support.google.com/accounts/answer/185833).

### 1. Database and Redis

```sql
CREATE DATABASE event_reg_db;
```

```bash
docker run -d --name redis -p 6379:6379 redis
```

### 2. Environment variables

Secrets are read from the environment, never committed:

| Variable | Purpose |
|---|---|
| `DB_USERNAME` | MySQL user (defaults to `root`) |
| `DB_PASSWORD` | MySQL password |
| `JWT_SECRET` | Long random string used to sign tokens |
| `MAIL_USERNAME` | Gmail address that sends emails |
| `MAIL_APP_PASSWORD` | Gmail App Password |

### 3. Run

```bash
mvn spring-boot:run
```

Tables are created automatically (`spring.jpa.hibernate.ddl-auto=update`).

### A typical flow

1. Register two users, then promote one to `ORGANISER` with the SQL above
2. Log in as the organiser, create an event, publish it
3. Log in as the attendee, `GET /api/events`, then `POST /api/events/{id}/register`
4. Check the attendee's inbox for the confirmation email, and the `tickets` table for the ticket
5. As the organiser, `POST /api/tickets/{token}/checkin` (scan it twice to see idempotency)

## Tests

```bash
mvn test
```

Unit tests (JUnit 5 + Mockito) cover every service layer and need no database, Redis or mail server. They include the hard cases: team promotion, skipping a group that does not fit, idempotent check-in, retry-queue give-up, and the divide-by-zero guard in the check-in rate.

## Known limitations and future work

- **No rate limiting** on auth endpoints. Repeated `forgot-password` calls could flood an inbox.
- **Email verification is not enforced at login** (`isEnabled()` always returns true). It is a one-line change to require it.
- **Event `ONGOING` and `COMPLETED` statuses exist but nothing moves an event into them yet.**
- **One refresh token per user**, so logging in on a second device invalidates the first session.
- Check-in does not distinguish a first scan from a repeat scan in its response, which would help flag shared tickets.
- Only unit tests so far. Concurrency behaviour is reasoned about and manually verified, not yet covered by integration tests (for example Testcontainers).
- Team members do not need accounts: a team booking is N tickets held by the lead user, by design.
