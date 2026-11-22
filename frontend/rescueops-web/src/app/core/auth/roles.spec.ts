import { extraireRoles, ROLES } from './roles';

/** Construit un faux JWT (non signé) : seul le contenu compte pour l'extraction. */
function jwt(payload: object): string {
  const b64 = (o: object) => btoa(JSON.stringify(o)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${b64({ alg: 'none' })}.${b64(payload)}.sig`;
}

describe('extraireRoles', () => {
  it('garde uniquement les rôles RescueOps (pas les rôles techniques Keycloak)', () => {
    const token = jwt({ realm_access: { roles: ['offline_access', 'default-roles-rescueops', 'CITOYEN', 'uma_authorization', 'AMBULANCIER'] } });
    expect(extraireRoles(token)).toEqual(['CITOYEN', 'AMBULANCIER']);
  });

  it('renvoie une liste vide sans token, avec un token illisible ou sans realm_access', () => {
    expect(extraireRoles(null)).toEqual([]);
    expect(extraireRoles('pas-un-jwt')).toEqual([]);
    expect(extraireRoles(jwt({ sub: 'x' }))).toEqual([]);
  });

  it('décode le base64url (caractères - et _)', () => {
    const token = jwt({ realm_access: { roles: ['ADMIN'] }, name: '??>>éè' });
    expect(extraireRoles(token)).toEqual(['ADMIN']);
  });

  it('connaît les 8 rôles du realm', () => {
    expect(ROLES.length).toBe(8);
  });
});
