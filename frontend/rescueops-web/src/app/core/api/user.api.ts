import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { DemandeRole, NouvelleDemandeRole, Page, StatutDemandeRole, UpdateMyProfile, UserProfile } from './models';

export interface RechercheUtilisateurs {
  query?: string;
  active?: boolean | null;
  page: number;
  size: number;
  sort: string;
}

/** Client typé de ms-user (via la Gateway : /api/users/**). */
@Injectable({ providedIn: 'root' })
export class UserApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBase}/users`;

  moi() {
    return this.http.get<UserProfile>(`${this.base}/me`);
  }

  modifierMoi(req: UpdateMyProfile) {
    return this.http.put<UserProfile>(`${this.base}/me`, req);
  }

  rechercher(r: RechercheUtilisateurs) {
    let params = new HttpParams().set('page', r.page).set('size', r.size).set('sort', r.sort);
    if (r.query?.trim()) params = params.set('query', r.query.trim());
    if (r.active !== undefined && r.active !== null) params = params.set('active', r.active);
    return this.http.get<Page<UserProfile>>(this.base, { params });
  }

  changerStatut(keycloakId: string, active: boolean) {
    return this.http.patch<UserProfile>(`${this.base}/${encodeURIComponent(keycloakId)}/status`, { active });
  }

  // ----- demandes de rôle -----
  demanderRole(req: NouvelleDemandeRole) {
    return this.http.post<DemandeRole>(`${this.base}/me/role-requests`, req);
  }

  mesDemandes() {
    return this.http.get<DemandeRole[]>(`${this.base}/me/role-requests`);
  }

  demandes(statut: StatutDemandeRole | null, page = 0, size = 20) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (statut) params = params.set('statut', statut);
    return this.http.get<Page<DemandeRole>>(`${this.base}/role-requests`, { params });
  }

  approuver(id: string) {
    return this.http.post<DemandeRole>(`${this.base}/role-requests/${id}/approve`, null);
  }

  refuser(id: string, motif: string | null) {
    return this.http.post<DemandeRole>(`${this.base}/role-requests/${id}/reject`, { motif });
  }
}
