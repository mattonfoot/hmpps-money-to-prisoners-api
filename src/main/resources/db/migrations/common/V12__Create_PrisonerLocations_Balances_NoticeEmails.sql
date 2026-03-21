CREATE TABLE prison_prisonerlocation
(
    id              SERIAL       NOT NULL,
    prisoner_number VARCHAR(250) NOT NULL,
    prison_id       VARCHAR(10)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by      VARCHAR(250) NOT NULL,
    prisoner_dob    DATE,
    created         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified        TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_prisoner_locations PRIMARY KEY (id),
    CONSTRAINT fk_prisoner_locations_prison FOREIGN KEY (prison_id) REFERENCES prison_prison (nomis_id) ON DELETE CASCADE
);

CREATE INDEX idx_prisoner_locations_prisoner_number ON prison_prisonerlocation (prisoner_number);
CREATE INDEX idx_prisoner_locations_active ON prison_prisonerlocation (active);

CREATE TABLE prison_prisonerbalance
(
    id              SERIAL       NOT NULL,
    prisoner_number VARCHAR(250) NOT NULL,
    prison_id       VARCHAR(10)  NOT NULL,
    amount          BIGINT       NOT NULL DEFAULT 0,
    created         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified        TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_prisoner_balances PRIMARY KEY (id),
    CONSTRAINT fk_prisoner_balances_prison FOREIGN KEY (prison_id) REFERENCES prison_prison (nomis_id) ON DELETE CASCADE
);

CREATE TABLE prison_prisonercreditnoticeemail
(
    prison_id VARCHAR(10)  NOT NULL,
    email     VARCHAR(255) NOT NULL,
    created   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified  TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_prisoner_credit_notice_emails PRIMARY KEY (prison_id),
    CONSTRAINT fk_prisoner_credit_notice_emails_prison FOREIGN KEY (prison_id) REFERENCES prison_prison (nomis_id) ON DELETE CASCADE
);
