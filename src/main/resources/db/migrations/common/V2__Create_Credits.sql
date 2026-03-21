CREATE TABLE credit_credit
(
    credit_id              SERIAL                      NOT NULL,
    amount                 BIGINT                      NOT NULL,
    prisoner_number        VARCHAR(250),
    prisoner_name          VARCHAR(250),
    prisoner_dob           DATE,
    prison                 VARCHAR(10),
    resolution             VARCHAR(50)                 NOT NULL DEFAULT 'PENDING',
    reconciled             BOOLEAN                     NOT NULL DEFAULT FALSE,
    reviewed               BOOLEAN                     NOT NULL DEFAULT FALSE,
    blocked                BOOLEAN                     NOT NULL DEFAULT FALSE,
    received_at            TIMESTAMP WITHOUT TIME ZONE,
    owner                  VARCHAR(255),
    nomis_transaction_id   VARCHAR(50),
    source                 VARCHAR(50)                 NOT NULL DEFAULT 'UNKNOWN',
    created                TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified               TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_credits PRIMARY KEY (credit_id)
);

CREATE INDEX idx_credits_prisoner_number ON credit_credit (prisoner_number);
CREATE INDEX idx_credits_amount ON credit_credit (amount);
CREATE INDEX idx_credits_received_at ON credit_credit (received_at);
CREATE INDEX idx_credits_resolution ON credit_credit (resolution);
CREATE INDEX idx_credits_owner ON credit_credit (owner);
