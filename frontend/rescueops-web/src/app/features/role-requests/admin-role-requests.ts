import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { filter, finalize, switchMap, tap } from 'rxjs';
import { DemandeRole, Page, StatutDemandeRole } from '../../core/api/models';
import { UserApi } from '../../core/api/user.api';
import { LIBELLES_ROLE } from '../../core/auth/roles';
import { Confirm } from '../../core/ui/confirm';
import { ToastService } from '../../core/ui/toast.service';

/** Demandes de rôle professionnel à traiter par un ADMIN (accorder dans Keycloak, ou refuser avec un motif). */
@Component({
  selector: 'app-admin-role-requests',
  imports: [DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="eyebrow">Administration</div>
    <h1 class="page-title">Demandes de <em>rôle</em></h1>
    <p class="page-sub">L'inscription donne le rôle Citoyen. Les rôles professionnels sont accordés ici, après vérification.</p>

    <div class="chips" role="group" aria-label="Filtrer par statut">
      @for (f of filtres; track f.libelle) {
        <button type="button" class="chip" [class.on]="statut() === f.statut" (click)="choisir(f.statut)">{{ f.libelle }}</button>
      }
    </div>

    <div class="liste">
      @for (d of page()?.content ?? []; track d.id) {
        <article class="card demande">
          <div class="haut">
            <span class="role">{{ libelles[d.roleDemande] }}</span>
            <span class="tag" [class.tag-warn]="d.statut === 'EN_ATTENTE'" [class.tag-ok]="d.statut === 'ACCEPTEE'" [class.tag-danger]="d.statut === 'REFUSEE'">
              {{ d.statut === 'EN_ATTENTE' ? 'EN ATTENTE' : d.statut === 'ACCEPTEE' ? 'ACCEPTÉE' : 'REFUSÉE' }}
            </span>
          </div>
          <div class="org">{{ d.organisation }}</div>
          @if (d.motif) { <p class="motif">« {{ d.motif }} »</p> }
          <div class="meta mono">Utilisateur {{ d.keycloakId.slice(0, 8) }} · {{ d.createdAt | date: 'dd/MM/yyyy HH:mm' }}</div>
          @if (d.motifRefus) { <p class="motif">Motif du refus : {{ d.motifRefus }}</p> }
          @if (d.statut === 'EN_ATTENTE') {
            <div class="actions">
              <button type="button" class="btn btn-outline" [disabled]="enCours() === d.id" (click)="refuser(d)">Refuser</button>
              <button type="button" class="btn btn-primary" [disabled]="enCours() === d.id" (click)="approuver(d)">Accorder le rôle</button>
            </div>
          }
        </article>
      } @empty {
        <div class="card vide">{{ chargement() ? 'Chargement…' : 'Aucune demande dans cette catégorie.' }}</div>
      }
    </div>
  `,
  styles: `
    .chips { display: flex; gap: 6px; margin: 20px 0 14px; flex-wrap: wrap; }
    .liste { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 14px; }
    .demande { display: grid; gap: 6px; align-content: start; }
    .haut { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
    .role { font-weight: 600; font-size: 16px; }
    .org { color: var(--brand); font-weight: 500; }
    .motif { color: var(--text-2); font-size: 13px; }
    .meta { font-size: 11px; color: var(--text-3); }
    .actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 8px; }
    .vide { color: var(--text-3); text-align: center; grid-column: 1 / -1; }
    .vide:hover { transform: none; }
  `,
})
export class AdminRoleRequests {
  private readonly api = inject(UserApi);
  private readonly confirm = inject(Confirm);
  private readonly toast = inject(ToastService);
  protected readonly libelles = LIBELLES_ROLE;
  protected readonly filtres: { libelle: string; statut: StatutDemandeRole | null }[] = [
    { libelle: 'En attente', statut: 'EN_ATTENTE' },
    { libelle: 'Acceptées', statut: 'ACCEPTEE' },
    { libelle: 'Refusées', statut: 'REFUSEE' },
    { libelle: 'Toutes', statut: null },
  ];

  protected readonly statut = signal<StatutDemandeRole | null>('EN_ATTENTE');
  protected readonly page = signal<Page<DemandeRole> | null>(null);
  protected readonly chargement = signal(false);
  protected readonly enCours = signal<string | null>(null);

  constructor() {
    this.charger();
  }

  protected choisir(s: StatutDemandeRole | null): void {
    this.statut.set(s);
    this.charger();
  }

  protected approuver(d: DemandeRole): void {
    this.traiter(d, {
      titre: `Accorder le rôle ${this.libelles[d.roleDemande]} ?`,
      texte: `Organisation déclarée : ${d.organisation}. Le rôle est ajouté au compte dans Keycloak.`,
      action: 'Accorder',
    }, () => this.api.approuver(d.id), 'Rôle accordé');
  }

  protected refuser(d: DemandeRole): void {
    this.traiter(d, {
      titre: 'Refuser cette demande ?',
      texte: `Rôle ${this.libelles[d.roleDemande]} demandé pour ${d.organisation}.`,
      action: 'Refuser',
      danger: true,
      champ: 'Motif (visible par le demandeur, facultatif)',
    }, (motif) => this.api.refuser(d.id, motif || null), 'Demande refusée');
  }

  private traiter(d: DemandeRole, dialogue: Parameters<Confirm['demander']>[0],
                  appel: (saisie: string) => ReturnType<UserApi['approuver']>, succes: string): void {
    this.confirm
      .demander(dialogue)
      .pipe(
        filter((r): r is string => r !== null),
        tap(() => this.enCours.set(d.id)),
        switchMap((saisie) => appel(saisie).pipe(finalize(() => this.enCours.set(null)))),
      )
      .subscribe({
        next: () => {
          this.toast.ok(succes);
          this.charger();
        },
        // 409 DEMANDE_DEJA_TRAITEE (traitée par un autre admin) : on recharge la liste à jour.
        error: () => this.charger(),
      });
  }

  private charger(): void {
    this.chargement.set(true);
    this.api
      .demandes(this.statut())
      .pipe(finalize(() => this.chargement.set(false)))
      .subscribe((p) => this.page.set(p));
  }
}
