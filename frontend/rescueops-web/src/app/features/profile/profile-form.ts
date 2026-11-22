import { AbstractControl, FormControl, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { UpdateMyProfile, UserProfile } from '../../core/api/models';

/** Même règle que le backend (UpdateMyProfileRequest) : format international, ou vide pour effacer. */
export const TELEPHONE_E164 = /^\+?[1-9][0-9]{7,14}$/;

/** Espaces tolérés à la saisie (« +216 98 123 456 ») : ils sont retirés avant l'envoi. */
function telephone(c: AbstractControl<string>): ValidationErrors | null {
  const v = (c.value ?? '').replace(/\s+/g, '');
  return !v || TELEPHONE_E164.test(v) ? null : { pattern: true };
}

export function creerFormulaireProfil() {
  return new FormGroup({
    firstName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100), Validators.pattern(/\S/)] }),
    lastName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100), Validators.pattern(/\S/)] }),
    phone: new FormControl('', { nonNullable: true, validators: [telephone] }),
    address: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(255)] }),
  });
}

export type FormulaireProfil = ReturnType<typeof creerFormulaireProfil>;

export function remplir(form: FormulaireProfil, p: UserProfile): void {
  form.reset({ firstName: p.firstName ?? '', lastName: p.lastName ?? '', phone: p.phone ?? '', address: p.address ?? '' });
}

/** Valeurs envoyées à PUT /api/users/me (espaces retirés, champ vide → null). */
export function versRequete(form: FormulaireProfil): UpdateMyProfile {
  const v = form.getRawValue();
  const ouNull = (s: string) => (s.trim() ? s.trim() : null);
  return { firstName: v.firstName.trim(), lastName: v.lastName.trim(), phone: ouNull(v.phone.replace(/\s+/g, '')), address: ouNull(v.address) };
}
