
CREATE TABLE historique_statut_mission (
                                           id BINARY(16) NOT NULL PRIMARY KEY,
                                           mission_id BINARY(16) NOT NULL,
                                           ancien_statut VARCHAR(30) NOT NULL,
                                           nouveau_statut VARCHAR(30) NOT NULL,
                                           date_changement DATETIME(6) NOT NULL,
                                           modifie_par VARCHAR(36) NOT NULL,

                                           CONSTRAINT fk_historique_mission
                                               FOREIGN KEY (mission_id)
                                                   REFERENCES mission(id)
);

CREATE INDEX idx_historique_mission
    ON historique_statut_mission(mission_id);
