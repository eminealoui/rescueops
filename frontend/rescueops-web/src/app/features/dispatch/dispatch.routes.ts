import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, Routes } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { roleGuard } from '../../core/auth/role.guard';
import { Role } from '../../core/auth/roles';

const SOUS_PAGES: { label: string; path: string; roles: readonly Role[] }[] = [
  { label: 'Recommandations', path: 'recommandations', roles: ['OPERATEUR', 'ADMIN'] },
  { label: 'Affectations', path: 'affectations', roles: ['OPERATEUR', 'ADMIN'] },
  { label: 'Nouvelle affectation', path: 'nouvelle-affectation', roles: ['OPERATEUR', 'ADMIN'] },
];

/** Module 3 · Dispatching intelligent : sous-navigation par rôle. */
@Component({
  selector: 'app-dispatch-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav class="sous" aria-label="Dispatch intelligent">
      @for (p of pages(); track p.path) {
        <a [routerLink]="p.path" routerLinkActive="on" ariaCurrentWhenActive="page">{{ p.label }}</a>
      }
    </nav>
    <router-outlet />
  `,
  styles: `
    .sous { display: flex; gap: 22px; border-bottom: 1px solid var(--line); margin-bottom: 22px; overflow-x: auto; scrollbar-width: none; }
    .sous a { padding: 10px 0; font-size: 13.5px; color: var(--text-2); border-bottom: 2px solid transparent; white-space: nowrap;
              transition: color var(--t-fast), border-color var(--t); }
    .sous a:hover { color: var(--text); }
    .sous a.on { color: var(--brand); border-color: var(--brand); font-weight: 600; }
  `,
})
export class DispatchShell {
  private readonly auth = inject(AuthService);
  protected readonly pages = computed(() => SOUS_PAGES.filter((p) => this.auth.hasAnyRole(p.roles)));
}

export const DISPATCH_ROUTES: Routes = [
  {
    path: '',
    component: DispatchShell,
    children: [
      { path: '', redirectTo: 'recommandations', pathMatch: 'full' },
      {
        path: 'recommandations',
        title: 'Recommandations · RescueOps',
        canActivate: [roleGuard('OPERATEUR', 'ADMIN')],
        loadComponent: () => import('./recommandations').then((m) => m.Recommandations),
      },
      {
        path: 'affectations',
        title: 'Affectations · RescueOps',
        canActivate: [roleGuard('OPERATEUR', 'ADMIN')],
        loadComponent: () => import('./affectations').then((m) => m.Affectations),
      },
      {
        path: 'nouvelle-affectation',
        title: 'Nouvelle affectation · RescueOps',
        canActivate: [roleGuard('OPERATEUR', 'ADMIN')],
        loadComponent: () => import('./nouvelle-affectation').then((m) => m.NouvelleAffectation),
      },
    ],
  },
];
