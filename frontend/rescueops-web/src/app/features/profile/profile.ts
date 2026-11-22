import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { DemandeRole, UserProfile } from '../../core/api/models';
import { UserApi } from '../../core/api/user.api';
import { AuthService } from '../../core/auth/auth.service';
import { LIBELLES_ROLE, ROLES_DEMANDABLES, Role } from '../../core/auth/roles';
import { ToastService } from '../../core/ui/toast.service';
import { creerFormulaireProfil, remplir, versRequete } from './profile-form';

const LIBELLES_STATUT_DEMANDE = { EN_ATTENTE: 'En attente', ACCEPTEE: 'Acceptée', REFUSEE: 'Refusée' } as const;

/** Mon profil (coordonnées) et mes rôles (demande d'un rôle professionnel, suivi des demandes). */
@Component({
  selector: 'app-profile',
  imports: [ReactiveFormsModule, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile {
  private readonly api = inject(UserApi);
  private readonly toast = inject(ToastService);
  protected readonly auth = inject(AuthService);
  protected readonly libellesRole = LIBELLES_ROLE;
  protected readonly libellesStatut = LIBELLES_STATUT_DEMANDE;

  protected readonly profil = signal<UserProfile | null>(null);
  protected readonly enregistrement = signal(false);
  protected readonly form = creerFormulaireProfil();

  protected readonly demandes = signal<DemandeRole[]>([]);
  protected readonly envoi = signal(false);
  protected readonly demandeForm = new FormGroup({
    role: new FormControl<Role | null>(null, { validators: [Validators.required] }),
    organisation: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(150), Validators.pattern(/\S/)] }),
    motif: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(500)] }),
  });

  protected readonly rolesPossibles = computed(() => ROLES_DEMANDABLES.filter((r) => !this.auth.roles().includes(r)));
  protected readonly enAttente = computed(() => this.demandes().find((d) => d.statut === 'EN_ATTENTE') ?? null);
  /** Rôle accordé par un ADMIN mais pas encore présent dans le token actuel. */
  protected readonly roleAccordeNonVisible = computed(() =>
    this.demandes().some((d) => d.statut === 'ACCEPTEE' && !this.auth.roles().includes(d.roleDemande)),
  );

  constructor() {
    this.api.moi().subscribe((p) => {
      this.profil.set(p);
      remplir(this.form, p);
    });
    this.chargerDemandes();
  }

  protected enregistrer(): void {
    if (this.form.invalid) return this.form.markAllAsTouched();
    this.enregistrement.set(true);
    this.api
      .modifierMoi(versRequete(this.form))
      .pipe(finalize(() => this.enregistrement.set(false)))
      .subscribe((p) => {
        this.profil.set(p);
        remplir(this.form, p);
        this.toast.ok('Profil enregistré');
      });
  }

  protected annulerModifs(): void {
    const p = this.profil();
    if (p) remplir(this.form, p);
  }

  protected demanderRole(): void {
    if (this.demandeForm.invalid) return this.demandeForm.markAllAsTouched();
    const v = this.demandeForm.getRawValue();
    this.envoi.set(true);
    this.api
      .demanderRole({ role: v.role!, organisation: v.organisation.trim(), motif: v.motif.trim() || null })
      .pipe(finalize(() => this.envoi.set(false)))
      .subscribe(() => {
        this.toast.ok('Demande envoyée. Un administrateur va l\'examiner.');
        this.demandeForm.reset();
        this.chargerDemandes();
      });
  }

  protected async actualiser(): Promise<void> {
    await this.auth.actualiserRoles();
    this.toast.ok('Rôles actualisés');
  }

  protected invalide(nom: keyof typeof this.form.controls): boolean {
    const c = this.form.controls[nom];
    return c.invalid && c.touched;
  }

  private chargerDemandes(): void {
    this.api.mesDemandes().subscribe((d) => this.demandes.set(d));
  }
}
