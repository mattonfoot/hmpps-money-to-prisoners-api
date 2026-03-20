-- User flags (AUTH-030 to AUTH-033)
CREATE TABLE user_flags
(
    id        SERIAL       NOT NULL,
    user_id   INTEGER      NOT NULL REFERENCES mtp_users (id) ON DELETE CASCADE,
    flag_name VARCHAR(50)  NOT NULL,

    CONSTRAINT pk_user_flags PRIMARY KEY (id),
    CONSTRAINT uq_user_flags_user_flag UNIQUE (user_id, flag_name)
);
