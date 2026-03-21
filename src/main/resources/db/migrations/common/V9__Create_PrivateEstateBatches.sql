-- Private estate credit_processingbatch table
CREATE TABLE credit_privateestatebatch
(
    ref          VARCHAR(30)                 NOT NULL,
    prison       VARCHAR(10)                 NOT NULL,
    date         DATE                        NOT NULL,
    total_amount BIGINT                      NOT NULL DEFAULT 0,
    created      TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_private_estate_batches PRIMARY KEY (ref),
    CONSTRAINT fk_private_estate_batches_prison FOREIGN KEY (prison) REFERENCES prison_prison (nomis_id)
);

-- Join table for private estate batch credit_credit
CREATE TABLE credit_privateestatebatch_credits
(
    ref       VARCHAR(30) NOT NULL,
    credit_id INTEGER     NOT NULL,

    CONSTRAINT pk_private_estate_batch_credits PRIMARY KEY (ref, credit_id),
    CONSTRAINT fk_peb_credits_batch FOREIGN KEY (ref) REFERENCES credit_privateestatebatch (ref) ON DELETE CASCADE,
    CONSTRAINT fk_peb_credits_credit FOREIGN KEY (credit_id) REFERENCES credit_credit (credit_id)
);

CREATE INDEX idx_private_estate_batches_prison ON credit_privateestatebatch (prison);
CREATE INDEX idx_private_estate_batches_date ON credit_privateestatebatch (date);
CREATE INDEX idx_peb_credits_ref ON credit_privateestatebatch_credits (ref);
