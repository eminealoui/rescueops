import { ChangeDetectionStrategy, Component, ElementRef, effect, inject, input, output, viewChild, afterNextRender, DestroyRef } from '@angular/core';
import * as L from 'leaflet';
import { ThemeService } from '../../../core/theme/theme.service';

export interface PointCarte {
  lat: number;
  lon: number;
  libelle: string;
  type: 'origine' | 'hopital' | 'choix';
}

/** Centre par défaut : Tunis. */
const TUNIS: L.LatLngTuple = [36.8065, 10.1815];

/**
 * Carte Leaflet avec les tuiles gratuites d'OpenStreetMap (attribution affichée ; sombres par filtre CSS en mode sombre).
 * Aucune requête vers les tuiles ne porte le token : l'intercepteur ne l'ajoute qu'à /api.
 * Points en cercles colorés (pas d'images d'icônes), trajet en pointillés entre l'origine et l'hôpital choisi.
 */
@Component({
  selector: 'app-carte',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div #zone class="zone" [attr.aria-label]="libelle()" role="region"></div>`,
  styles: `
    :host { display: block; position: relative; border-radius: inherit; overflow: hidden; }
    .zone { position: absolute; inset: 0; background: var(--map-bg); }
  `,
})
export class Carte {
  readonly points = input<PointCarte[]>([]);
  readonly trajet = input(false);
  readonly cliquable = input(false);
  readonly libelle = input('Carte');
  readonly clic = output<{ lat: number; lon: number }>();

  private readonly zone = viewChild.required<ElementRef<HTMLDivElement>>('zone');
  private readonly theme = inject(ThemeService);
  private carte?: L.Map;
  private tuiles?: L.TileLayer;
  private calque = L.layerGroup();

  constructor() {
    afterNextRender(() => {
      this.carte = L.map(this.zone().nativeElement, { zoomControl: true, attributionControl: true }).setView(TUNIS, 11);
      this.calque.addTo(this.carte);
      this.carte.on('click', (e: L.LeafletMouseEvent) => {
        if (this.cliquable()) this.clic.emit({ lat: +e.latlng.lat.toFixed(5), lon: +e.latlng.lng.toFixed(5) });
      });
      this.appliquerTuiles();
      this.dessiner();
      // La carte peut naître dans un tiroir encore en animation : on recalcule sa taille.
      setTimeout(() => this.carte?.invalidateSize(), 450);
    });
    effect(() => {
      this.theme.theme();
      this.appliquerTuiles();
    });
    effect(() => {
      this.points();
      this.trajet();
      this.dessiner();
    });
    inject(DestroyRef).onDestroy(() => this.carte?.remove());
  }

  private appliquerTuiles(): void {
    if (!this.carte) return;
    this.tuiles?.remove();
    // Tuiles OpenStreetMap (gratuites, sans clé, attribution obligatoire). Mode sombre : filtre CSS sur les tuiles.
    this.tuiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      className: this.theme.theme() === 'dark' ? 'tuiles-sombres' : '',
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(this.carte);
  }

  private dessiner(): void {
    if (!this.carte) return;
    this.calque.clearLayers();
    const css = getComputedStyle(document.documentElement);
    const couleur = (v: string) => css.getPropertyValue(v).trim() || '#17698a';
    const pts = this.points();
    for (const p of pts) {
      const c = p.type === 'origine' ? couleur('--danger') : p.type === 'choix' ? couleur('--brand') : couleur('--ok');
      L.circleMarker([p.lat, p.lon], { radius: p.type === 'hopital' ? 6 : 8, color: '#fff', weight: 2, fillColor: c, fillOpacity: 1 })
        .bindTooltip(p.libelle, { direction: 'top', offset: [0, -6] })
        .addTo(this.calque);
    }
    const origine = pts.find((p) => p.type === 'origine');
    const choix = pts.find((p) => p.type === 'choix');
    if (this.trajet() && origine && choix) {
      L.polyline([[origine.lat, origine.lon], [choix.lat, choix.lon]], { color: couleur('--brand'), weight: 3, dashArray: '7 6' }).addTo(this.calque);
    }
    if (pts.length > 1) this.carte.fitBounds(L.latLngBounds(pts.map((p) => [p.lat, p.lon] as L.LatLngTuple)), { padding: [30, 30], maxZoom: 13 });
    else if (pts.length === 1) this.carte.setView([pts[0]!.lat, pts[0]!.lon], 12);
  }
}
