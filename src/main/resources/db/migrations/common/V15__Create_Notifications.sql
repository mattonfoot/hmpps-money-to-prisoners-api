-- Notification events
CREATE TABLE notification_events
(
    id                  SERIAL       NOT NULL,
    rule                VARCHAR(8)   NOT NULL,
    description         VARCHAR(500) NOT NULL DEFAULT '',
    triggered_at        TIMESTAMP WITHOUT TIME ZONE,
    username            VARCHAR(250),
    credit_id           BIGINT,
    disbursement_id     BIGINT,
    sender_profile_id   BIGINT,
    prisoner_profile_id BIGINT,

    CONSTRAINT pk_notification_events PRIMARY KEY (id),
    CONSTRAINT fk_notification_events_credit FOREIGN KEY (credit_id) REFERENCES credits (credit_id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_events_disbursement FOREIGN KEY (disbursement_id) REFERENCES disbursements (disbursement_id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_events_sender_profile FOREIGN KEY (sender_profile_id) REFERENCES sender_profiles (sender_profile_id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_events_prisoner_profile FOREIGN KEY (prisoner_profile_id) REFERENCES prisoner_profiles (prisoner_profile_id) ON DELETE CASCADE
);

CREATE INDEX idx_notification_events_triggered_at_id ON notification_events (triggered_at DESC, id);
CREATE INDEX idx_notification_events_rule ON notification_events (rule);

-- Email notification preferences
CREATE TABLE email_notification_preferences
(
    id          SERIAL       NOT NULL,
    username    VARCHAR(250) NOT NULL,
    frequency   VARCHAR(50)  NOT NULL,
    last_sent_at DATE,

    CONSTRAINT pk_email_notification_preferences PRIMARY KEY (id),
    CONSTRAINT uq_email_notification_preferences_username UNIQUE (username)
);
