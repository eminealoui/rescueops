import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { Role } from '../../core/auth/roles';

interface Raccourci {
  titre: string;
  texte: string;
  lien: string;
  icone: string;
  roles: readonly Role[];
}

const RACCOURCIS: Raccourci[] = [
  { titre: 'Capacités hospitalières', texte: 'Places libres, réservations et services saturés, en temps réel.', lien: '/hopitaux/capacites', icone: '✚',
    roles: ['OPERATEUR', 'AMBULANCIER', 'COORDINATEUR_HOSPITALIER', 'ADMIN'] },
  { titre: 'Nouvelle demande de transfert', texte: 'Trouver l\'établissement compatible et retenir une place.', lien: '/hopitaux/nouvelle-demande', icone: '➜',
    roles: ['OPERATEUR', 'AMBULANCIER', 'ADMIN'] },
  { titre: 'Transferts', texte: 'Suivre, confirmer ou refuser les demandes en cours.', lien: '/hopitaux/transferts', icone: '⇄',
    roles: ['OPERATEUR', 'AMBULANCIER', 'COORDINATEUR_HOSPITALIER', 'ADMIN'] },
  { titre: 'Admissions', texte: 'Admettre les patients attendus et déclarer les sorties.', lien: '/hopitaux/admissions', icone: '⌂',
    roles: ['COORDINATEUR_HOSPITALIER', 'ADMIN'] },
  { titre: 'Signaler une urgence', texte: 'Déclarer un incident et suivre son traitement.', lien: '/modules/ms1', icone: '⚠', roles: ['CITOYEN', 'OPERATEUR'] },
  { titre: 'Ressources', texte: 'Ambulances, équipes et maintenances.', lien: '/modules/ms2', icone: '⛟', roles: ['GESTIONNAIRE_RESSOURCES'] },
  { titre: 'Formations', texte: 'Sessions, évaluations et certifications.', lien: '/modules/ms6', icone: '✎', roles: ['FORMATEUR', 'APPRENANT'] },
  { titre: 'Utilisateurs', texte: 'Rechercher, activer ou désactiver les comptes.', lien: '/admin/utilisateurs', icone: '☰', roles: ['ADMIN'] },
  { titre: 'Demandes de rôle', texte: 'Accorder ou refuser les rôles professionnels demandés.', lien: '/admin/demandes-role', icone: '✓', roles: ['ADMIN'] },
  { titre: 'Mon profil', texte: 'Mes coordonnées, et la demande d\'un rôle professionnel.', lien: '/profil', icone: '☺', roles: [] },
];

/** Tableau d'accueil : raccourcis selon les rôles de l'utilisateur connecté. */
@Component({
  selector: 'app-accueil',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="hero rise">
      <div class="eyebrow">Bonjour</div>
      <h1>{{ prenom() }}, <em>que faisons-nous aujourd'hui ?</em></h1>
      <p>Vos raccourcis s'adaptent à vos rôles. Un rôle professionnel vous manque ? Demandez-le depuis votre profil.</p>
    </section>
    <div class="grille">
      @for (r of raccourcis(); track r.lien; let i = $index) {
        <a class="card tuile rise" [routerLink]="r.lien" [style.animation-delay.s]="0.05 * i">
          <span class="ic" aria-hidden="true">{{ r.icone }}</span>
          <span class="t">{{ r.titre }}</span>
          <span class="d">{{ r.texte }}</span>
        </a>
      }
    </div>
  `,
  styles: `
    .hero { background: var(--brand-deep); border-radius: var(--radius-lg); padding: 30px 32px; color: #fdfcf0; box-shadow: var(--shadow-deep);
            position: relative; overflow: hidden; }
    .hero::after { content: ""; position: absolute; right: -80px; top: -80px; width: 320px; height: 320px; border-radius: 50%;
                   background: radial-gradient(circle, rgba(143, 211, 236, .18), transparent 65%); }
    .hero .eyebrow { color: var(--brand-on); }
    h1 { font-size: var(--fs-h1); font-weight: 300; letter-spacing: -.8px; line-height: 1.15; margin: 8px 0 6px; }
    p { color: rgba(255, 255, 255, .72); max-width: 560px; }
    .grille { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 14px; margin-top: 22px; }
    .tuile { display: grid; gap: 6px; align-content: start; position: relative; }
    .tuile::after { content: "→"; position: absolute; right: 18px; top: 18px; color: var(--brand); opacity: 0; transform: translateX(-6px); transition: all var(--t); }
    .tuile:hover::after { opacity: 1; transform: none; }
    .ic { width: 40px; height: 40px; border-radius: 11px; background: var(--brand-deep); color: #fff; display: grid; place-items: center; font-size: 18px; margin-bottom: 6px; }
    .t { font-weight: 600; font-size: 16px; }
    .d { color: var(--text-2); font-size: 13px; }
    @media (max-width: 560px) { .hero { padding: 24px 20px; } h1 { font-size: 26px; } }
  `,
})
export class Accueil {
  private readonly auth = inject(AuthService);
  protected readonly prenom = computed(() => this.auth.utilisateur()?.nom.split(' ')[0] || 'Bienvenue');
  protected readonly raccourcis = computed(() =>
    RACCOURCIS.filter((r) => r.roles.length === 0 || r.roles.some((x) => this.auth.roles().includes(x))),
  );
}
