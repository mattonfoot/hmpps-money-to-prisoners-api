-- Account requests for new user accounts (AUTH-060 to AUTH-067)
CREATE TABLE mtp_auth_accountrequest
(
    id              SERIAL       NOT NULL,
    username        VARCHAR(150) NOT NULL,
    first_name      VARCHAR(150) NOT NULL DEFAULT '',
    last_name       VARCHAR(150) NOT NULL DEFAULT '',
    email           VARCHAR(254) NOT NULL DEFAULT '',
    role_id         INTEGER REFERENCES mtp_auth_role (id),
    prison_nomis_id VARCHAR(10) REFERENCES prison_prison (nomis_id),
    status          VARCHAR(20)  NOT NULL DEFAULT 'pending',
    created         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified        TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_account_requests PRIMARY KEY (id)
);

CREATE INDEX idx_account_requests_status_created ON mtp_auth_accountrequest (status, created DESC);
