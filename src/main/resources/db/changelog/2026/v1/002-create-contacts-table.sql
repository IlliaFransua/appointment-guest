-- liquibase formatted sql

-- changeset IlliaFransua:1
-- comment: Create sequence for contacts
CREATE SEQUENCE contact_id_sequence START WITH 1 INCREMENT BY 20 NO CYCLE;
-- rollback DROP SEQUENCE contact_id_sequence;

-- changeset IlliaFransua:2
-- comment: Create contacts table
CREATE TABLE contacts (
    id             BIGINT          NOT NULL,
    appointment_id BIGINT          NOT NULL,

    value          VARCHAR(255),
    value_hash     VARCHAR(64),
    type           VARCHAR(20)     NOT NULL,
    status         VARCHAR(30)     NOT NULL,

    created_at     TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_contacts PRIMARY KEY (id),
    
    CONSTRAINT check_contact_type CHECK (type IN (
        'PHONE', 'EMAIL', 'TELEGRAM', 'WHATSAPP'
    )),
    
    CONSTRAINT check_contact_status CHECK (status IN (
        'ATTACHED', 'PENDING_VERIFICATION', 'VERIFIED'
    ))
);
-- rollback DROP TABLE contacts;

-- changeset IlliaFransua:3
-- comment: Create indexes and foreign key for contacts
CREATE INDEX idx_contacts_appointment_id ON contacts (appointment_id);
CREATE UNIQUE INDEX uk_contacts_appointment_type ON contacts (appointment_id, type);
CREATE INDEX idx_contacts_value_hash ON contacts (value_hash);
-- rollback DROP INDEX idx_contacts_appointment_id;
-- rollback DROP INDEX uk_contacts_appointment_type;
-- rollback DROP INDEX idx_contacts_value_hash;
