import { DOCUMENT, Injectable, inject, signal } from '@angular/core';

export type Theme = 'light' | 'dark';
const CLE = 'rescueops.theme';

/** Clair par défaut (choix validé de la maquette), sombre sur demande, mémorisé dans le navigateur. Appliqué sur <html data-theme>. */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly doc = inject(DOCUMENT);
  readonly theme = signal<Theme>(this.initial());

  constructor() {
    this.appliquer(this.theme());
  }

  basculer(): void {
    const t: Theme = this.theme() === 'dark' ? 'light' : 'dark';
    this.theme.set(t);
    this.appliquer(t);
    try {
      localStorage.setItem(CLE, t);
    } catch {
      /* navigation privée : le choix n'est simplement pas mémorisé */
    }
  }

  private initial(): Theme {
    try {
      const memo = localStorage.getItem(CLE);
      if (memo === 'light' || memo === 'dark') return memo;
    } catch {
      /* ignoré */
    }
    return 'light';
  }

  private appliquer(t: Theme): void {
    this.doc.documentElement.dataset['theme'] = t;
  }
}
