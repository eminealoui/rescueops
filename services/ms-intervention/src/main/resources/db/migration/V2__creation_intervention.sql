-- =====================================================================
--  ms-intervention - V2 : table métier + intervention statique de test
--  Règle Flyway : ne jamais modifier ce fichier après sa première exécution.
-- =====================================================================

CREATE TABLE intervention (
    id                  VARCHAR(36)     NOT NULL,
    incident_id         VARCHAR(100)    NOT NULL,
    titre               VARCHAR(200)    NOT NULL,
    type_intervention   VARCHAR(50)     NOT NULL,
    latitude            DOUBLE,
    longitude           DOUBLE,
    statut              VARCHAR(30)     NOT NULL DEFAULT 'EN_ATTENTE',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_intervention_incident (incident_id),
    INDEX idx_intervention_statut (statut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Intervention fixe, utilisée comme référence stable pour tester le CRUD ms-dispatch.
INSERT INTO intervention (id, incident_id, titre, type_intervention, latitude, longitude, statut)
VALUES ('11111111-1111-4111-8111-111111111111',
        '11111111-1111-4111-8111-111111111111',
        'Intervention statique - test dispatch CRUD',
        'URGENCE',
        36.8065,
        10.1815,
        'EN_ATTENTE');
