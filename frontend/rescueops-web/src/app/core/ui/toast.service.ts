import { Injectable, signal } from '@angular/core';

export type ToastKind = 'ok' | 'info' | 'danger';
export interface Toast {
  id: number;
  kind: ToastKind;
  text: string;
}

/** File de messages courts (succès, erreurs métier). Chaque toast disparaît seul après quelques secondes. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private seq = 0;
  readonly toasts = signal<Toast[]>([]);

  ok(text: string): void {
    this.show('ok', text);
  }

  info(text: string): void {
    this.show('info', text);
  }

  danger(text: string): void {
    this.show('danger', text);
  }

  show(kind: ToastKind, text: string, dureeMs = 5000): void {
    // Le même message déjà affiché n'est pas empilé (ex. plusieurs requêtes en échec en même temps).
    if (this.toasts().some((t) => t.text === text)) return;
    const id = ++this.seq;
    this.toasts.update((l) => [...l.slice(-3), { id, kind, text }]);
    setTimeout(() => this.fermer(id), dureeMs);
  }

  fermer(id: number): void {
    this.toasts.update((l) => l.filter((t) => t.id !== id));
  }
}
