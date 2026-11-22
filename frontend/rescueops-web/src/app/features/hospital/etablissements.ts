import { Dialog } from '@angular/cdk/dialog';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Observable, filter, switchMap } from 'rxjs';
import { Hopital, HopitalRequest, LIBELLES_SPECIALITE, ServiceHospitalier, Specialite } from '../../core/api/models';
import { HospitalApi } from '../../core/api/hospital.api';
import { Confirm } from '../../core/ui/confirm';
import { ToastService } from '../../core/ui/toast.service';
import { CapaciteDialog, CapaciteSaisie, HopitalDialog, ServiceDialog } from './etablissement-form';
import { ReferentielHopitaux } from './referentiel.service';

/** Établissements (coordinateur) : hôpitaux, services, ouverture/fermeture, ajustement des capacités. */
@Component({
  selector: 'app-etablissements',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="entete">
      <div>
        <div class="eyebrow">Référentiel</div>
        <h1 class="page-title"><em>Établissements</em> et services</h1>
        <p class="page-sub">Portée régionale : le coordinateur gère tous les établissements.</p>
      </div>
      <button type="button" class="btn btn-primary" (click)="creer()">Nouvel établissement <span class="arr">→</span></button>
    </div>

    <div class="liste">
      @for (h of ref.hopitaux(); track h.id) {
        <article class="card hop" [class.ferme]="!h.actif">
          <div class="haut">
            <div>
              <h2>{{ h.nom }}</h2>
              <div class="meta">{{ h.ville }}@if (h.adresse) { · {{ h.adresse }}}@if (h.telephone) { · <span class="mono">{{ h.telephone }}</span>}</div>
            </div>
            <div class="outils">
              <button type="button" class="btn btn-outline mini" (click)="modifier(h)">Modifier</button>
              <button type="button" class="btn btn-outline mini" (click)="ajouterService(h)">+ Service</button>
              <button type="button" class="btn btn-outline mini danger" (click)="supprimer(h)" aria-label="Supprimer l'établissement">Supprimer</button>
            </div>
          </div>
          <div class="services">
            @for (s of h.services; track s.id) {
              <div class="svc" [class.inactif]="!s.actif">
                <span class="nom">{{ libelles[s.specialite] }}</span>
                <span class="mono places"><b>{{ s.placesDisponibles }}</b> / {{ s.capaciteTotale }}</span>
                @if (!s.actif) { <span class="tag tag-danger">FERMÉ</span> }
                <span class="svc-actions">
                  <button type="button" class="lien" (click)="ajuster(s)">Ajuster</button>
                  <button type="button" class="lien" (click)="basculer(s)">{{ s.actif ? 'Fermer' : 'Rouvrir' }}</button>
                </span>
              </div>
            } @empty {
              <span class="meta">Aucun service.</span>
            }
          </div>
        </article>
      } @empty {
        <div class="card vide">Aucun établissement.</div>
      }
    </div>
  `,
  styles: `
    .entete { display: flex; justify-content: space-between; align-items: flex-end; gap: 12px; flex-wrap: wrap; margin-bottom: 18px; }
    .liste { display: grid; gap: 14px; }
    .hop:hover { transform: none; }
    .hop.ferme { opacity: .6; }
    .haut { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap; }
    h2 { font-size: 17px; font-weight: 600; }
    .meta { font-size: 12.5px; color: var(--text-2); }
    .outils { display: flex; gap: 6px; flex-wrap: wrap; }
    .mini { padding: 6px 12px; font-size: 12px; }
    .danger { color: var(--danger); border-color: var(--danger); }
    .services { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 8px; margin-top: 14px; }
    .svc { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; padding: 10px 12px; border: 1px solid var(--line); border-radius: var(--radius-sm); background: var(--surface-2); }
    .svc.inactif { opacity: .7; }
    .nom { font-weight: 500; flex: 1; }
    .places { font-size: 12px; color: var(--text-2); }
    .places b { color: var(--text); font-size: 14px; }
    .svc-actions { display: flex; gap: 10px; width: 100%; }
    .lien { border: 0; background: none; padding: 0; color: var(--brand); font-size: 12px; font-weight: 600; cursor: pointer; }
    .vide { text-align: center; color: var(--text-3); }
  `,
})
export class Etablissements {
  private readonly api = inject(HospitalApi);
  private readonly dialog = inject(Dialog);
  private readonly confirm = inject(Confirm);
  private readonly toast = inject(ToastService);
  protected readonly ref = inject(ReferentielHopitaux);
  protected readonly libelles = LIBELLES_SPECIALITE;

  constructor() {
    this.ref.charger(true);
  }

  protected creer(): void {
    this.ouvrir<HopitalRequest>(HopitalDialog, null).pipe(switchMap((req) => this.api.creerHopital(req)))
      .subscribe(() => this.fait('Établissement créé'));
  }

  protected modifier(h: Hopital): void {
    this.ouvrir<HopitalRequest>(HopitalDialog, h).pipe(switchMap((req) => this.api.modifierHopital(h.id, req)))
      .subscribe(() => this.fait('Établissement modifié'));
  }

  protected supprimer(h: Hopital): void {
    this.confirm
      .demander({ titre: `Supprimer ${h.nom} ?`, texte: 'Refusé par le serveur si des transferts sont en cours dans cet établissement.', action: 'Supprimer', danger: true })
      .pipe(filter((r) => r !== null), switchMap(() => this.api.supprimerHopital(h.id)))
      .subscribe(() => this.fait('Établissement supprimé'));
  }

  protected ajouterService(h: Hopital): void {
    this.ouvrir<{ specialite: Specialite; capaciteTotale: number }>(ServiceDialog, null)
      .pipe(switchMap((s) => this.api.ajouterService(h.id, s.specialite, s.capaciteTotale)))
      .subscribe(() => this.fait('Service ajouté'));
  }

  protected ajuster(s: ServiceHospitalier): void {
    this.ouvrir<CapaciteSaisie>(CapaciteDialog, s)
      .pipe(switchMap((c) => this.api.ajusterCapacite(s.id, c.capaciteTotale, c.placesDisponibles, c.motif)))
      .subscribe(() => this.fait('Capacité ajustée'));
  }

  protected basculer(s: ServiceHospitalier): void {
    this.api.changerEtatService(s.id, !s.actif).subscribe(() => this.fait(s.actif ? 'Service fermé' : 'Service rouvert'));
  }

  private ouvrir<R>(composant: Parameters<Dialog['open']>[0], data: unknown): Observable<R> {
    return this.dialog.open<R>(composant, { data, backdropClass: 'dlg-fond' }).closed.pipe(filter((r): r is R => r !== undefined && r !== null));
  }

  private fait(message: string): void {
    this.toast.ok(message);
    this.ref.charger(true);
  }
}
