-- =====================================================================
--  ms-user : profils métier liés aux comptes Keycloak.
--  AUCUN mot de passe et AUCUN rôle ici : Keycloak en est la seule autorité.
-- =====================================================================
CREATE TABLE user_profile (
    id            BINARY(16)   NOT NULL PRIMARY KEY,
    keycloak_id   VARCHAR(36)  NOT NULL,
    email         VARCHAR(254),
    first_name    VARCHAR(100),
    last_name     VARCHAR(100),
    phone         VARCHAR(20),
    address       VARCHAR(255),
    organisation  VARCHAR(150),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    version       BIGINT       NOT NULL,
    CONSTRAINT uk_user_profile_keycloak_id UNIQUE (keycloak_id),
    CONSTRAINT uk_user_profile_email UNIQUE (email)
);

CREATE INDEX idx_user_profile_last_name ON user_profile (last_name);
