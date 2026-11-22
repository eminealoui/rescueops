import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, afterNextRender, inject, signal, viewChild } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';
import { Brand, ThemeToggle } from '../../core/ui/brand';
import { Ticker } from '../../core/ui/ticker';
import { CHIFFRES, ETAPES, MODULES, PROFILS, TICKER_DEMO } from './landing-data';
import { Reveal } from './reveal.directive';

const DUREE_ETAPE_MS = 4000;

/** Page d'accueil publique (maquette docs/design/accueil-v1.html). Inscription : toujours CITOYEN, rôle pro sur demande. */
@Component({
  selector: 'app-landing',
  imports: [Brand, ThemeToggle, Ticker, Reveal],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './landing.html',
  styleUrl: './landing.scss',
})
export class Landing {
  protected readonly auth = inject(AuthService);
  protected readonly ticker = TICKER_DEMO;
  protected readonly chiffres = CHIFFRES;
  protected readonly etapes = ETAPES;
  protected readonly modules = MODULES;
  protected readonly profils = PROFILS;

  /** Valeurs affichées des compteurs (animées de 0 à la cible). */
  protected readonly compteurs = signal(CHIFFRES.map(() => 0));
  protected readonly etape = signal(0);
  /** Incrémenté à chaque changement d'étape : relance l'animation de la barre de progression. */
  protected readonly cycle = signal(0);
  protected readonly profil = signal(0);

  private readonly trajet = viewChild<ElementRef<SVGPathElement>>('trajet');
  private readonly ambulance = viewChild<ElementRef<SVGGElement>>('ambulance');
  private readonly reduit = typeof matchMedia !== 'undefined' && matchMedia('(prefers-reduced-motion: reduce)').matches;
  private timer: ReturnType<typeof setTimeout> | null = null;
  private raf = 0;

  constructor() {
    const destroy = inject(DestroyRef);
    afterNextRender(() => {
      this.choisirEtape(0);
      this.animerAmbulance();
    });
    destroy.onDestroy(() => {
      if (this.timer) clearTimeout(this.timer);
      cancelAnimationFrame(this.raf);
    });
  }

  protected choisirEtape(i: number): void {
    this.etape.set(i);
    this.cycle.update((c) => c + 1);
    if (this.timer) clearTimeout(this.timer);
    if (!this.reduit) this.timer = setTimeout(() => this.choisirEtape((i + 1) % this.etapes.length), DUREE_ETAPE_MS);
  }

  protected defiler(id: string): void {
    document.getElementById(id)?.scrollIntoView({ behavior: this.reduit ? 'auto' : 'smooth', block: 'start' });
  }

  protected lancerCompteurs(): void {
    const cibles = this.chiffres.map((c) => c.valeur);
    if (this.reduit) return this.compteurs.set(cibles);
    const debut = performance.now();
    const pas = (t: number) => {
      const r = Math.min(1, (t - debut) / 1200);
      const ease = 1 - Math.pow(1 - r, 3);
      this.compteurs.set(cibles.map((c) => Math.round(c * ease)));
      if (r < 1) requestAnimationFrame(pas);
    };
    requestAnimationFrame(pas);
  }

  /** L'ambulance suit le trajet en pointillés de la carte du héro (boucle de 7 s). */
  private animerAmbulance(): void {
    const path = this.trajet()?.nativeElement;
    const amb = this.ambulance()?.nativeElement;
    if (!path || !amb || typeof path.getTotalLength !== 'function') return;
    const L = path.getTotalLength();
    const placer = (p: number) => {
      const pt = path.getPointAtLength(p * L);
      amb.setAttribute('transform', `translate(${pt.x},${pt.y})`);
    };
    if (this.reduit) return placer(0.55);
    let t0: number | null = null;
    const boucle = (t: number) => {
      t0 ??= t;
      placer(((t - t0) % 7000) / 7000);
      this.raf = requestAnimationFrame(boucle);
    };
    this.raf = requestAnimationFrame(boucle);
  }
}
