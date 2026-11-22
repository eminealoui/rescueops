
CREATE TABLE demande_reaffectation (

                                       id BINARY(16) NOT NULL PRIMARY KEY,

                                       mission_id BINARY(16) NOT NULL,

                                       motif VARCHAR(500) NOT NULL,

                                       statut VARCHAR(30) NOT NULL,

                                       demande_par VARCHAR(36) NOT NULL,

                                       date_demande DATETIME(6) NOT NULL,

                                       date_traitement DATETIME(6) NULL,

                                       version BIGINT NOT NULL,

                                       CONSTRAINT fk_reaffectation_mission
                                           FOREIGN KEY (mission_id)
                                               REFERENCES mission(id),

                                       INDEX idx_reaffectation_mission (mission_id)
);
