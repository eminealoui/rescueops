import { DestroyRef, Injectable, computed, effect, inject, signal } from '@angular/core';
import { environment } from '../../environments/environment';
import { AuthService } from '../core/auth/auth.service';
import { ROLES_OPERATIONNELS } from '../core/auth/roles';
import { LIBELLES_STATUT, EvenementFlux, codeCourt } from '../core/api/models';
import { TransferApi } from '../core/api/hospital.api';
import { TickerItem } from '../core/ui/ticker';

const PERIODE_MS = 15_000;
const NOUVEL_ESSAI_FLUX_MS = 30_000;

/**
 * Activité récente des transferts pour le bandeau EN DIRECT, seulement pour les rôles opérationnels
 * (les autres n'appellent jamais l'API : pas de 403 en boucle).
 *
 * Temps réel : flux Server-Sent Events (GET /api/transfers/feed/stream) lu avec fetch, car EventSource
 * ne sait pas envoyer le jeton dans l'en-tête Authorization. ms-hospital pousse la liste à chaque
 * changement de statut ; à la fin d'une connexion (10 min) on se reconnecte avec un jeton frais.
 * Secours : si le flux échoue, interrogation toutes les 15 s (en pause onglet masqué) et nouvel essai du flux 30 s plus tard.
 */
@Injectable({ providedIn: 'root' })
export class LiveFeed {
  private readonly api = inject(TransferApi);
  private readonly auth = inject(AuthService);
  private readonly evenements = signal<EvenementFlux[]>([]);
  private actif = false;
  private flux: AbortController | null = null;
  private timer: ReturnType<typeof setInterval> | null = null;
  private relance: ReturnType<typeof setTimeout> | null = null;

  /** Vrai quand le bandeau est alimenté en temps réel (et non par interrogation périodique). */
  readonly tempsReel = signal(false);

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
    if (this.actif) return;
    this.actif = true;
    void this.ouvrirFlux();
  }

  private arreter(): void {
    this.actif = false;
    this.flux?.abort();
    this.flux = null;
    this.arreterInterrogation();
    if (this.relance) clearTimeout(this.relance);
    this.relance = null;
    this.tempsReel.set(false);
  }

  private async ouvrirFlux(): Promise<void> {
    if (!this.actif || typeof fetch === 'undefined') return this.passerEnInterrogation();
    const token = this.auth.token();
    if (!token) return this.passerEnInterrogation();

    const controle = new AbortController();
    this.flux = controle;
    try {
      const reponse = await fetch(`${environment.apiBase}/transfers/feed/stream`, {
        headers: {
          Accept: 'text/event-stream',
          Authorization: `Bearer ${token}`,
          'X-Correlation-Id': crypto.randomUUID(),
        },
        signal: controle.signal,
      });
      if (!reponse.ok || !reponse.body) throw new Error(`flux ${reponse.status}`);

      this.arreterInterrogation();
      this.tempsReel.set(true);
      await this.lire(reponse.body);
      // Fin normale (connexion de 10 min écoulée) : reconnexion immédiate avec un jeton frais.
      if (this.actif) void this.ouvrirFlux();
    } catch {
      if (controle.signal.aborted) return;
      this.passerEnInterrogation();
    } finally {
      this.tempsReel.set(false);
    }
  }

  /** Lit le flux SSE : blocs séparés par une ligne vide, lignes « event: » et « data: ». */
  private async lire(corps: ReadableStream<Uint8Array>): Promise<void> {
    const lecteur = corps.getReader();
    const decodeur = new TextDecoder();
    let tampon = '';
    for (;;) {
      const { value, done } = await lecteur.read();
      if (done) return;
      tampon += decodeur.decode(value, { stream: true }).replace(/\r\n/g, '\n');
      let fin: number;
      while ((fin = tampon.indexOf('\n\n')) >= 0) {
        const bloc = tampon.slice(0, fin);
        tampon = tampon.slice(fin + 2);
        this.traiter(bloc);
      }
    }
  }

  private traiter(bloc: string): void {
    let evenement = 'message';
    const donnees: string[] = [];
    for (const ligne of bloc.split('\n')) {
      if (ligne.startsWith('event:')) evenement = ligne.slice(6).trim();
      else if (ligne.startsWith('data:')) donnees.push(ligne.slice(5).trimStart());
      // les lignes « : ping » (commentaires) servent seulement à garder la connexion ouverte
    }
    if (evenement !== 'flux' || donnees.length === 0) return;
    try {
      this.evenements.set(JSON.parse(donnees.join('\n')) as EvenementFlux[]);
    } catch {
      // bloc illisible ignoré : le suivant remettra la liste à jour
    }
  }

  /** Secours : interrogation périodique, et nouvel essai du flux temps réel un peu plus tard. */
  private passerEnInterrogation(): void {
    if (!this.actif) return;
    this.tempsReel.set(false);
    if (!this.timer) {
      this.rafraichir();
      this.timer = setInterval(() => this.rafraichir(), PERIODE_MS);
    }
    if (!this.relance) {
      this.relance = setTimeout(() => {
        this.relance = null;
        void this.ouvrirFlux();
      }, NOUVEL_ESSAI_FLUX_MS);
    }
  }

  private arreterInterrogation(): void {
    if (this.timer) clearInterval(this.timer);
    this.timer = null;
  }
}
