-- MTP Roles
CREATE TABLE mtp_auth_role
(
    id           SERIAL       NOT NULL,
    name         VARCHAR(150) NOT NULL,
    key_group    VARCHAR(150) NOT NULL DEFAULT '',
    other_groups TEXT         NOT NULL DEFAULT '',
    application  VARCHAR(50)  NOT NULL DEFAULT '',

    CONSTRAINT pk_mtp_roles PRIMARY KEY (id),
    CONSTRAINT uq_mtp_roles_name UNIQUE (name)
);

-- MTP Users
CREATE TABLE mtp_users
(
    id         SERIAL       NOT NULL,
    username   VARCHAR(150) NOT NULL,
    email      VARCHAR(254) NOT NULL DEFAULT '',
    first_name VARCHAR(150) NOT NULL DEFAULT '',
    last_name  VARCHAR(150) NOT NULL DEFAULT '',
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    role_id    INTEGER REFERENCES mtp_auth_role (id),
    created    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified   TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_mtp_users PRIMARY KEY (id),
    CONSTRAINT uq_mtp_users_username UNIQUE (username)
);

-- User-Prison mapping (many-to-many)
CREATE TABLE mtp_auth_prisonusermapping_prisons
(
    user_id        INTEGER     NOT NULL REFERENCES mtp_users (id) ON DELETE CASCADE,
    prison_nomis_id VARCHAR(10) NOT NULL REFERENCES prison_prison (nomis_id) ON DELETE CASCADE,

    CONSTRAINT pk_mtp_user_prisons PRIMARY KEY (user_id, prison_nomis_id)
);

-- Failed login attempts
CREATE TABLE mtp_auth_failedloginattempt
(
    id           SERIAL      NOT NULL,
    user_id      INTEGER     NOT NULL REFERENCES mtp_users (id) ON DELETE CASCADE,
    application  VARCHAR(50) NOT NULL DEFAULT '',
    attempted_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_failed_login_attempts PRIMARY KEY (id)
);

CREATE INDEX idx_failed_login_attempts_user_app ON mtp_auth_failedloginattempt (user_id, application, attempted_at DESC);

-- Login records
CREATE TABLE mtp_auth_login
(
    id          SERIAL      NOT NULL,
    user_id     INTEGER     NOT NULL REFERENCES mtp_users (id) ON DELETE CASCADE,
    application VARCHAR(50) NOT NULL DEFAULT '',
    logged_in_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_mtp_logins PRIMARY KEY (id)
);
