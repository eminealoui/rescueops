/** URLs locales. Aucun secret ici : le client Keycloak « rescueops-web » est public (PKCE). */
export const environment = {
  /** Préfixe de l'API : toujours la Gateway, via le proxy de ng serve (voir proxy.conf.mjs). */
  apiBase: '/api',
  keycloakIssuer: 'http://localhost:8180/realms/rescueops',
  keycloakClientId: 'rescueops-web',
};
