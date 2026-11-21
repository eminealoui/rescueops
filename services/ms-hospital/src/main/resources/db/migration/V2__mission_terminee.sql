-- Missions terminées (événement mission.terminee de ms-intervention) : plus aucune demande ne peut
-- être créée pour une mission close, même si l'événement arrive avant une requête tardive.
CREATE TABLE mission_terminee (
    mission_id   BINARY(16)   NOT NULL PRIMARY KEY,
    terminee_le  DATETIME(6)  NOT NULL
);
