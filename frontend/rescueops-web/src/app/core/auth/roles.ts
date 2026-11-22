/** Les 8 rôles de realm RescueOps (même liste que l'enum Role de rescueops-common). */
export const ROLES = [
  'CITOYEN',
  'OPERATEUR',
  'GESTIONNAIRE_RESSOURCES',
  'AMBULANCIER',
  'COORDINATEUR_HOSPITALIER',
  'FORMATEUR',
  'APPRENANT',
  'ADMIN',
] as const;

export type Role = (typeof ROLES)[number];

/** Rôles qu'un utilisateur peut demander (jamais ADMIN, et CITOYEN est déjà acquis). */
export const ROLES_DEMANDABLES: readonly Role[] = [
  'OPERATEUR',
  'GESTIONNAIRE_RESSOURCES',
  'AMBULANCIER',
  'COORDINATEUR_HOSPITALIER',
  'FORMATEUR',
  'APPRENANT',
];

export const LIBELLES_ROLE: Record<Role, string> = {
  CITOYEN: 'Citoyen',
  OPERATEUR: 'Opérateur',
  GESTIONNAIRE_RESSOURCES: 'Gestionnaire des ressources',
  AMBULANCIER: 'Ambulancier',
  COORDINATEUR_HOSPITALIER: 'Coordinateur hospitalier',
  FORMATEUR: 'Formateur',
  APPRENANT: 'Apprenant',
  ADMIN: 'Administrateur',
};

/** Rôles qui voient l'activité opérationnelle (ticker EN DIRECT) : les mêmes que GET /api/transfers/feed. */
export const ROLES_OPERATIONNELS: readonly Role[] = ['OPERATEUR', 'AMBULANCIER', 'COORDINATEUR_HOSPITALIER', 'GESTIONNAIRE_RESSOURCES', 'ADMIN'];

/** Décode le contenu (payload) d'un JWT. Aucune vérification de signature : c'est le rôle de l'API. */
export function decoderJwt(token: string | null): Record<string, unknown> | null {
  if (!token) return null;
  const partie = token.split('.')[1];
  if (!partie) return null;
  try {
    const b64 = partie.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(partie.length / 4) * 4, '=');
    const octets = Uint8Array.from(atob(b64), (c) => c.charCodeAt(0));
    return JSON.parse(new TextDecoder().decode(octets));
  } catch {
    return null;
  }
}

/** Rôles RescueOps lus dans realm_access.roles de l'access token (les rôles techniques sont ignorés). */
export function extraireRoles(accessToken: string | null): Role[] {
  const claims = decoderJwt(accessToken);
  const realm = claims?.['realm_access'] as { roles?: unknown } | undefined;
  const roles = Array.isArray(realm?.roles) ? realm.roles : [];
  return roles.filter((r): r is Role => (ROLES as readonly string[]).includes(r as string));
}
