import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EMPTY, Subject, catchError, debounceTime, distinctUntilChanged, filter, switchMap, tap } from 'rxjs';
import { Page, UserProfile } from '../../core/api/models';
import { RechercheUtilisateurs, UserApi } from '../../core/api/user.api';
import { Confirm } from '../../core/ui/confirm';
import { ToastService } from '../../core/ui/toast.service';

type ChampTri = 'lastName' | 'firstName' | 'email' | 'createdAt';
export const TAILLE_PAGE = 20;

/** Administration des utilisateurs (ADMIN) : recherche, filtre, tri, pagination, activation. */
@Component({
  selector: 'app-admin-users',
  imports: [ReactiveFormsModule, DatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.scss',
})
export class AdminUsers {
  private readonly api = inject(UserApi);
  private readonly confirm = inject(Confirm);
  private readonly toast = inject(ToastService);

  protected readonly recherche = new FormControl('', { nonNullable: true });
  protected readonly etat = signal<RechercheUtilisateurs>({ query: '', active: null, page: 0, size: TAILLE_PAGE, sort: 'lastName,asc' });
  protected readonly resultat = signal<Page<UserProfile> | null>(null);
  protected readonly chargement = signal(false);
  protected readonly enCours = signal<string | null>(null);
  private readonly demandes = new Subject<RechercheUtilisateurs>();

  constructor() {
    const destroy = inject(DestroyRef);
    // switchMap : seule la dernière recherche compte (une réponse lente ne remplace pas une plus récente).
    this.demandes
      .pipe(
        tap(() => this.chargement.set(true)),
        // Une recherche en échec (toast affiché par l'intercepteur) n'arrête pas les suivantes.
        switchMap((r) => this.api.rechercher(r).pipe(catchError(() => (this.chargement.set(false), EMPTY)))),
        takeUntilDestroyed(destroy),
      )
      .subscribe((p) => {
        this.resultat.set(p);
        this.chargement.set(false);
      });
    this.recherche.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), filter((q) => q.length !== 1), takeUntilDestroyed(destroy))
      .subscribe((query) => this.modifier({ query, page: 0 }));
    this.lancer();
  }

  protected filtrer(active: boolean | null): void {
    this.modifier({ active, page: 0 });
  }

  protected trier(champ: ChampTri): void {
    const [actuel, sens] = this.etat().sort.split(',');
    this.modifier({ sort: `${champ},${actuel === champ && sens === 'asc' ? 'desc' : 'asc'}`, page: 0 });
  }

  protected tri(champ: ChampTri): 'ascending' | 'descending' | 'none' {
    const [actuel, sens] = this.etat().sort.split(',');
    return actuel !== champ ? 'none' : sens === 'asc' ? 'ascending' : 'descending';
  }

  protected allerA(page: number): void {
    this.modifier({ page });
  }

  protected basculer(u: UserProfile): void {
    const activer = !u.active;
    this.confirm
      .demander({
        titre: activer ? 'Réactiver ce compte ?' : 'Désactiver ce compte ?',
        texte: `${u.firstName} ${u.lastName} (${u.email}) ${activer ? 'pourra de nouveau utiliser RescueOps.' : 'ne pourra plus utiliser les services RescueOps.'}`,
        action: activer ? 'Réactiver' : 'Désactiver',
        danger: !activer,
      })
      .pipe(
        filter((r) => r !== null),
        tap(() => this.enCours.set(u.keycloakId)),
        switchMap(() => this.api.changerStatut(u.keycloakId, activer)),
      )
      .subscribe({
        next: (maj) => {
          this.enCours.set(null);
          this.resultat.update((p) => p && { ...p, content: p.content.map((x) => (x.keycloakId === maj.keycloakId ? maj : x)) });
          this.toast.ok(activer ? 'Compte réactivé' : 'Compte désactivé');
        },
        error: () => this.enCours.set(null),
      });
  }

  private modifier(m: Partial<RechercheUtilisateurs>): void {
    this.etat.update((e) => ({ ...e, ...m }));
    this.lancer();
  }

  private lancer(): void {
    this.demandes.next(this.etat());
  }
}
