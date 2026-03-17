CREATE TABLE transactions
(
    transaction_id           SERIAL                      NOT NULL,
    amount                   BIGINT                      NOT NULL DEFAULT 0,
    sender_sort_code         VARCHAR(50),
    sender_account_number    VARCHAR(50),
    sender_name              VARCHAR(250),
    sender_roll_number       VARCHAR(50),
    reference                TEXT,
    received_at              TIMESTAMP WITHOUT TIME ZONE,
    ref_code                 VARCHAR(50),
    incomplete_sender_info   BOOLEAN                     NOT NULL DEFAULT FALSE,
    reference_in_sender_field BOOLEAN                    NOT NULL DEFAULT FALSE,
    credit_id                INTEGER                     NOT NULL,
    created                  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified                 TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_transactions PRIMARY KEY (transaction_id),
    CONSTRAINT uq_transactions_credit_id UNIQUE (credit_id),
    CONSTRAINT fk_transactions_credit FOREIGN KEY (credit_id) REFERENCES credits (credit_id)
);

CREATE TABLE billing_addresses
(
    billing_address_id       SERIAL                      NOT NULL,
    line1                    VARCHAR(250),
    line2                    VARCHAR(250),
    city                     VARCHAR(250),
    country                  VARCHAR(250),
    postcode                 VARCHAR(250),
    created                  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified                 TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_billing_addresses PRIMARY KEY (billing_address_id)
);

CREATE TABLE payments
(
    uuid                     UUID                        NOT NULL,
    amount                   BIGINT                      NOT NULL DEFAULT 0,
    service_charge           BIGINT                      NOT NULL DEFAULT 0,
    status                   VARCHAR(50),
    processor_id             VARCHAR(250),
    recipient_name           VARCHAR(250),
    email                    VARCHAR(254),
    cardholder_name          VARCHAR(250),
    card_number_first_digits VARCHAR(6),
    card_number_last_digits  VARCHAR(4),
    card_expiry_date         VARCHAR(5),
    card_brand               VARCHAR(250),
    ip_address               VARCHAR(45),
    credit_id                INTEGER                     NOT NULL,
    billing_address_id       INTEGER,
    created                  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified                 TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_payments PRIMARY KEY (uuid),
    CONSTRAINT uq_payments_credit_id UNIQUE (credit_id),
    CONSTRAINT fk_payments_credit FOREIGN KEY (credit_id) REFERENCES credits (credit_id),
    CONSTRAINT fk_payments_billing_address FOREIGN KEY (billing_address_id) REFERENCES billing_addresses (billing_address_id)
);
