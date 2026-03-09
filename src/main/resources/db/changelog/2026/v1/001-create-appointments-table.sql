-- liquibase formatted sql

-- changeset IlliaFransua:1
-- comment: Create sequence for appointments
CREATE SEQUENCE appointment_id_sequence START WITH 1 INCREMENT BY 20 NO CYCLE;
-- rollback DROP SEQUENCE appointment_id_sequence;

-- changeset IlliaFransua:2
-- comment: Create appointments table
CREATE TABLE appointments (
    id                      BIGINT          NOT NULL,
    master_id               BIGINT          NOT NULL,
    slug                    VARCHAR(50)     NOT NULL,
    -- Guest
    guest_name              VARCHAR(50),
    guest_pre_appointment_notes VARCHAR(500),
    -- Snapshot of shift
    shift_id                BIGINT          NOT NULL,
    date                    DATE            NOT NULL,
    start_time              TIME(0) WITHOUT TIME ZONE NOT NULL,
    end_time                TIME(0) WITHOUT TIME ZONE NOT NULL,
    -- Snapshot of offering
    offering_id             BIGINT          NOT NULL,
    offering_name           VARCHAR(128)    NOT NULL,
    offering_description    VARCHAR(1024),
    offering_price          DECIMAL(12, 2)  NOT NULL CHECK (offering_price >= 0),
    -- Snapshot of address
    address_id              BIGINT          NOT NULL,
    address_country_code    VARCHAR(2)      NOT NULL CHECK (address_country_code ~ '^[A-Z]{2}$'), -- ISO 3166-1 alpha-2 (US, CH, ZA, etc.)
    address_currency_code   VARCHAR(3)      NOT NULL CHECK (address_currency_code ~ '^[A-Z]{3}$'),
    address_full            VARCHAR(512)    NOT NULL,
    address_details         VARCHAR(200),
    address_timezone        VARCHAR(50)     NOT NULL, -- IANA timezone (America/New_York, Europe/Zurich, Africa/Cairo)
    -- Status
    status                  VARCHAR(30)     NOT NULL
        CHECK (status IN (
            'CREATED',
            'CONFIRMED',
            'CANCELLED_BY_GUEST',
            'CANCELLED_BY_MASTER',
            'NO_SHOW',
            'COMPLETED'
        )),
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_appointments PRIMARY KEY (id)
);
-- rollback DROP TABLE appointments;

-- changeset IlliaFransua:3
-- comment: Create indexes for appointments
CREATE INDEX idx_appointments_shift_id         ON appointments (shift_id);
CREATE INDEX idx_appointments_address_date     ON appointments (address_id, date);
CREATE INDEX idx_appointments_slug             ON appointments (slug);
CREATE INDEX idx_appointments_master_date      ON appointments (master_id, date);
CREATE INDEX idx_appointments_status           ON appointments (status);
-- rollback DROP INDEX idx_appointments_shift_id;
-- rollback DROP INDEX idx_appointments_address_date;
-- rollback DROP INDEX idx_appointments_slug;
-- rollback DROP INDEX idx_appointments_master_date;
-- rollback DROP INDEX idx_appointments_status;
