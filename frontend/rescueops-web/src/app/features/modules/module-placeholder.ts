import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

export const MODULES_EQUIPE: Record<string, { titre: string; texte: string }> = {
  ms1: { titre: 'Gestion des urgences', texte: 'Signalements, priorisation, doublons et statuts.' },
  ms2: { titre: 'Gestion des ressources', texte: 'Ambulances, équipes, équipements et maintenances.' },
  ms3: { titre: 'Dispatching intelligent', texte: 'Recommandations classées et affectations.' },
  ms4: { titre: 'Interventions', texte: 'Missions, états, itinéraires et rapports.' },
  ms6: { titre: 'Formations', texte: 'Sessions, évaluations, certifications et simulations.' },
};

/** Écran d'attente d'un module développé par un autre membre de l'équipe (remplacé par ses vraies routes). */
@Component({
  selector: 'app-module-placeholder',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (module(); as m) {
      <div class="eyebrow">Module {{ code().toUpperCase() }}</div>
      <h1 class="page-title">{{ m.titre }}</h1>
      <p class="page-sub">{{ m.texte }}</p>
      <div class="card vide">
        <div class="ic" aria-hidden="true">⚙</div>
        <h2>Écrans en construction</h2>
        <p>Ce module est développé par un autre membre de l'équipe. Il apparaîtra ici dès que ses écrans seront branchés sur la coquille commune.</p>
        <a routerLink="/accueil" class="btn btn-outline">Retour à l'accueil</a>
      </div>
    } @else {
      <h1 class="page-title">Module inconnu</h1>
      <a routerLink="/accueil" class="btn btn-outline">Retour à l'accueil</a>
    }
  `,
  styles: `
    .vide { margin-top: 24px; display: grid; gap: 10px; justify-items: start; max-width: 640px; }
    .vide:hover { transform: none; }
    .ic { width: 44px; height: 44px; border-radius: 12px; background: var(--brand-soft); color: var(--brand); display: grid; place-items: center; font-size: 20px; }
    h2 { font-size: 18px; font-weight: 600; }
    p { color: var(--text-2); }
  `,
})
export class ModulePlaceholder {
  readonly code = input<string>('');
  protected readonly module = computed(() => MODULES_EQUIPE[this.code()]);
}
