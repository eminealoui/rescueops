import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { UserApi } from './user.api';

describe('UserApi', () => {
  let api: UserApi;
  let ctrl: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(UserApi);
    ctrl = TestBed.inject(HttpTestingController);
  });

  afterEach(() => ctrl.verify());

  it('rechercher envoie page, taille, tri, la recherche nettoyée et le filtre actif', () => {
    api.rechercher({ query: '  ben ', active: false, page: 2, size: 20, sort: 'email,desc' }).subscribe();
    const req = ctrl.expectOne((r) => r.url === '/api/users');
    const p = req.request.params;
    expect([p.get('page'), p.get('size'), p.get('sort'), p.get('query'), p.get('active')]).toEqual(['2', '20', 'email,desc', 'ben', 'false']);
    req.flush({ content: [], page: 2, size: 20, totalElements: 0, totalPages: 0 });
  });

  it("rechercher n'envoie ni query vide ni filtre « tous »", () => {
    api.rechercher({ query: '   ', active: null, page: 0, size: 20, sort: 'lastName,asc' }).subscribe();
    const req = ctrl.expectOne((r) => r.url === '/api/users');
    expect(req.request.params.has('query')).toBe(false);
    expect(req.request.params.has('active')).toBe(false);
    req.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('refuser envoie le motif dans le corps', () => {
    api.refuser('42', 'Pièce justificative manquante').subscribe();
    const req = ctrl.expectOne('/api/users/role-requests/42/reject');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ motif: 'Pièce justificative manquante' });
    req.flush({});
  });
});
