import { DestroyRef, Injectable, computed, effect, inject, signal } from '@angular/core';
import { AuthService } from '../core/auth/auth.service';
import { ROLES_OPERATIONNELS } from '../core/auth/roles';
import { LIBELLES_STATUT, EvenementFlux, codeCourt } from '../core/api/models';
import { TransferApi } from '../core/api/hospital.api';
import { TickerItem } from '../core/ui/ticker';

const PERIODE_MS = 15_000;

/**
 * Activité récente des transferts pour le bandeau EN DIRECT : interrogée toutes les 15 s,
 * seulement pour les rôles opérationnels (les autres n'appellent jamais l'API : pas de 403 en boucle),
 * et en pause quand l'onglet du navigateur est masqué.
 */
@Injectable({ providedIn: 'root' })
export class LiveFeed {
  private readonly api = inject(TransferApi);
  private readonly auth = inject(AuthService);
  private readonly evenements = signal<EvenementFlux[]>([]);
  private timer: ReturnType<typeof setInterval> | null = null;

  readonly items = computed<TickerItem[]>(() =>
    this.evenements().slice(0, 12).map((e) => ({
      code: codeCourt('TRF', e.transfertId),
      texte: `${LIBELLES_STATUT[e.statut].toLowerCase()}${e.hopitalNom ? ' · ' + e.hopitalNom : ''}`,
    })),
  );

  constructor() {
    effect(() => (this.auth.hasAnyRole(ROLES_OPERATIONNELS) ? this.demarrer() : this.arreter()));
    inject(DestroyRef).onDestroy(() => this.arreter());
  }

  /** Relecture immédiate (après une action de l'utilisateur). */
  rafraichir(): void {
    if (typeof document !== 'undefined' && document.hidden) return;
    // Erreurs silencieuses : le bandeau garde simplement ses dernières données.
    this.api.flux().subscribe({
      next: (e) => this.evenements.set(e),
      error: () => {},
    });
  }

  private demarrer(): void {
    if (this.timer) return;
    this.rafraichir();
    this.timer = setInterval(() => this.rafraichir(), PERIODE_MS);
  }

  private arreter(): void {
    if (this.timer) clearInterval(this.timer);
    this.timer = null;
  }
}

