-- Initial schema: accounts, refresh tokens, and the two synced shapes.
--
-- Written in the portable subset of SQL that both PostgreSQL (production) and
-- H2 in PostgreSQL compatibility mode (the test suite) accept, so the schema
-- the tests exercise is the schema that ships. That rules out citext, jsonb and
-- ON CONFLICT; the notes below say what is used instead.

-- Accounts -------------------------------------------------------------------
--
-- email is stored already lower-cased by the application rather than wrapped in
-- citext, which H2 does not have. The unique constraint therefore provides the
-- case-insensitivity, and every lookup must lower-case its argument.
CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(320) NOT NULL UNIQUE,
    password_hash   VARCHAR(120) NOT NULL,
    display_name    VARCHAR(80)  NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Refresh tokens -------------------------------------------------------------
--
-- Only the SHA-256 of each token is stored, so a dump of this table does not
-- let the reader sign in as anybody.
--
-- family_id ties every token descended from one sign-in together. Refreshing
-- rotates the token and revokes its predecessor, so a token presented twice
-- means its holder was not the only one who had it -- at which point the whole
-- family is revoked rather than just that one token.
CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    family_id       UUID NOT NULL,
    token_hash      CHAR(64) NOT NULL UNIQUE,
    issued_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);

-- Study progress -------------------------------------------------------------
--
-- One small JSON document per user: what has been read, how far, what is
-- bookmarked. The server never looks inside it. It is the app's own format
-- stored whole, which is why this is TEXT rather than jsonb.
--
-- revision is the optimistic-concurrency token. A client PUTs the revision its
-- edit was based on; if the row has moved on since, the write is rejected with
-- 409 and the client merges and retries. Without it, the last device to sync
-- silently erases what was read on the other one.
CREATE TABLE study_progress (
    user_id         UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    document        TEXT NOT NULL,
    revision        BIGINT NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Attempts -------------------------------------------------------------------
--
-- Append-only, which is what makes attempt sync trivial: a finished attempt is
-- an immutable historical fact, so two devices can never disagree about one.
-- There is no conflict resolution here because the shape of the data removes
-- the possibility of a conflict.
--
-- client_attempt_id is generated on the device and makes upload idempotent: a
-- retry after a dropped response inserts nothing the second time, so a flaky
-- connection cannot double-count an attempt.
--
-- server_seq is the download cursor. Clients ask for everything above the
-- highest sequence they already hold, which stays correct under concurrent
-- inserts in a way that a wall-clock timestamp does not.
CREATE TABLE attempts (
    server_seq          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    client_attempt_id   VARCHAR(64) NOT NULL,
    test_id             VARCHAR(200) NOT NULL,
    test_title          VARCHAR(200) NOT NULL,
    started_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_seconds    INTEGER NOT NULL,
    score               DOUBLE PRECISION NOT NULL,
    max_score           DOUBLE PRECISION NOT NULL,
    uploaded_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_attempts_client_id UNIQUE (user_id, client_attempt_id)
);

CREATE INDEX idx_attempts_user_seq ON attempts (user_id, server_seq);

-- One row per question of one attempt. This is the grain the app's Progress tab
-- aggregates over: per-topic accuracy is a GROUP BY on this table, which is the
-- whole reason attempts are not stored as one blob.
CREATE TABLE attempt_questions (
    id                  UUID PRIMARY KEY,
    attempt_seq         BIGINT NOT NULL REFERENCES attempts (server_seq) ON DELETE CASCADE,
    question_id         VARCHAR(120) NOT NULL,
    subject_id          VARCHAR(60) NOT NULL,
    topic_id            VARCHAR(120),
    -- Nullable, and it usually is. The app's own attempt rows record whether a
    -- question was right, not whether it was an MCQ, so a client that has been
    -- storing history since before sync existed has nothing to put here. The
    -- column stays for the clients that will.
    question_type       VARCHAR(16),
    marks               DOUBLE PRECISION NOT NULL,
    awarded             DOUBLE PRECISION NOT NULL,
    was_attempted       BOOLEAN NOT NULL,
    was_correct         BOOLEAN NOT NULL
);

CREATE INDEX idx_attempt_questions_attempt ON attempt_questions (attempt_seq);
CREATE INDEX idx_attempt_questions_topic ON attempt_questions (subject_id, topic_id);
