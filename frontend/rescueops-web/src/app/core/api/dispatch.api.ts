import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AffectationRequest,
  AffectationResponse,
  RecommandationRequest,
  RecommandationResponse,
  StatutAffectation,
} from './models';

/** Représentation brute de ms-dispatch : les scores y sont en 0–1 (voir ScoringService). */
interface AffectationBrute extends Omit<AffectationResponse, 'score'> {
  score: number | null;
}
interface RecommandationBrute extends Omit<RecommandationResponse, 'score'> {
  score: number;
}

/** Client typé de ms-dispatch : affectations et recommandations (/api/dispatch/**). */
@Injectable({ providedIn: 'root' })
export class DispatchApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBase}/dispatch`;

  // ── Recommandations (scoring BF-07) ───────────────────────────────────────

  getRecommandations(incidentId: string) {
    return this.http
      .get<RecommandationBrute[]>(`${this.base}/recommandations/${incidentId}`)
      .pipe(map((liste) => liste.map(this.recommandationDepuisApi)));
  }

  calculerRecommandations(req: RecommandationRequest) {
    return this.http
      .post<RecommandationBrute[]>(`${this.base}/recommandations/calculer`, req)
      .pipe(map((liste) => liste.map(this.recommandationDepuisApi)));
  }

  // ── Affectations (BF-08 / BF-09) ─────────────────────────────────────────

  creerAffectation(req: AffectationRequest) {
    const corps: Record<string, unknown> = {
      incidentId: req.incidentId,
      ressourceId: req.ressourceId,
      typeRessource: req.typeRessource ?? null,
    };
    // L'UI raisonne en 0–100, le contrat Java attend 0–1.
    if (req.score !== undefined) corps['score'] = req.score / 100;
    return this.http.post<AffectationBrute>(`${this.base}/affectations`, corps).pipe(map(this.affectationDepuisApi));
  }

  listerAffectations(statut?: StatutAffectation | null) {
    let params = new HttpParams();
    if (statut) params = params.set('statut', statut);
    return this.http
      .get<AffectationBrute[]>(`${this.base}/affectations`, { params })
      .pipe(map((liste) => liste.map(this.affectationDepuisApi)));
  }

  getAffectation(id: string) {
    return this.http.get<AffectationBrute>(`${this.base}/affectations/${id}`).pipe(map(this.affectationDepuisApi));
  }

  getAffectationsParIncident(incidentId: string) {
    return this.http
      .get<AffectationBrute[]>(`${this.base}/affectations/incident/${incidentId}`)
      .pipe(map((liste) => liste.map(this.affectationDepuisApi)));
  }

  confirmerAffectation(id: string) {
    return this.http.patch<AffectationBrute>(`${this.base}/affectations/${id}/confirmer`, null).pipe(map(this.affectationDepuisApi));
  }

  /**
   * BF-09 : le backend annule l'affectation et renvoie une NOUVELLE proposition (ou 200 sans corps
   * s'il n'y a pas d'alternative). Le composant doit donc recharger la liste, pas remplacer la ligne.
   */
  annulerAffectation(id: string) {
    return this.http
      .patch<AffectationBrute | null>(`${this.base}/affectations/${id}/annuler`, null)
      .pipe(map((rep) => (rep ? this.affectationDepuisApi(rep) : null)));
  }

  private readonly affectationDepuisApi = (a: AffectationBrute): AffectationResponse => ({
    ...a,
    score: a.score === null || a.score === undefined ? null : Math.round(a.score * 100),
  });

  private readonly recommandationDepuisApi = (r: RecommandationBrute): RecommandationResponse => ({
    ...r,
    score: Math.round(r.score * 100),
    disponible: Boolean(r.disponible),
    competencesOk: Boolean(r.competencesOk),
  });
}
