
ALTER TABLE mission
    ADD COLUMN affectation_id BINARY(16) NULL;

CREATE UNIQUE INDEX uk_mission_affectation_id
    ON mission(affectation_id);
