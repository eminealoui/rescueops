import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, Routes } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { roleGuard } from '../../core/auth/role.guard';
import { Role } from '../../core/auth/roles';

const SOUS_PAGES: { label: string; path: string; roles: readonly Role[] }[] = [
  { label: 'Capacités', path: 'capacites', roles: [] },
  { label: 'Nouvelle demande', path: 'nouvelle-demande', roles: ['OPERATEUR', 'AMBULANCIER', 'ADMIN'] },
  { label: 'Transferts', path: 'transferts', roles: [] },
  { label: 'Admissions', path: 'admissions', roles: ['COORDINATEUR_HOSPITALIER', 'ADMIN'] },
  { label: 'Établissements', path: 'etablissements', roles: ['COORDINATEUR_HOSPITALIER', 'ADMIN'] },
];

/** Module 5 · Coordination hospitalière : sous-navigation par rôle. */
@Component({
  selector: 'app-hospital-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav class="sous" aria-label="Coordination hospitalière">
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
export class HospitalShell {
  private readonly auth = inject(AuthService);
  protected readonly pages = computed(() => SOUS_PAGES.filter((p) => this.auth.hasAnyRole(p.roles)));
}

export const HOSPITAL_ROUTES: Routes = [
  {
    path: '',
    component: HospitalShell,
    children: [
      { path: '', redirectTo: 'capacites', pathMatch: 'full' },
      { path: 'capacites', title: 'Capacités · RescueOps', loadComponent: () => import('./capacites').then((m) => m.Capacites) },
      {
        path: 'nouvelle-demande',
        title: 'Nouvelle demande · RescueOps',
        canActivate: [roleGuard('OPERATEUR', 'AMBULANCIER', 'ADMIN')],
        loadComponent: () => import('./nouvelle-demande').then((m) => m.NouvelleDemande),
      },
      { path: 'transferts', title: 'Transferts · RescueOps', loadComponent: () => import('./transferts').then((m) => m.Transferts) },
      {
        path: 'admissions',
        title: 'Admissions · RescueOps',
        canActivate: [roleGuard('COORDINATEUR_HOSPITALIER', 'ADMIN')],
        loadComponent: () => import('./admissions').then((m) => m.Admissions),
      },
      {
        path: 'etablissements',
        title: 'Établissements · RescueOps',
        canActivate: [roleGuard('COORDINATEUR_HOSPITALIER', 'ADMIN')],
        loadComponent: () => import('./etablissements').then((m) => m.Etablissements),
      },
    ],
  },
];
