-- Comments table for credit annotations
CREATE TABLE credit_comment
(
    comment_id SERIAL                      NOT NULL,
    comment    TEXT                        NOT NULL,
    credit_id  INTEGER,
    user_id    VARCHAR(255),
    created    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified   TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_comments PRIMARY KEY (comment_id),
    CONSTRAINT fk_comments_credit FOREIGN KEY (credit_id) REFERENCES credit_credit (credit_id)
);

CREATE INDEX idx_comments_credit_id ON credit_comment (credit_id);
