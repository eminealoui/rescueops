import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { publicGuard, roleGuard } from './role.guard';
import { Role } from './roles';

describe('roleGuard', () => {
  let roles: Role[];
  let connecte: boolean;
  const auth = {
    isAuthenticated: () => connecte,
    hasAnyRole: (r: readonly Role[]) => r.length === 0 || r.some((x) => roles.includes(x)),
    login: vi.fn(),
    retourApresConnexion: () => null as string | null,
  };

  const lancer = (guard: ReturnType<typeof roleGuard>, url = '/hopitaux') =>
    TestBed.runInInjectionContext(() => guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot));

  beforeEach(() => {
    roles = ['CITOYEN'];
    connecte = true;
    auth.login.mockClear();
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: AuthService, useValue: auth }] });
  });

  it('laisse passer un utilisateur qui a l\'un des rôles demandés', () => {
    roles = ['AMBULANCIER'];
    expect(lancer(roleGuard('AMBULANCIER', 'OPERATEUR'))).toBe(true);
  });

  it('rôle manquant : redirige vers /acces-refuse', () => {
    const r = lancer(roleGuard('ADMIN'));
    expect(r instanceof UrlTree && TestBed.inject(Router).serializeUrl(r)).toBe('/acces-refuse');
  });

  it('non connecté : lance la connexion en mémorisant la page demandée', () => {
    connecte = false;
    expect(lancer(roleGuard(), '/profil')).toBe(false);
    expect(auth.login).toHaveBeenCalledWith('/profil');
  });

  it('page publique : un utilisateur connecté va sur /accueil', () => {
    const r = TestBed.runInInjectionContext(() => publicGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot));
    expect(r instanceof UrlTree && TestBed.inject(Router).serializeUrl(r)).toBe('/accueil');
  });
});
