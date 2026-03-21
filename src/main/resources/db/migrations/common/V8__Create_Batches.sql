-- Batches table for processing groups of credit_credit
CREATE TABLE credit_processingbatch
(
    batch_id  SERIAL                      NOT NULL,
    owner     VARCHAR(255)                NOT NULL,
    created   TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_batches PRIMARY KEY (batch_id)
);

-- Join table for batch-credit many-to-many relationship
CREATE TABLE credit_processingbatch_credits
(
    batch_id  INTEGER NOT NULL,
    credit_id INTEGER NOT NULL,

    CONSTRAINT pk_batch_credits PRIMARY KEY (batch_id, credit_id),
    CONSTRAINT fk_batch_credits_batch FOREIGN KEY (batch_id) REFERENCES credit_processingbatch (batch_id) ON DELETE CASCADE,
    CONSTRAINT fk_batch_credits_credit FOREIGN KEY (credit_id) REFERENCES credit_credit (credit_id)
);

CREATE INDEX idx_batch_credits_batch_id ON credit_processingbatch_credits (batch_id);
CREATE INDEX idx_batch_credits_credit_id ON credit_processingbatch_credits (credit_id);
CREATE INDEX idx_batches_owner ON credit_processingbatch (owner);
