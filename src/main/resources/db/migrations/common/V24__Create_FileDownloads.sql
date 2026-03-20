-- File download tracking records (COR-001 to COR-003)
CREATE TABLE file_downloads
(
    id       SERIAL      NOT NULL,
    label    VARCHAR(255) NOT NULL,
    date     DATE         NOT NULL,
    created  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_file_downloads PRIMARY KEY (id),
    CONSTRAINT uq_file_downloads_label_date UNIQUE (label, date)
);

CREATE INDEX idx_file_downloads_label ON file_downloads (label);
CREATE INDEX idx_file_downloads_date ON file_downloads (date);
