-- User event log: records user actions (UEL-001 to UEL-006)
CREATE TABLE user_events
(
    id        BIGSERIAL NOT NULL,
    user_id   INTEGER REFERENCES mtp_users (id),
    path      TEXT,
    data      TEXT,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_user_events PRIMARY KEY (id)
);

CREATE INDEX idx_user_events_timestamp_id ON user_events (timestamp DESC, id DESC);
