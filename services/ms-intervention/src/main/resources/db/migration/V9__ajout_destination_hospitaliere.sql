ALTER TABLE mission
    ADD COLUMN transfert_id BINARY(16) NULL,
    ADD COLUMN service_hospitalier_id BINARY(16) NULL,
    ADD COLUMN destination_confirmee_le DATETIME(6) NULL;
