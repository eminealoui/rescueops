import { Routes } from '@angular/router';
import { publicGuard, roleGuard } from './core/auth/role.guard';

/**
 * Routes. Tout est chargé à la demande. Les guards ne servent qu'à l'ergonomie : l'API vérifie toujours les rôles.
 * Chaque coéquipier ajoute SES routes dans le bloc « modules » (voir features/modules/README.md).
 */
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    canActivate: [publicGuard],
    title: 'RescueOps · Coordination des urgences',
    loadComponent: () => import('./features/landing/landing').then((m) => m.Landing),
  },
  {
    path: '',
    canActivate: [roleGuard()],
    loadComponent: () => import('./layout/shell').then((m) => m.Shell),
    children: [
      { path: 'accueil', title: 'Accueil · RescueOps', loadComponent: () => import('./features/accueil/accueil').then((m) => m.Accueil) },
      { path: 'profil', title: 'Mon profil · RescueOps', loadComponent: () => import('./features/profile/profile').then((m) => m.Profile) },
      {
        path: 'admin/utilisateurs',
        title: 'Utilisateurs · RescueOps',
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () => import('./features/admin-users/admin-users').then((m) => m.AdminUsers),
      },
      {
        path: 'admin/demandes-role',
        title: 'Demandes de rôle · RescueOps',
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () => import('./features/role-requests/admin-role-requests').then((m) => m.AdminRoleRequests),
      },
      {
        path: 'hopitaux',
        canActivate: [roleGuard('OPERATEUR', 'AMBULANCIER', 'COORDINATEUR_HOSPITALIER', 'ADMIN')],
        loadChildren: () => import('./features/hospital/hospital.routes').then((m) => m.HOSPITAL_ROUTES),
      },
      {
        path: 'modules/:code',
        title: 'Module · RescueOps',
        loadComponent: () => import('./features/modules/module-placeholder').then((m) => m.ModulePlaceholder),
      },
      {
        path: 'acces-refuse',
        title: 'Accès refusé · RescueOps',
        data: { code: 403 },
        loadComponent: () => import('./features/errors/error-page').then((m) => m.ErrorPage),
      },
    ],
  },
  {
    path: '**',
    title: 'Page introuvable · RescueOps',
    data: { code: 404 },
    loadComponent: () => import('./features/errors/error-page').then((m) => m.ErrorPage),
  },
];
