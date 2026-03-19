CREATE TABLE disbursements
(
    disbursement_id      SERIAL                      NOT NULL,
    amount               BIGINT                      NOT NULL,
    method               VARCHAR(50)                 NOT NULL,
    prison               VARCHAR(10),
    prisoner_number      VARCHAR(250),
    prisoner_name        VARCHAR(250),
    recipient_first_name VARCHAR(250),
    recipient_last_name  VARCHAR(250),
    recipient_email      VARCHAR(254),
    address_line1        VARCHAR(250),
    address_line2        VARCHAR(250),
    city                 VARCHAR(250),
    postcode             VARCHAR(250),
    country              VARCHAR(250),
    sort_code            VARCHAR(50),
    account_number       VARCHAR(50),
    roll_number          VARCHAR(50),
    recipient_is_company BOOLEAN                     NOT NULL DEFAULT FALSE,
    resolution           VARCHAR(50)                 NOT NULL DEFAULT 'PENDING',
    nomis_transaction_id VARCHAR(50),
    invoice_number       VARCHAR(50),
    created              TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified             TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_disbursements PRIMARY KEY (disbursement_id)
);

CREATE INDEX idx_disbursements_prisoner_number ON disbursements (prisoner_number);
CREATE INDEX idx_disbursements_resolution ON disbursements (resolution);
CREATE INDEX idx_disbursements_prison ON disbursements (prison);

CREATE TABLE disbursement_logs
(
    disbursement_log_id SERIAL                      NOT NULL,
    action              VARCHAR(50)                 NOT NULL,
    disbursement_id     INTEGER,
    user_id             VARCHAR(255),
    created             TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_disbursement_logs PRIMARY KEY (disbursement_log_id),
    CONSTRAINT fk_disbursement_logs_disbursement FOREIGN KEY (disbursement_id) REFERENCES disbursements (disbursement_id)
);

CREATE TABLE disbursement_comments
(
    disbursement_comment_id SERIAL                      NOT NULL,
    comment                 TEXT                        NOT NULL,
    category                VARCHAR(100),
    disbursement_id         INTEGER,
    user_id                 VARCHAR(255),
    created                 TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified                TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_disbursement_comments PRIMARY KEY (disbursement_comment_id),
    CONSTRAINT fk_disbursement_comments_disbursement FOREIGN KEY (disbursement_id) REFERENCES disbursements (disbursement_id)
);
