import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthConfig, OAuthService } from 'angular-oauth2-oidc';
import { filter } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Role, decoderJwt, extraireRoles } from './roles';

export const authConfig: AuthConfig = {
  issuer: environment.keycloakIssuer,
  clientId: environment.keycloakClientId,
  responseType: 'code', // Authorization Code + PKCE (S256), sans secret côté navigateur
  redirectUri: window.location.origin + '/',
  postLogoutRedirectUri: window.location.origin + '/',
  scope: 'openid profile email',
  // Pages de connexion et d'inscription Keycloak en français, quelle que soit la langue du navigateur.
  customQueryParams: { ui_locales: 'fr' },
  useSilentRefresh: false, // renouvellement par refresh token (pas d'iframe)
  timeoutFactor: 0.75,
  sessionChecksEnabled: false,
  clearHashAfterLogin: true,
  showDebugInformation: false,
};

export interface Utilisateur {
  sub: string;
  nom: string;
  email: string;
  initiales: string;
}

/**
 * État de connexion exposé en signals. Les rôles servent UNIQUEMENT à l'ergonomie (menus, guards) :
 * la sécurité réelle est appliquée par la Gateway et les microservices.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oauth = inject(OAuthService);
  private readonly router = inject(Router);

  private readonly accessToken = signal<string | null>(null);

  readonly roles = computed(() => extraireRoles(this.accessToken()));
  readonly isAuthenticated = computed(() => this.accessToken() !== null);
  readonly utilisateur = computed<Utilisateur | null>(() => {
    const c = decoderJwt(this.accessToken());
    if (!c) return null;
    const nom = String(c['name'] ?? c['preferred_username'] ?? '');
    const initiales = nom.split(/\s+/).filter(Boolean).slice(0, 2).map((m) => m[0]!.toUpperCase()).join('');
    return { sub: String(c['sub'] ?? ''), nom, email: String(c['email'] ?? ''), initiales: initiales || '?' };
  });

  /** Appelé une fois au démarrage : traite le retour de Keycloak (?code=...) puis lance le renouvellement. */
  async init(): Promise<void> {
    this.oauth.configure(authConfig);
    this.oauth.events.subscribe(() => this.synchroniser());
    // Renouvellement impossible (session Keycloak expirée) : on oublie les tokens, retour à l'accueil public.
    // Pas de nouvelle redirection automatique vers Keycloak : aucune boucle possible.
    this.oauth.events.pipe(filter((e) => e.type === 'token_refresh_error')).subscribe(() => {
      this.oauth.logOut(true);
      this.synchroniser();
      void this.router.navigateByUrl('/');
    });
    try {
      await this.oauth.loadDiscoveryDocumentAndTryLogin();
      if (this.oauth.hasValidAccessToken()) this.oauth.setupAutomaticSilentRefresh();
    } catch {
      // Keycloak injoignable : l'application reste utilisable en mode public (page d'accueil).
    }
    this.synchroniser();
  }

  token(): string | null {
    return this.oauth.hasValidAccessToken() ? this.oauth.getAccessToken() : null;
  }

  hasRole(role: Role): boolean {
    return this.roles().includes(role);
  }

  hasAnyRole(roles: readonly Role[]): boolean {
    return roles.length === 0 || roles.some((r) => this.hasRole(r));
  }

  login(retour = '/accueil'): void {
    this.oauth.initCodeFlow(retour);
  }

  /**
   * Après un 401 de l'API : relance la connexion, sauf si on en revient à l'instant (moins de 30 s).
   * Dans ce cas l'API refuse un token tout neuf : relancer bouclerait à l'infini. Renvoie false.
   */
  reconnecter(retour: string): boolean {
    const CLE = 'rescueops.relogin';
    try {
      const dernier = Number(sessionStorage.getItem(CLE) ?? 0);
      if (Date.now() - dernier < 30_000) return false;
      sessionStorage.setItem(CLE, String(Date.now()));
    } catch {
      /* stockage indisponible : on tente quand même une seule fois */
    }
    this.login(retour);
    return true;
  }

  /** Ouvre directement la page d'inscription de Keycloak (OIDC prompt=create). */
  register(): void {
    this.oauth.initCodeFlow('/accueil', { prompt: 'create' });
  }

  /** Redemande un token (ex. après l'attribution d'un rôle par un ADMIN : il apparaît sans se reconnecter). */
  async actualiserRoles(): Promise<void> {
    try {
      await this.oauth.refreshToken();
    } finally {
      this.synchroniser();
    }
  }

  logout(): void {
    this.oauth.logOut();
  }

  /** Chemin demandé avant la connexion (transmis par Keycloak dans le paramètre state). */
  retourApresConnexion(): string | null {
    const s = this.oauth.state ? decodeURIComponent(this.oauth.state) : '';
    return s.startsWith('/') && !s.startsWith('//') ? s : null;
  }

  private synchroniser(): void {
    this.accessToken.set(this.token());
  }
}
