CREATE TABLE IF NOT EXISTS bootcamp (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(50)  NOT NULL,
    description    VARCHAR(90)  NOT NULL,
    launch_date    DATE         NOT NULL,
    duration_days  INT          NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS bootcamp_capability (
    bootcamp_id   BIGINT NOT NULL,
    capability_id BIGINT NOT NULL,
    PRIMARY KEY (bootcamp_id, capability_id),
    CONSTRAINT fk_bc_bootcamp FOREIGN KEY (bootcamp_id) REFERENCES bootcamp(id)
);
