-- Password reset tokens (AUTH-040 to AUTH-049)
CREATE TABLE password_reset_tokens
(
    id          SERIAL      NOT NULL,
    user_id     INTEGER     NOT NULL REFERENCES mtp_users (id) ON DELETE CASCADE,
    token       UUID        NOT NULL,
    application VARCHAR(50) NOT NULL DEFAULT '',
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    used        BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT uq_password_reset_tokens_token UNIQUE (token)
);
