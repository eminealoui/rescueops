import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DispatchApi } from './dispatch.api';

describe('DispatchApi', () => {
  let api: DispatchApi;
  let ctrl: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(DispatchApi);
    ctrl = TestBed.inject(HttpTestingController);
  });

  afterEach(() => ctrl.verify());

  it('calculerRecommandations poste les critères et convertit les scores 0–1 en 0–100', () => {
    const req = {
      incidentId: '11111111-1111-4111-8111-111111111111',
      latitudeIncident: 36.8065,
      longitudeIncident: 10.1815,
      typeRessource: 'AMBULANCE',
    };
    let scores: number[] = [];
    api.calculerRecommandations(req).subscribe((res) => (scores = res.map((r) => r.score)));
    const appel = ctrl.expectOne('/api/dispatch/recommandations/calculer');
    expect(appel.request.method).toBe('POST');
    expect(appel.request.body).toEqual(req);
    appel.flush([
      {
        id: 1,
        incidentId: req.incidentId,
        ressourceId: 'res-1',
        nomRessource: 'Ambulance A1',
        typeRessource: 'AMBULANCE',
        score: 0.87,
        justification: 'Score 0.87',
        distanceKm: 2.1,
        disponible: true,
        competencesOk: true,
        createdAt: '2026-01-01T00:00:00Z',
      },
    ]);
    expect(scores).toEqual([87]);
  });

  it('getRecommandations appelle la ressource de l’incident', () => {
    api.getRecommandations('inc-1').subscribe();
    const appel = ctrl.expectOne('/api/dispatch/recommandations/inc-1');
    expect(appel.request.method).toBe('GET');
    appel.flush([]);
  });

  it('creerAffectation envoie le score en 0–1 et convertit la réponse en 0–100', () => {
    let scoreAffiche: number | null = null;
    api
      .creerAffectation({ incidentId: 'inc-1', ressourceId: 'res-1', score: 87, typeRessource: 'AMBULANCE' })
      .subscribe((aff) => (scoreAffiche = aff.score));
    const appel = ctrl.expectOne('/api/dispatch/affectations');
    expect(appel.request.method).toBe('POST');
    expect(appel.request.body).toEqual({
      incidentId: 'inc-1',
      ressourceId: 'res-1',
      typeRessource: 'AMBULANCE',
      score: 0.87,
    });
    appel.flush({ id: 12, statut: 'PROPOSEE', score: 0.87 });
    expect(scoreAffiche).toBe(87);
  });

  it('listerAffectations envoie le statut quand il est fourni', () => {
    api.listerAffectations('CONFIRMEE').subscribe();
    const appel = ctrl.expectOne((r) => r.url === '/api/dispatch/affectations');
    expect(appel.request.method).toBe('GET');
    expect(appel.request.params.get('statut')).toBe('CONFIRMEE');
    appel.flush([]);
  });

  it('listerAffectations sans filtre n’envoie aucun paramètre', () => {
    api.listerAffectations().subscribe();
    const appel = ctrl.expectOne((r) => r.url === '/api/dispatch/affectations');
    expect(appel.request.params.keys()).toEqual([]);
    appel.flush([]);
  });

  it('getAffectation et getAffectationsParIncident ciblent les bonnes URLs', () => {
    api.getAffectation('12').subscribe();
    api.getAffectationsParIncident('inc-1').subscribe();
    const parId = ctrl.expectOne('/api/dispatch/affectations/12');
    const parIncident = ctrl.expectOne('/api/dispatch/affectations/incident/inc-1');
    expect(parId.request.method).toBe('GET');
    expect(parIncident.request.method).toBe('GET');
    parId.flush({});
    parIncident.flush([]);
  });

  it('confirmerAffectation envoie un PATCH sans corps', () => {
    api.confirmerAffectation('12').subscribe();
    const appel = ctrl.expectOne('/api/dispatch/affectations/12/confirmer');
    expect(appel.request.method).toBe('PATCH');
    expect(appel.request.body).toBeNull();
    appel.flush({});
  });

  it('annulerAffectation renvoie la nouvelle proposition BF-09', () => {
    let idAlternative: string | number | undefined;
    api.annulerAffectation('12').subscribe((alt) => (idAlternative = alt?.id));
    const appel = ctrl.expectOne('/api/dispatch/affectations/12/annuler');
    expect(appel.request.method).toBe('PATCH');
    expect(appel.request.body).toBeNull();
    appel.flush({ id: 13, statut: 'PROPOSEE' });
    expect(idAlternative).toBe(13);
  });

  it('annulerAffectation sans alternative (200 sans corps) renvoie null', () => {
    let recu: unknown = 'non appelé';
    api.annulerAffectation('12').subscribe((alt) => (recu = alt));
    ctrl.expectOne('/api/dispatch/affectations/12/annuler').flush(null);
    expect(recu).toBeNull();
  });
});
