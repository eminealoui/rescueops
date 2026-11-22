import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Role } from './roles';

/**
 * Guard d'ERGONOMIE uniquement (la vraie sécurité est côté API).
 * Non connecté → connexion Keycloak, puis retour sur la page demandée. Rôle manquant → /acces-refuse.
 */
export function roleGuard(...roles: Role[]): CanActivateFn {
  return (_route, state) => {
    const auth = inject(AuthService);
    if (!auth.isAuthenticated()) {
      auth.login(state.url);
      return false;
    }
    return auth.hasAnyRole(roles) ? true : inject(Router).createUrlTree(['/acces-refuse']);
  };
}

/** Page publique : un utilisateur déjà connecté va directement sur son tableau d'accueil. */
export const publicGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (!auth.isAuthenticated()) return true;
  return inject(Router).parseUrl(auth.retourApresConnexion() ?? '/accueil');
};
