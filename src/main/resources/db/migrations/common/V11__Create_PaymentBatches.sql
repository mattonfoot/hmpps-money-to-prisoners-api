-- Payment reconciliation credit_processingbatch table for grouping online payment credit_credit
CREATE TABLE payment_batches
(
    payment_batch_id  SERIAL                      NOT NULL,
    ref_code          INTEGER                     NOT NULL,
    settlement_date   DATE,
    created           TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_payment_batches PRIMARY KEY (payment_batch_id),
    CONSTRAINT uq_payment_batches_ref_code UNIQUE (ref_code)
);

-- Join table for payment_batch-credit many-to-many relationship
CREATE TABLE payment_batch_credits
(
    payment_batch_id  INTEGER NOT NULL,
    credit_id         INTEGER NOT NULL,

    CONSTRAINT pk_payment_batch_credits PRIMARY KEY (payment_batch_id, credit_id),
    CONSTRAINT fk_payment_batch_credits_batch FOREIGN KEY (payment_batch_id) REFERENCES payment_batches (payment_batch_id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_batch_credits_credit FOREIGN KEY (credit_id) REFERENCES credit_credit (credit_id)
);

CREATE INDEX idx_payment_batch_credits_batch_id ON payment_batch_credits (payment_batch_id);
CREATE INDEX idx_payment_batch_credits_credit_id ON payment_batch_credits (credit_id);
CREATE INDEX idx_payment_batches_settlement_date ON payment_batches (settlement_date);
