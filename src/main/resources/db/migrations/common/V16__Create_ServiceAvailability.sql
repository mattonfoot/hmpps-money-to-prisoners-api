-- Service downtime records
CREATE TABLE service_downtime
(
    id                SERIAL       NOT NULL,
    service           VARCHAR(50)  NOT NULL,
    start_time        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_time          TIMESTAMP WITHOUT TIME ZONE,
    message_to_users  VARCHAR(255) NOT NULL DEFAULT '',

    CONSTRAINT pk_service_downtime PRIMARY KEY (id)
);

CREATE INDEX idx_service_downtime_service_start ON service_downtime (service, start_time);
