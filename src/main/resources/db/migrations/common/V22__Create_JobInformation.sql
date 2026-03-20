CREATE TABLE job_information (
    id              SERIAL       NOT NULL,
    user_id         INTEGER      NOT NULL REFERENCES mtp_users (id),
    title           VARCHAR(255) NOT NULL DEFAULT '',
    prison_estate   VARCHAR(255) NOT NULL DEFAULT '',
    tasks           TEXT         NOT NULL DEFAULT '',
    created         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_job_information PRIMARY KEY (id)
);
CREATE INDEX idx_job_information_user_id ON job_information (user_id);
