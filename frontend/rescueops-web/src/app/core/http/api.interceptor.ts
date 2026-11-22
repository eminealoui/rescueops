import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';

/** Vrai seulement pour l'API RescueOps (chemin relatif /api/..., donc la même origine que l'application). */
export function estApiRescueOps(url: string): boolean {
  return url === environment.apiBase || url.startsWith(environment.apiBase + '/');
}

/**
 * Ajoute le Bearer et un X-Correlation-Id, UNIQUEMENT vers l'API RescueOps.
 * Les tuiles de carte, les polices ou toute autre origine ne reçoivent jamais le token.
 */
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  if (!estApiRescueOps(req.url)) return next(req);
  const token = inject(AuthService).token();
  const headers: Record<string, string> = { 'X-Correlation-Id': crypto.randomUUID() };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return next(req.clone({ setHeaders: headers }));
};
