
ALTER TABLE mission
    ADD COLUMN incident_id BINARY(16) NULL,
ADD COLUMN ambulance_id BINARY(16) NULL,
ADD COLUMN ambulancier_keycloak_id VARCHAR(36) NULL;
