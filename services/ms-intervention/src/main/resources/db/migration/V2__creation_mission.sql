
CREATE TABLE mission (
                         id          BINARY(16)   NOT NULL PRIMARY KEY,
                         commentaire VARCHAR(500) NOT NULL,
                         statut      VARCHAR(30)  NOT NULL,
                         cree_par    VARCHAR(36)  NOT NULL,
                         cree_le     DATETIME(6)  NOT NULL,
                         version     BIGINT       NOT NULL
);
