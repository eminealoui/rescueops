ALTER TABLE mission

    ADD COLUMN type_itineraire VARCHAR(30) NULL,

    ADD COLUMN latitude_depart DOUBLE NULL,
    ADD COLUMN longitude_depart DOUBLE NULL,

    ADD COLUMN latitude_destination DOUBLE NULL,
    ADD COLUMN longitude_destination DOUBLE NULL,

    ADD COLUMN distance_km DOUBLE NULL,

    ADD COLUMN duree_estimee_minutes INT NULL,

    ADD COLUMN itineraire_modifie_le DATETIME(6) NULL;
