
CREATE TABLE evenement_affectation_recu (
                                            event_id BINARY(16) NOT NULL PRIMARY KEY,
                                            affectation_id BINARY(16) NOT NULL,
                                            payload_json JSON NOT NULL,
                                            recu_le DATETIME(6) NOT NULL,
                                            traite BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_evenement_affectation
    ON evenement_affectation_recu(affectation_id);
