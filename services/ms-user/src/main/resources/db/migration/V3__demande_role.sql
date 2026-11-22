-- =====================================================================
--  Demandes de rôle professionnel. L'inscription publique donne CITOYEN ;
--  un rôle professionnel est DEMANDÉ ici puis accordé par un ADMIN (attribué dans Keycloak).
-- =====================================================================
CREATE TABLE demande_role (
    id            BINARY(16)    NOT NULL PRIMARY KEY,
    keycloak_id   VARCHAR(36)   NOT NULL,
    role_demande  VARCHAR(40)   NOT NULL,
    organisation  VARCHAR(150)  NOT NULL,
    motif         VARCHAR(500),
    statut        VARCHAR(20)   NOT NULL,
    traite_par    VARCHAR(36),
    motif_refus   VARCHAR(500),
    created_at    DATETIME(6)   NOT NULL,
    traite_le     DATETIME(6),
    version       BIGINT        NOT NULL
);
CREATE INDEX idx_demande_role_statut ON demande_role (statut, created_at);
CREATE INDEX idx_demande_role_keycloak ON demande_role (keycloak_id);
