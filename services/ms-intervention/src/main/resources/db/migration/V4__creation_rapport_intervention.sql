
CREATE TABLE rapport_intervention (
                                      id BINARY(16) NOT NULL PRIMARY KEY,
                                      mission_id BINARY(16) NOT NULL,
                                      bilan TEXT NOT NULL,
                                      actions_realisees TEXT NOT NULL,
                                      observations TEXT,
                                      cree_par VARCHAR(36) NOT NULL,
                                      cree_le DATETIME(6) NOT NULL,
                                      version BIGINT NOT NULL,

                                      CONSTRAINT uq_rapport_mission
                                          UNIQUE (mission_id),

                                      CONSTRAINT fk_rapport_mission
                                          FOREIGN KEY (mission_id)
                                              REFERENCES mission(id)
);
