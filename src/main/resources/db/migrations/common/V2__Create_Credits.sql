CREATE TABLE credits
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

CREATE INDEX idx_credits_prisoner_number ON credits (prisoner_number);
CREATE INDEX idx_credits_amount ON credits (amount);
CREATE INDEX idx_credits_received_at ON credits (received_at);
CREATE INDEX idx_credits_resolution ON credits (resolution);
CREATE INDEX idx_credits_owner ON credits (owner);
