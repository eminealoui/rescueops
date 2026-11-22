import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { ToastService } from '../ui/toast.service';
import { estApiRescueOps } from './api.interceptor';

/** Pour un appel qui gère lui-même ses erreurs (pas de toast automatique). */
export const ERREUR_GEREE = new HttpContextToken<boolean>(() => false);

export const MESSAGE_INDISPONIBLE = 'Service momentanément indisponible. Réessayez dans un instant.';
const MESSAGE_GENERIQUE = 'Une erreur inattendue est survenue.';

/** Réponse d'erreur RFC 9457 renvoyée par tous les microservices (GlobalExceptionHandler). */
export interface ProblemDetail {
  status?: number;
  title?: string;
  detail?: string;
  code?: string;
  correlationId?: string;
  errors?: { field: string; message: string }[];
}

/**
 * Traduit une erreur HTTP en message pour l'utilisateur (jamais de message technique) :
 * 401 → nouvelle connexion, 403 → page « Accès refusé », 0/502/503/504 → « indisponible », sinon le `detail`.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);
  const router = inject(Router);
  const auth = inject(AuthService);

  return next(req).pipe(
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse) || !estApiRescueOps(req.url)) return throwError(() => err);
      if (req.context.get(ERREUR_GEREE)) return throwError(() => err);

      const pb = (err.error && typeof err.error === 'object' ? err.error : {}) as ProblemDetail;
      switch (err.status) {
        case 401:
          // Token absent ou refusé : une seule redirection vers Keycloak, en revenant sur la page courante.
          if (!auth.reconnecter(router.url)) toast.danger('Votre session n\'est pas reconnue par le serveur. Reconnectez-vous.');
          break;
        case 403:
          void router.navigateByUrl('/acces-refuse');
          break;
        case 0:
        case 502:
        case 503:
        case 504:
          toast.danger(MESSAGE_INDISPONIBLE);
          break;
        default:
          toast.danger(pb.detail || MESSAGE_GENERIQUE);
      }
      return throwError(() => err);
    }),
  );
};
