CREATE TABLE balances
(
    balance_id SERIAL                      NOT NULL,
    closing_balance BIGINT                 NOT NULL,
    date DATE                              NOT NULL UNIQUE,
    created TIMESTAMP WITHOUT TIME ZONE    NOT NULL,
    modified TIMESTAMP WITHOUT TIME ZONE   NOT NULL,

    CONSTRAINT pk_balances PRIMARY KEY (balance_id)
);