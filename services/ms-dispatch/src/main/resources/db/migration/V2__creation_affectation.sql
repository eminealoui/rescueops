-- =====================================================================
--  ms-dispatch - V2 : création des tables métier
--  BF-07 : recommandation (scoring multicritère)
--  BF-08 : affectation (validation par l'opérateur)
--  BF-09 : annulation / reproposition
--
--  RÈGLE FLYWAY : ne jamais modifier ce fichier après sa première exécution.
--  Toute évolution de schéma → nouveau fichier V3__..., V4__...
-- =====================================================================

-- ----------------------------------------------------------------
--  TABLE : recommandation
--  Résultats du scoring multicritère pour un incident (BF-07).
--  Une ligne par ressource candidate, triée par score DESC dans l'appli.
-- ----------------------------------------------------------------
CREATE TABLE recommandation (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    incident_id         VARCHAR(100)    NOT NULL,
    ressource_id        VARCHAR(100)    NOT NULL,
    nom_ressource       VARCHAR(200),
    type_ressource      VARCHAR(50),
    score               DOUBLE          NOT NULL COMMENT 'Score global entre 0.0 et 1.0',
    justification       TEXT            COMMENT 'Explication lisible du score',
    distance_km         DOUBLE          COMMENT 'Distance en km entre la ressource et l incident',
    latitude_incident   DOUBLE,
    longitude_incident  DOUBLE,
    disponible          BOOLEAN,
    competences_ok      BOOLEAN,
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_recommandation_incident (incident_id),
    INDEX idx_recommandation_score (incident_id, score DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------
--  TABLE : affectation
--  Affectation validée (ou proposée / annulée) d'une ressource à un incident (BF-08, BF-09).
-- ----------------------------------------------------------------
CREATE TABLE affectation (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    incident_id     VARCHAR(100)    NOT NULL,
    ressource_id    VARCHAR(100)    NOT NULL,
    type_ressource  VARCHAR(50),
    statut          VARCHAR(30)     NOT NULL DEFAULT 'PROPOSEE'
                        COMMENT 'PROPOSEE | CONFIRMEE | ANNULEE',
    score           DOUBLE          COMMENT 'Score au moment de la proposition',
    justification   TEXT,
    operateur_id    VARCHAR(100)    COMMENT 'keycloakId de l opérateur (null si PROPOSEE)',
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_affectation_incident  (incident_id),
    INDEX idx_affectation_statut    (statut),
    INDEX idx_affectation_ressource (ressource_id, statut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
