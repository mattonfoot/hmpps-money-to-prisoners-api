-- Private estate batches table
CREATE TABLE private_estate_batches
(
    ref          VARCHAR(30)                 NOT NULL,
    prison       VARCHAR(10)                 NOT NULL,
    date         DATE                        NOT NULL,
    total_amount BIGINT                      NOT NULL DEFAULT 0,
    created      TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_private_estate_batches PRIMARY KEY (ref),
    CONSTRAINT fk_private_estate_batches_prison FOREIGN KEY (prison) REFERENCES prisons (nomis_id)
);

-- Join table for private estate batch credits
CREATE TABLE private_estate_batch_credits
(
    ref       VARCHAR(30) NOT NULL,
    credit_id INTEGER     NOT NULL,

    CONSTRAINT pk_private_estate_batch_credits PRIMARY KEY (ref, credit_id),
    CONSTRAINT fk_peb_credits_batch FOREIGN KEY (ref) REFERENCES private_estate_batches (ref) ON DELETE CASCADE,
    CONSTRAINT fk_peb_credits_credit FOREIGN KEY (credit_id) REFERENCES credits (credit_id)
);

CREATE INDEX idx_private_estate_batches_prison ON private_estate_batches (prison);
CREATE INDEX idx_private_estate_batches_date ON private_estate_batches (date);
CREATE INDEX idx_peb_credits_ref ON private_estate_batch_credits (ref);
