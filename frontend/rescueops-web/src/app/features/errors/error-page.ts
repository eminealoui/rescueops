import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

const PAGES: Record<number, { titre: string; texte: string }> = {
  403: { titre: 'Accès refusé', texte: "Votre rôle ne permet pas d'ouvrir cette page. Si vous en avez besoin, demandez le rôle adapté depuis votre profil." },
  404: { titre: 'Page introuvable', texte: "Cette adresse n'existe pas, ou plus." },
  503: { titre: 'Service momentanément indisponible', texte: 'Le serveur ne répond pas pour le moment. Réessayez dans un instant.' },
};

/** Pages d'erreur (le code vient des données de la route). */
@Component({
  selector: 'app-error-page',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="err">
      <div class="code num">{{ code() }}</div>
      <h1 class="page-title">{{ page().titre }}</h1>
      <p class="page-sub">{{ page().texte }}</p>
      <div class="actions">
        @if (auth.isAuthenticated()) {
          <a routerLink="/accueil" class="btn btn-primary">Retour à l'accueil</a>
          @if (code() === 403) { <a routerLink="/profil" class="btn btn-outline">Demander un rôle</a> }
        } @else {
          <a routerLink="/" class="btn btn-primary">Retour à l'accueil</a>
        }
      </div>
    </div>
  `,
  styles: `
    .err { max-width: 560px; margin: 10vh auto; padding: 0 16px; text-align: center; display: grid; gap: 10px; justify-items: center; }
    .code { font-size: 96px; line-height: 1; color: var(--brand); opacity: .25; }
    .actions { display: flex; gap: 10px; margin-top: 14px; flex-wrap: wrap; justify-content: center; }
  `,
})
export class ErrorPage {
  protected readonly auth = inject(AuthService);
  readonly code = input<number>(404);
  protected readonly page = computed(() => PAGES[this.code()] ?? PAGES[404]!);
}
