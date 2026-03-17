CREATE TABLE prison_categories
(
    category_id SERIAL      NOT NULL,
    name        VARCHAR(255) NOT NULL,

    CONSTRAINT pk_prison_categories PRIMARY KEY (category_id),
    CONSTRAINT uq_prison_categories_name UNIQUE (name)
);

CREATE TABLE prison_populations
(
    population_id SERIAL      NOT NULL,
    name          VARCHAR(255) NOT NULL,

    CONSTRAINT pk_prison_populations PRIMARY KEY (population_id),
    CONSTRAINT uq_prison_populations_name UNIQUE (name)
);

CREATE TABLE prisons
(
    nomis_id               VARCHAR(10)  NOT NULL,
    name                   VARCHAR(255) NOT NULL DEFAULT '',
    region                 VARCHAR(255) NOT NULL DEFAULT '',
    pre_approval_required  BOOLEAN      NOT NULL DEFAULT FALSE,
    private_estate         BOOLEAN      NOT NULL DEFAULT FALSE,
    use_nomis_for_balances BOOLEAN      NOT NULL DEFAULT TRUE,
    created                TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified               TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_prisons PRIMARY KEY (nomis_id)
);

CREATE TABLE prison_prison_categories
(
    prison_nomis_id VARCHAR(10) NOT NULL,
    category_id     INTEGER     NOT NULL,

    CONSTRAINT pk_prison_prison_categories PRIMARY KEY (prison_nomis_id, category_id),
    CONSTRAINT fk_ppc_prison FOREIGN KEY (prison_nomis_id) REFERENCES prisons (nomis_id),
    CONSTRAINT fk_ppc_category FOREIGN KEY (category_id) REFERENCES prison_categories (category_id)
);

CREATE TABLE prison_prison_populations
(
    prison_nomis_id VARCHAR(10) NOT NULL,
    population_id   INTEGER     NOT NULL,

    CONSTRAINT pk_prison_prison_populations PRIMARY KEY (prison_nomis_id, population_id),
    CONSTRAINT fk_ppp_prison FOREIGN KEY (prison_nomis_id) REFERENCES prisons (nomis_id),
    CONSTRAINT fk_ppp_population FOREIGN KEY (population_id) REFERENCES prison_populations (population_id)
);

ALTER TABLE credits
    ADD CONSTRAINT fk_credits_prison FOREIGN KEY (prison) REFERENCES prisons (nomis_id);
