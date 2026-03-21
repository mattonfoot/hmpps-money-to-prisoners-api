-- Logs table for credit audit trail
CREATE TABLE credit_log
(
    log_id    SERIAL                      NOT NULL,
    action    VARCHAR(50)                 NOT NULL,
    credit_id INTEGER,
    user_id   VARCHAR(255),
    created   TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_logs PRIMARY KEY (log_id),
    CONSTRAINT fk_logs_credit FOREIGN KEY (credit_id) REFERENCES credit_credit (credit_id)
);

CREATE INDEX idx_logs_credit_id ON credit_log (credit_id);
CREATE INDEX idx_logs_action ON credit_log (action);

-- Security checks table
CREATE TABLE security_check
(
    check_id        SERIAL                      NOT NULL,
    status          VARCHAR(50)                 NOT NULL DEFAULT 'PENDING',
    description     TEXT,
    decision_reason TEXT,
    actioned_by     VARCHAR(255),
    actioned_at     TIMESTAMP WITHOUT TIME ZONE,
    credit_id       INTEGER                     NOT NULL,
    created         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified        TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_security_checks PRIMARY KEY (check_id),
    CONSTRAINT fk_security_checks_credit FOREIGN KEY (credit_id) REFERENCES credit_credit (credit_id),
    CONSTRAINT uq_security_checks_credit UNIQUE (credit_id)
);

CREATE INDEX idx_security_checks_credit_id ON security_check (credit_id);
CREATE INDEX idx_security_checks_status ON security_check (status);

-- Sender profiles table
CREATE TABLE security_senderprofile
(
    sender_profile_id SERIAL                      NOT NULL,
    created           TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified          TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_sender_profiles PRIMARY KEY (sender_profile_id)
);

-- Sender profile credit_credit join table
CREATE TABLE sender_profile_credits
(
    sender_profile_id INTEGER NOT NULL,
    credit_id         INTEGER NOT NULL,

    CONSTRAINT pk_sender_profile_credits PRIMARY KEY (sender_profile_id, credit_id),
    CONSTRAINT fk_spc_sender_profile FOREIGN KEY (sender_profile_id) REFERENCES security_senderprofile (sender_profile_id),
    CONSTRAINT fk_spc_credit FOREIGN KEY (credit_id) REFERENCES credit_credit (credit_id)
);

-- Sender profile monitoring users
CREATE TABLE sender_profile_monitoring_users
(
    sender_profile_id INTEGER      NOT NULL,
    user_id           VARCHAR(255) NOT NULL,

    CONSTRAINT pk_sender_profile_monitoring_users PRIMARY KEY (sender_profile_id, user_id),
    CONSTRAINT fk_spmu_sender_profile FOREIGN KEY (sender_profile_id) REFERENCES security_senderprofile (sender_profile_id)
);

-- Prisoner profiles table
CREATE TABLE security_prisonerprofile
(
    prisoner_profile_id SERIAL                      NOT NULL,
    prisoner_number     VARCHAR(250),
    prisoner_name       VARCHAR(250),
    created             TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified            TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_prisoner_profiles PRIMARY KEY (prisoner_profile_id)
);

-- Prisoner profile credit_credit join table
CREATE TABLE prisoner_profile_credits
(
    prisoner_profile_id INTEGER NOT NULL,
    credit_id           INTEGER NOT NULL,

    CONSTRAINT pk_prisoner_profile_credits PRIMARY KEY (prisoner_profile_id, credit_id),
    CONSTRAINT fk_ppc_prisoner_profile FOREIGN KEY (prisoner_profile_id) REFERENCES security_prisonerprofile (prisoner_profile_id),
    CONSTRAINT fk_ppc_credit FOREIGN KEY (credit_id) REFERENCES credit_credit (credit_id)
);

-- Prisoner profile monitoring users
CREATE TABLE prisoner_profile_monitoring_users
(
    prisoner_profile_id INTEGER      NOT NULL,
    user_id             VARCHAR(255) NOT NULL,

    CONSTRAINT pk_prisoner_profile_monitoring_users PRIMARY KEY (prisoner_profile_id, user_id),
    CONSTRAINT fk_ppmu_prisoner_profile FOREIGN KEY (prisoner_profile_id) REFERENCES security_prisonerprofile (prisoner_profile_id)
);
