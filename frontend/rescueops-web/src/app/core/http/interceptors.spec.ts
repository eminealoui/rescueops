import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { ToastService } from '../ui/toast.service';
import { apiInterceptor } from './api.interceptor';
import { ERREUR_GEREE, MESSAGE_INDISPONIBLE, errorInterceptor } from './error.interceptor';
import { HttpContext } from '@angular/common/http';

describe('intercepteurs HTTP', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;
  let toast: ToastService;
  const auth = { token: vi.fn(() => 'TOKEN'), reconnecter: vi.fn(() => true) };
  const router = { url: '/hopitaux', navigateByUrl: vi.fn() };

  beforeEach(() => {
    auth.token.mockClear();
    auth.reconnecter.mockClear();
    router.navigateByUrl.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiInterceptor, errorInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
      ],
    });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService);
  });

  afterEach(() => ctrl.verify());

  it('ajoute le Bearer et un X-Correlation-Id vers /api', () => {
    http.get('/api/users/me').subscribe();
    const req = ctrl.expectOne('/api/users/me');
    expect(req.request.headers.get('Authorization')).toBe('Bearer TOKEN');
    expect(req.request.headers.get('X-Correlation-Id')).toMatch(/^[0-9a-f-]{36}$/);
    req.flush({});
  });

  it("n'envoie JAMAIS le token vers une autre origine (tuiles de carte, polices)", () => {
    http.get('https://tile.openstreetmap.org/1/1/1.png').subscribe();
    http.get('/apiculture').subscribe();
    const tuile = ctrl.expectOne((r) => r.url.startsWith('https://tile.openstreetmap.org'));
    const piege = ctrl.expectOne('/apiculture');
    expect(tuile.request.headers.has('Authorization')).toBe(false);
    expect(piege.request.headers.has('Authorization')).toBe(false);
    tuile.flush('');
    piege.flush('');
  });

  it('409 ProblemDetail : toast avec le detail renvoyé par l\'API', () => {
    http.post('/api/transfers/1/confirm', {}).subscribe({ error: () => {} });
    ctrl.expectOne('/api/transfers/1/confirm').flush(
      { status: 409, detail: 'Cette demande a déjà été traitée.', code: 'TRANSITION_INVALIDE' },
      { status: 409, statusText: 'Conflict' },
    );
    expect(toast.toasts().map((t) => t.text)).toEqual(['Cette demande a déjà été traitée.']);
  });

  it('503 : toast « Service momentanément indisponible »', () => {
    http.get('/api/hospitals').subscribe({ error: () => {} });
    ctrl.expectOne('/api/hospitals').flush(null, { status: 503, statusText: 'Service Unavailable' });
    expect(toast.toasts()[0]?.text).toBe(MESSAGE_INDISPONIBLE);
  });

  it('Gateway injoignable (statut 0) : même message « indisponible »', () => {
    http.get('/api/hospitals').subscribe({ error: () => {} });
    ctrl.expectOne('/api/hospitals').error(new ProgressEvent('error'));
    expect(toast.toasts()[0]?.text).toBe(MESSAGE_INDISPONIBLE);
  });

  it('401 : relance la connexion en revenant sur la page courante', () => {
    http.get('/api/users/me').subscribe({ error: () => {} });
    ctrl.expectOne('/api/users/me').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(auth.reconnecter).toHaveBeenCalledWith('/hopitaux');
  });

  it('401 juste après une connexion : pas de nouvelle redirection (pas de boucle), un toast', () => {
    auth.reconnecter.mockReturnValueOnce(false);
    http.get('/api/users/me').subscribe({ error: () => {} });
    ctrl.expectOne('/api/users/me').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(toast.toasts()).toHaveLength(1);
  });

  it('403 : page « Accès refusé »', () => {
    http.get('/api/users').subscribe({ error: () => {} });
    ctrl.expectOne('/api/users').flush(null, { status: 403, statusText: 'Forbidden' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/acces-refuse');
  });

  it("403 sur une action (écriture) : toast avec le detail, on reste sur la page", () => {
    http.post('/api/transfers/1/cancel', null).subscribe({ error: () => {} });
    ctrl.expectOne('/api/transfers/1/cancel').flush(
      { status: 403, detail: "Seul le demandeur peut annuler cette demande." },
      { status: 403, statusText: 'Forbidden' },
    );
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(toast.toasts()[0]?.text).toBe('Seul le demandeur peut annuler cette demande.');
  });

  it('un appel marqué ERREUR_GEREE ne déclenche pas de toast', () => {
    http.get('/api/users/me', { context: new HttpContext().set(ERREUR_GEREE, true) }).subscribe({ error: () => {} });
    ctrl.expectOne('/api/users/me').flush({ detail: 'x' }, { status: 409, statusText: 'Conflict' });
    expect(toast.toasts()).toHaveLength(0);
  });
});
