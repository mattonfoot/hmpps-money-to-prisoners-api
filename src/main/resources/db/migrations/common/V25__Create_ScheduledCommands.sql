-- Scheduled management commands with cron-based execution (COR-010 to COR-014)
CREATE TABLE core_scheduledcommand
(
    id                SERIAL       NOT NULL,
    name              VARCHAR(255) NOT NULL,
    arg_string        VARCHAR(500) NOT NULL DEFAULT '',
    cron_entry        VARCHAR(255) NOT NULL,
    next_execution    TIMESTAMP WITHOUT TIME ZONE,
    delete_after_next BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_scheduled_commands PRIMARY KEY (id)
);

CREATE INDEX idx_scheduled_commands_next_execution ON core_scheduledcommand (next_execution);
