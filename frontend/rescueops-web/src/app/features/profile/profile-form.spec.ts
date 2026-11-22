import { creerFormulaireProfil, versRequete } from './profile-form';

describe('formulaire de profil', () => {
  it('prénom et nom obligatoires (des espaces seuls ne suffisent pas)', () => {
    const f = creerFormulaireProfil();
    f.setValue({ firstName: '   ', lastName: '', phone: '', address: '' });
    expect(f.controls.firstName.valid).toBe(false);
    expect(f.controls.lastName.hasError('required')).toBe(true);
  });

  it('téléphone facultatif, au format international', () => {
    const f = creerFormulaireProfil();
    f.setValue({ firstName: 'Amine', lastName: 'Ben Salah', phone: '', address: '' });
    expect(f.valid).toBe(true);
    f.controls.phone.setValue('abc');
    expect(f.controls.phone.hasError('pattern')).toBe(true);
    f.controls.phone.setValue('+21698123456');
    expect(f.valid).toBe(true);
    f.controls.phone.setValue('+216 98 123 456');
    expect(f.valid).toBe(true);
  });

  it('la requête retire les espaces et transforme les champs vides en null', () => {
    const f = creerFormulaireProfil();
    f.setValue({ firstName: ' Amine ', lastName: 'Ben Salah', phone: '+216 98 123 456', address: '  ' });
    expect(versRequete(f)).toEqual({ firstName: 'Amine', lastName: 'Ben Salah', phone: '+21698123456', address: null });
  });
});
