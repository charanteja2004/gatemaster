# GateMaster sync API

The backend behind accounts and cross-device sync. Kotlin, Ktor, PostgreSQL.

The app works fully without it. Signing in is opt-in and adds one thing:
what you have read and every paper you have sat follow you to another phone.
Nothing about studying offline changes when you are signed out, which is the
constraint the whole design is built around.

```
POST   /v1/auth/register     -> 201 session
POST   /v1/auth/login        -> 200 session
POST   /v1/auth/refresh      -> 200 session      (rotates the refresh token)
POST   /v1/auth/logout       -> 204
GET    /v1/me                -> 200 user         (bearer)
GET    /v1/sync/progress     -> 200 document + revision
PUT    /v1/sync/progress     -> 200 | 409 conflict, with the current document
POST   /v1/sync/attempts     -> 200 accepted + duplicates
GET    /v1/sync/attempts     -> 200 page + cursor
GET    /health               -> 200
```

## Run it

Tests need nothing installed:

```sh
./gradlew :server:test
```

Neither does the server itself, if you point it at H2. That is for development
only -- it boots with a warning saying so -- but it means a working API is one
command away with no Docker and no database:

```sh
export GATEMASTER_DATABASE_URL='jdbc:h2:file:./build/devdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE'
export GATEMASTER_DATABASE_USER=sa
export GATEMASTER_JWT_SECRET=local-development-secret-not-for-any-real-deployment
export GATEMASTER_BCRYPT_COST=10
./gradlew :server:run
```

Against the database it actually deploys on:

```sh
docker compose -f server/docker-compose.yml up --build
curl localhost:8080/health
```

### Pointing the app at it

The debug build permits cleartext to localhost (`app/src/debug`, so the release
APK never does). Forward the port down the USB cable and the phone's localhost
becomes yours -- no LAN address, no router, no self-signed certificate:

```sh
adb reverse tcp:8080 tcp:8080
```

Then set `http://localhost:8080` in the app under Settings -> Account.

## Configuration

Read once at startup, so a missing variable fails the boot rather than the
first request that needed it.

| Variable | Required | Default | |
|---|---|---|---|
| `GATEMASTER_DATABASE_URL` | yes | | JDBC URL |
| `GATEMASTER_DATABASE_USER` | | | |
| `GATEMASTER_DATABASE_PASSWORD` | | | |
| `GATEMASTER_JWT_SECRET` | yes | | 32+ chars; `openssl rand -base64 48` |
| `GATEMASTER_JWT_ISSUER` | | `gatemaster` | |
| `GATEMASTER_JWT_AUDIENCE` | | `gatemaster-app` | |
| `GATEMASTER_ACCESS_TOKEN_MINUTES` | | 15 | |
| `GATEMASTER_REFRESH_TOKEN_DAYS` | | 60 | |
| `GATEMASTER_BCRYPT_COST` | | 12 | 10 is the floor |
| `GATEMASTER_ALLOWED_ORIGINS` | | none | CORS; the app needs none |
| `PORT` | | 8080 | |

## The two things worth reading the code for

### Refresh-token rotation, with reuse detection

Access tokens are JWTs and cannot be revoked, so they live fifteen minutes.
Refresh tokens are opaque random strings, can be revoked, and live two months.
Only the SHA-256 of a refresh token is stored, so the table is useless to
anyone who reads it.

Every refresh **rotates**: the presented token is revoked and a new one issued.
That makes each token single-use, which turns a stolen token into something
detectable — if one is presented twice, two parties hold it.

There is no way to tell from the server which of them is the legitimate user,
so neither keeps the session: every live token descended from that sign-in is
revoked (`family_id` is what ties them together). The real user signs in again
with a password the thief does not have. The thief gets nothing.

Sign-ins are separate families, so this never signs out the other device.

### Sync: two shapes, two mechanisms

The two things worth syncing are opposite kinds of data, and using one
mechanism for both would break one of them.

**Study progress is mutable shared state.** What you have read changes on
whichever device you are reading on, so two devices genuinely can disagree.
Last-write-wins would mean a phone that synced an hour late silently erases a
week of reading on the tablet. So it uses **optimistic concurrency**: the row
carries a `revision`, a write states which revision it was based on, and a
write from a stale revision is rejected with `409` — carrying the server's
current document, because the client needs it to merge and has just proved it
does not have it.

**Attempts are immutable historical facts.** A finished paper never changes, so
there is nothing to disagree about and no conflict resolution to write. Upload
is **append-only and idempotent**, keyed on a client-generated id: the retry
after a dropped response inserts nothing and reports the id as a duplicate,
so a flaky connection cannot double-count an attempt and skew every average
computed from it. Download is a cursor over a server-assigned sequence, which
stays correct under concurrent inserts in a way a timestamp does not.

## Tests

34 tests, `./gradlew :server:test`. No Docker, no database, no fixtures to
start.

They are integration tests against a real (in-memory) database rather than unit
tests with the SQL mocked out, because everything interesting here — rotation,
optimistic concurrency, idempotent upload — lives in how the routes, the
service and the SQL fit together. A test with the database faked would assert
the parts and prove nothing about the joins.

- **`AuthApiTest`** — registration, the case-insensitive email rule, and the
  two properties that are easy to get wrong: a wrong password and an unknown
  account return byte-identical responses, and a 150-character password is not
  silently truncated to BCrypt's 72-byte limit.
- **`RefreshTokenTest`** — rotation, reuse detection revoking the family, one
  compromised family not touching another device's, expiry on both token types,
  and logout scoped to the device that asked.
- **`SyncApiTest`** — the concurrency rules above, in the interleaving that
  motivates them: two devices, both writing from revision 1.
- **`MigrationTest`** — the schema, which is the one thing here that cannot be
  rolled back in production.

### What H2 costs

The suite runs on H2 in PostgreSQL compatibility mode. That buys a suite with
no Docker requirement, matching the Android side's rule that everything runs on
the JVM. What it costs is that H2 is not Postgres: `jsonb`, `citext`,
`ON CONFLICT` and advisory locks would pass here and fail in production.

So the schema and every query deliberately stay inside the portable subset, and
the notes in `V1__initial_schema.sql` say which construct was avoided and what
replaced it. `docker-compose.yml` runs the same code against real Postgres for
anything the subset cannot cover. The day this server needs a Postgres-only
feature is the day `TestDatabase` is replaced by Testcontainers.

## Deploying

The image is self-contained and reads everything from the environment, so any
platform that runs a container works. Render, Fly.io, Railway and Koyeb all
have a free tier that fits.

```sh
docker build -f server/Dockerfile -t gatemaster-api .
```

Migrations run on boot, so a deploy is one step. Point
`GATEMASTER_DATABASE_URL` at a managed Postgres, set `GATEMASTER_JWT_SECRET` to
something from `openssl rand -base64 48`, and the health check at `/health`.
