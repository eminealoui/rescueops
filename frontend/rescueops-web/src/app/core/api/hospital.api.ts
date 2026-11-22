import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { ERREUR_GEREE } from '../http/error.interceptor';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import {
  Candidat, CreerTransfert, EtatService, EvenementFlux, Gravite, Hopital, HopitalRequest, ServiceHospitalier, Specialite,
  StatutTransfert, TableauDeBord, Transfert,
} from './models';

/** Client typé de ms-hospital : établissements et capacités (/api/hospitals/**). */
@Injectable({ providedIn: 'root' })
export class HospitalApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBase}/hospitals`;

  capacites(f: { ville?: string | null; specialite?: Specialite | null; etat?: EtatService | null } = {}) {
    let params = new HttpParams();
    if (f.ville) params = params.set('ville', f.ville);
    if (f.specialite) params = params.set('specialite', f.specialite);
    if (f.etat) params = params.set('etat', f.etat);
    return this.http.get<TableauDeBord>(`${this.base}/capacities`, { params });
  }

  compatibles(specialite: Specialite, gravite: Gravite, lat: number | null, lon: number | null) {
    let params = new HttpParams().set('specialite', specialite).set('gravite', gravite);
    if (lat !== null && lon !== null) params = params.set('lat', lat).set('lon', lon);
    return this.http.get<Candidat[]>(`${this.base}/compatible`, { params });
  }

  hopitaux() {
    return this.http.get<Hopital[]>(this.base);
  }

  creerHopital(req: HopitalRequest) {
    return this.http.post<Hopital>(this.base, req);
  }

  modifierHopital(id: string, req: HopitalRequest) {
    return this.http.put<Hopital>(`${this.base}/${id}`, req);
  }

  supprimerHopital(id: string) {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  ajouterService(hopitalId: string, specialite: Specialite, capaciteTotale: number) {
    return this.http.post<ServiceHospitalier>(`${this.base}/${hopitalId}/services`, { specialite, capaciteTotale });
  }

  changerEtatService(serviceId: string, actif: boolean) {
    return this.http.put<ServiceHospitalier>(`${this.base}/services/${serviceId}`, { actif });
  }

  ajusterCapacite(serviceId: string, capaciteTotale: number, placesDisponibles: number, motif: string | null) {
    return this.http.patch<ServiceHospitalier>(`${this.base}/services/${serviceId}/capacity`, { capaciteTotale, placesDisponibles, motif });
  }
}

/** Client typé de ms-hospital : demandes de transfert (/api/transfers/**). */
@Injectable({ providedIn: 'root' })
export class TransferApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBase}/transfers`;

  lister(statut: StatutTransfert | null, page = 0, size = 50) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (statut) params = params.set('statut', statut);
    return this.http.get<Transfert[]>(this.base, { params });
  }

  detail(id: string) {
    return this.http.get<Transfert>(`${this.base}/${id}`);
  }

  creer(req: CreerTransfert) {
    return this.http.post<Transfert>(this.base, req);
  }

  reserver(id: string, serviceId: string) {
    return this.http.post<Transfert>(`${this.base}/${id}/reserve`, { serviceId });
  }

  confirmer(id: string) {
    return this.http.post<Transfert>(`${this.base}/${id}/confirm`, null);
  }

  refuser(id: string, motif: string | null) {
    return this.http.post<Transfert>(`${this.base}/${id}/refuse`, { motif });
  }

  admettre(id: string) {
    return this.http.post<Transfert>(`${this.base}/${id}/admit`, null);
  }

  sortie(id: string) {
    return this.http.post<Transfert>(`${this.base}/${id}/discharge`, null);
  }

  annuler(id: string) {
    return this.http.post<Transfert>(`${this.base}/${id}/cancel`, null);
  }

  flux(since?: string) {
    const params = since ? new HttpParams().set('since', since) : undefined;
    // Appel de fond (bandeau) : jamais de toast ni de redirection en cas d'erreur.
    return this.http.get<EvenementFlux[]>(`${this.base}/feed`, { params, context: new HttpContext().set(ERREUR_GEREE, true) });
  }
}
