-- =====================================================================
--  DONNÉES FICTIVES de démonstration (profil "demo" uniquement : local,demo).
--  8 établissements du Grand Tunis, coordonnées approximatives. Aucune donnée réelle de capacité.
-- =====================================================================
SET @now = UTC_TIMESTAMP(6);

SET @h = UUID_TO_BIN(UUID());
INSERT INTO hopital (id, nom, adresse, ville, latitude, longitude, telephone, actif, created_at, updated_at, version)
VALUES (@h, 'CHU Charles Nicolle', 'Boulevard du 9 Avril 1938', 'Tunis', 36.8028, 10.1617, '+21671000001', TRUE, @now, @now, 0);
INSERT INTO service_hospitalier (id, hopital_id, specialite, capacite_totale, places_disponibles, actif, version) VALUES
 (UUID_TO_BIN(UUID()), @h, 'REANIMATION', 24, 0, TRUE, 0),
 (UUID_TO_BIN(UUID()), @h, 'CARDIOLOGIE', 18, 5, TRUE, 0),
 (UUID_TO_BIN(UUID()), @h, 'URGENCES_GENERALES', 30, 9, TRUE, 0);

SET @h = UUID_TO_BIN(UUID());
INSERT INTO hopital (id, nom, adresse, ville, latitude, longitude, telephone, actif, created_at, updated_at, version)
VALUES (@h, 'CHU La Rabta', 'Jabbari', 'Tunis', 36.8108, 10.1535, '+21671000002', TRUE, @now, @now, 0);
INSERT INTO service_hospitalier (id, hopital_id, specialite, capacite_totale, places_disponibles, actif, version) VALUES
 (UUID_TO_BIN(UUID()), @h, 'CARDIOLOGIE', 18, 1, TRUE, 0),
 (UUID_TO_BIN(UUID()), @h, 'NEUROLOGIE', 12, 4, TRUE, 0);

SET @h = UUID_TO_BIN(UUID());
INSERT INTO hopital (id, nom, adresse, ville, latitude, longitude, telephone, actif, created_at, updated_at, version)
VALUES (@h, 'Hôpital Habib Thameur', 'Montfleury', 'Tunis', 36.7889, 10.1806, '+21671000003', TRUE, @now, @now, 0);
INSERT INTO service_hospitalier (id, hopital_id, specialite, capacite_totale, places_disponibles, actif, version) VALUES
 (UUID_TO_BIN(UUID()), @h, 'TRAUMATOLOGIE', 20, 6, TRUE, 0),
 (UUID_TO_BIN(UUID()), @h, 'URGENCES_GENERALES', 25, 8, TRUE, 0);

SET @h = UUID_TO_BIN(UUID());
INSERT INTO hopital (id, nom, adresse, ville, latitude, longitude, telephone, actif, created_at, updated_at, version)
VALUES (@h, 'Hôpital Mongi Slim', 'La Marsa', 'La Marsa', 36.8890, 10.3120, '+21671000004', TRUE, @now, @now, 0);
INSERT INTO service_hospitalier (id, hopital_id, specialite, capacite_totale, places_disponibles, actif, version) VALUES
 (UUID_TO_BIN(UUID()), @h, 'URGENCES_GENERALES', 30, 11, TRUE, 0),
 (UUID_TO_BIN(UUID()), @h, 'CARDIOLOGIE', 10, 6, TRUE, 0),
 (UUID_TO_BIN(UUID()), @h, 'MATERNITE', 16, 7, TRUE, 0);

SET @h = UUID_TO_BIN(UUID());
INSERT INTO hopital (id, nom, adresse, ville, latitude, longitude, telephone, actif, created_at, updated_at, version)
VALUES (@h, 'Hôpital d''enfants Béchir Hamza', 'Bab Saadoun', 'Tunis', 36.8059, 10.1622, '+21671000005', TRUE, @now, @now, 0);
INSERT INTO service_hospitalier (id, hopital_id, specialite, capacite_totale, places_disponibles, actif, version) VALUES
 (UUID_TO_BIN(UUID()), @h, 'PEDIATRIE', 16, 0, TRUE, 0),
 (UUID_TO_BIN(UUID()), @h, 'REANIMATION', 8, 2, TRUE, 0);

SET @h = UUID_TO_BIN(UUID());
INSERT INTO hopital (id, nom, adresse, ville, latitude, longitude, telephone, actif, created_at, updated_at, version)
VALUES (@h, 'Centre de traumatologie et des grands brûlés', 'Ben Arous', 'Ben Arous', 36.7472, 10.2310, '+21671000006', TRUE, @now, @now, 0);
INSERT INTO service_hospitalier (id, hopital_id, specialite, capacite_totale, places_disponibles, actif, version) VALUES
 (UUID_TO_BIN(UUID()), @h, 'BRULES', 14, 3, TRUE, 0),
 (UUID_TO_BIN(UUID()), @h, 'TRAUMATOLOGIE', 22, 9, TRUE, 0),
 (UUID_TO_BIN(UUID()), @h, 'REANIMATION', 10, 1, TRUE, 0);

SET @h = UUID_TO_BIN(UUID());
INSERT INTO hopital (id, nom, adresse, ville, latitude, longitude, telephone, actif, created_at, updated_at, version)
VALUES (@h, 'Institut Salah Azaiez', 'Bab Saadoun', 'Tunis', 36.8075, 10.1588, '+21671000007', TRUE, @now, @now, 0);
INSERT INTO service_hospitalier (id, hopital_id, specialite, capacite_totale, places_disponibles, actif, version) VALUES
 (UUID_TO_BIN(UUID()), @h, 'URGENCES_GENERALES', 12, 3, TRUE, 0);

SET @h = UUID_TO_BIN(UUID());
INSERT INTO hopital (id, nom, adresse, ville, latitude, longitude, telephone, actif, created_at, updated_at, version)
VALUES (@h, 'Hôpital régional de l''Ariana', 'Ariana Ville', 'Ariana', 36.8665, 10.1647, '+21671000008', TRUE, @now, @now, 0);
INSERT INTO service_hospitalier (id, hopital_id, specialite, capacite_totale, places_disponibles, actif, version) VALUES
 (UUID_TO_BIN(UUID()), @h, 'URGENCES_GENERALES', 20, 12, TRUE, 0),
 (UUID_TO_BIN(UUID()), @h, 'CARDIOLOGIE', 8, 3, TRUE, 0),
 (UUID_TO_BIN(UUID()), @h, 'MATERNITE', 12, 5, TRUE, 0);
