-- Service-level notifications (banner/alert messages shown in apps)
CREATE TABLE service_notification
(
    id        SERIAL       NOT NULL,
    public    BOOLEAN      NOT NULL DEFAULT FALSE,
    target    VARCHAR(30)  NOT NULL,
    level     SMALLINT     NOT NULL,
    start     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "end"     TIMESTAMP WITHOUT TIME ZONE,
    headline  VARCHAR(200) NOT NULL,
    message   TEXT         NOT NULL DEFAULT '',

    CONSTRAINT pk_service_notification PRIMARY KEY (id)
);

CREATE INDEX idx_service_notification_start_end ON service_notification (start, "end");
