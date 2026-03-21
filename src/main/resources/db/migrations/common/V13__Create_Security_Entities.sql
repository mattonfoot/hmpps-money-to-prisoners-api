-- Add missing columns to security_check
ALTER TABLE security_check
    ADD COLUMN IF NOT EXISTS rule_codes    TEXT,
    ADD COLUMN IF NOT EXISTS descriptions  TEXT,
    ADD COLUMN IF NOT EXISTS rejection_reasons TEXT,
    ADD COLUMN IF NOT EXISTS started_at    TIMESTAMP WITHOUT TIME ZONE;

-- Auto-accept rules
CREATE TABLE security_checkautoacceptrule
(
    id                  SERIAL NOT NULL,
    sender_profile_id   BIGINT NOT NULL,
    prisoner_profile_id BIGINT NOT NULL,
    created             TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified            TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_auto_accept_rules PRIMARY KEY (id),
    CONSTRAINT fk_aar_sender_profile FOREIGN KEY (sender_profile_id) REFERENCES security_senderprofile (sender_profile_id) ON DELETE CASCADE,
    CONSTRAINT fk_aar_prisoner_profile FOREIGN KEY (prisoner_profile_id) REFERENCES security_prisonerprofile (prisoner_profile_id) ON DELETE CASCADE,
    CONSTRAINT uq_auto_accept_rules_pair UNIQUE (sender_profile_id, prisoner_profile_id)
);

CREATE TABLE security_checkautoacceptrulestate
(
    id             SERIAL  NOT NULL,
    rule_id        BIGINT  NOT NULL,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    reason         TEXT,
    created_by     VARCHAR(250),
    created        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified       TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_auto_accept_rule_states PRIMARY KEY (id),
    CONSTRAINT fk_aars_rule FOREIGN KEY (rule_id) REFERENCES security_checkautoacceptrule (id) ON DELETE CASCADE
);

-- Monitored partial email addresses
CREATE TABLE security_monitoredpartialemailaddress
(
    id       SERIAL       NOT NULL,
    keyword  VARCHAR(500) NOT NULL,
    created  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_monitored_partial_email_addresses PRIMARY KEY (id),
    CONSTRAINT uq_monitored_partial_email_keyword UNIQUE (keyword)
);

-- Saved searches
CREATE TABLE security_savedsearch
(
    id          SERIAL       NOT NULL,
    username    VARCHAR(250) NOT NULL,
    description TEXT         NOT NULL,
    endpoint    VARCHAR(500) NOT NULL,
    filters     TEXT,
    created     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified    TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_saved_searches PRIMARY KEY (id)
);
