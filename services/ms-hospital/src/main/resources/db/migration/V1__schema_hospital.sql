-- =====================================================================
--  ms-hospital : établissements, services, capacités et demandes de transfert.
--  Les clés étrangères restent INTERNES à hospital_db : mission_id et les *_user_id
--  sont des identifiants externes (ms-intervention, Keycloak), sans FK.
-- =====================================================================
CREATE TABLE hopital (
    id          BINARY(16)    NOT NULL PRIMARY KEY,
    nom         VARCHAR(150)  NOT NULL,
    adresse     VARCHAR(255),
    ville       VARCHAR(100)  NOT NULL,
    latitude    DOUBLE        NOT NULL,
    longitude   DOUBLE        NOT NULL,
    telephone   VARCHAR(20),
    actif       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    version     BIGINT        NOT NULL
);

CREATE TABLE service_hospitalier (
    id                  BINARY(16)   NOT NULL PRIMARY KEY,
    hopital_id          BINARY(16)   NOT NULL,
    specialite          VARCHAR(30)  NOT NULL,
    capacite_totale     INT          NOT NULL,
    places_disponibles  INT          NOT NULL,
    actif               BOOLEAN      NOT NULL DEFAULT TRUE,
    version             BIGINT       NOT NULL,
    CONSTRAINT fk_service_hopital FOREIGN KEY (hopital_id) REFERENCES hopital (id),
    CONSTRAINT uk_service_hopital_specialite UNIQUE (hopital_id, specialite),
    -- Garde-fou ultime (BNF-05) : jamais de places négatives ni supérieures à la capacité.
    CONSTRAINT ck_service_places CHECK (places_disponibles >= 0 AND places_disponibles <= capacite_totale),
    CONSTRAINT ck_service_capacite CHECK (capacite_totale >= 0)
);

CREATE TABLE demande_transfert (
    id                  BINARY(16)   NOT NULL PRIMARY KEY,
    mission_id          BINARY(16)   NOT NULL,
    specialite_requise  VARCHAR(30)  NOT NULL,
    gravite             VARCHAR(10)  NOT NULL,
    latitude_origine    DOUBLE,
    longitude_origine   DOUBLE,
    service_id          BINARY(16),
    statut              VARCHAR(20)  NOT NULL,
    expire_at           DATETIME(6),
    demandeur_user_id   VARCHAR(36)  NOT NULL,
    traite_par_user_id  VARCHAR(36),
    motif_refus         VARCHAR(500),
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    version             BIGINT       NOT NULL,
    CONSTRAINT fk_demande_service FOREIGN KEY (service_id) REFERENCES service_hospitalier (id)
);
CREATE INDEX idx_demande_statut_expire ON demande_transfert (statut, expire_at);
CREATE INDEX idx_demande_mission ON demande_transfert (mission_id);

CREATE TABLE historique_transfert (
    id              BINARY(16)   NOT NULL PRIMARY KEY,
    demande_id      BINARY(16)   NOT NULL,
    ancien_statut   VARCHAR(20),
    nouveau_statut  VARCHAR(20)  NOT NULL,
    acteur_user_id  VARCHAR(36)  NOT NULL,
    horodatage      DATETIME(6)  NOT NULL,
    commentaire     VARCHAR(500),
    CONSTRAINT fk_historique_demande FOREIGN KEY (demande_id) REFERENCES demande_transfert (id)
);
CREATE INDEX idx_historique_horodatage ON historique_transfert (horodatage);

CREATE TABLE historique_capacite (
    id                 BINARY(16)   NOT NULL PRIMARY KEY,
    service_id         BINARY(16)   NOT NULL,
    ancienne_capacite  INT          NOT NULL,
    nouvelle_capacite  INT          NOT NULL,
    anciennes_places   INT          NOT NULL,
    nouvelles_places   INT          NOT NULL,
    acteur_user_id     VARCHAR(36)  NOT NULL,
    horodatage         DATETIME(6)  NOT NULL,
    motif              VARCHAR(500),
    CONSTRAINT fk_historique_capacite_service FOREIGN KEY (service_id) REFERENCES service_hospitalier (id)
);

-- Idempotence des consommateurs RabbitMQ (BNF-08) : un eventId n'est traité qu'une fois.
CREATE TABLE evenement_traite (
    event_id   BINARY(16)   NOT NULL PRIMARY KEY,
    traite_le  DATETIME(6)  NOT NULL
);
