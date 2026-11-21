-- =====================================================================
--  L'email du profil n'est qu'une COPIE de l'email Keycloak (qui, lui, garantit l'unicité).
--  Une contrainte d'unicité ici pouvait bloquer définitivement un utilisateur dont l'email
--  était encore porté par un ancien profil non resynchronisé. On garde un simple index.
-- =====================================================================
ALTER TABLE user_profile DROP INDEX uk_user_profile_email;
CREATE INDEX idx_user_profile_email ON user_profile (email);
