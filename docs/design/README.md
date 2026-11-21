# 🎨 Design RescueOps : la charte commune

> Toute l'application doit avoir **le même look**, quel que soit le membre qui code l'écran.
> Trois fichiers font foi :
> - [`maquette-v3.html`](maquette-v3.html) : **le modèle visuel validé**. Télécharge-le et ouvre-le dans ton navigateur : il est interactif (clic sur une carte, filtres, mode sombre).
> - [`tokens.css`](tokens.css) : **les couleurs, polices et composants, en code**. On le copie, on ne le réécrit pas.
> - ce README : **les règles**.

Inspiration : TeleTracking (leader de la gestion hospitalière) pour les couleurs, les interactions et les transitions, et Palantir Blueprint pour la densité des écrans de données.

---

## 1. Les couleurs

### Marque : bleu pétrole
| Rôle | Clair | Sombre | Usage |
|---|---|---|---|
| `--brand-deep` | `#0b2f40` | `#0a2433` | bandeau du haut, bloc héro, **bouton principal** |
| `--brand` | `#17698a` | `#4fb3d9` | liens, élément actif, petits titres (eyebrow) |
| `--brand-soft` | `#e1edf2` | `#12303d` | fond d'un élément sélectionné |
| `--brand-on` | `#8fd3ec` | `#8fd3ec` | accent sur fond foncé |

### Statuts : réservés aux statuts
| Rôle | Clair | Usage |
|---|---|---|
| `--ok` | `#1e7a52` | **DISPONIBLE**, succès, terminé |
| `--warn` | `#d99a00` | **RÉSERVÉ**, en attente, en cours |
| `--danger` | `#c8331f` | **SATURÉ**, erreur, urgence critique |

> ⚠️ **Règle n°1 :** un bouton n'est **jamais** vert, ambre ou rouge. Ces couleurs veulent dire un **statut**. Un opérateur doit comprendre l'écran en une seconde.
> Seule exception : un bouton de suppression peut être `--danger` en contour.

### Fonds et texte
Le fond est crème `#f2f0eb` (`--bg`), les cartes blanches (`--surface`), les bordures `#e4e1d8` (`--line`). Le texte principal est `--text`, le secondaire `--text-2`, et les libellés `--text-3`.

---

## 2. Les polices

| Police | Où | Exemple |
|---|---|---|
| **IBM Plex Sans** | toute l'interface, 14 px | libellés, paragraphes, boutons |
| **IBM Plex Mono** | identifiants, minuteurs, codes | `TRF-0412`, `14:21`, `MSN-2291` |
| **Barlow Condensed** 700 | gros chiffres des indicateurs | **127** places libres |

| Élément | Taille | Style |
|---|---|---|
| Titre de page (héro) | 34 px | poids **300**, avec un mot en *italique 600* : « Chaque patient, ***la bonne place*** » |
| Titre de section | 22 px | poids 500 |
| Petit titre (eyebrow) | 11 px | MAJUSCULES, espacement 2,2 px, couleur `--brand` |
| Texte | 14 px | poids 400 |
| Aide, date | 12 px | `--text-3` |

❌ Pas d'Inter, pas de Roboto, pas de police « par défaut d'Angular Material ».

---

## 3. Les composants (classes prêtes dans `tokens.css`)

| Composant | Classe | Règle |
|---|---|---|
| Bouton principal | `.btn .btn-primary` | **pilule** (rayon 999 px), une seule action principale par écran |
| Bouton secondaire | `.btn .btn-outline` | contour 0,8 px |
| Carte | `.card` | rayon 12 px, **se soulève** de 3 px au survol |
| Statut | `.tag .tag-ok` / `.tag-warn` / `.tag-danger` | texte en MAJUSCULES, police mono, point de couleur |
| Filtre | `.chip` (+ `.on` si actif) | pilule, MAJUSCULES |
| Petit titre | `.eyebrow` | au-dessus de chaque titre de section |
| Chiffre clé | `.num` | Barlow Condensed |
| Apparition | `.rise` | fondu vers le haut, décalé de 0,05 s par élément |

**Structure type d'un écran** (voir la maquette) :
1. **Le bandeau vert foncé « EN DIRECT »**, en haut, réservé aux rôles opérationnels.
2. **L'en-tête blanc** : logo, onglets à pastille glissante, bouton de thème ☾ et avatar.
3. **Le bloc héro bleu pétrole** : petit titre, grand titre, et 4 indicateurs (Voir / Anticiper / Coordonner / Agir).
4. **Une section** : petit titre, titre, filtres à droite, puis la **grille de cartes** ou le **tableau**.
5. **Le détail d'un élément** s'ouvre dans un **tiroir latéral droit** (fond flouté, Échap pour fermer), **jamais une nouvelle page**.
6. **Une action réussie** affiche un **toast** (pilule bleu pétrole) en bas de l'écran.

---

## 4. Les interactions (durées à respecter)

| Effet | Durée | Variable |
|---|---|---|
| Survol (couleur) | 0,15 s | `--t-fast` |
| Carte qui se soulève, bouton | 0,22 s | `--t` |
| Tiroir latéral | 0,45 s | `--t-slow` |
| Courbe d'animation | `cubic-bezier(.2,.8,.2,1)` | `--ease` |

Si l'utilisateur a désactivé les animations dans son système, **tout s'arrête** : c'est déjà géré dans `tokens.css`.

---

## 5. Mode sombre

Ajouter `data-theme="dark"` sur la balise `<html>`. Toutes les couleurs basculent automatiquement **si tu utilises les variables**. Une couleur écrite en dur (`#fff`, `black`…) cassera le mode sombre.

---

## 6. Checklist avant de proposer un écran

- [ ] Aucune couleur en dur, uniquement `var(--...)`.
- [ ] Les couleurs vert, ambre et rouge ne servent qu'à des statuts.
- [ ] Un seul bouton principal (`.btn-primary`) par zone.
- [ ] Les identifiants et heures sont en police mono.
- [ ] Le détail s'ouvre en tiroir, pas dans une nouvelle page.
- [ ] L'écran a été vérifié en mode **clair et sombre**.
- [ ] L'écran est lisible sur un téléphone (largeur 375 px).
- [ ] Les erreurs de l'API sont affichées avec leur `detail`, et pas avec un message technique.

> La coquille Angular commune (menu, bandeau, thème, connexion Keycloak) sera fournie par le **plan 5**.
> Chaque membre ajoutera alors ses écrans dans `frontend/.../features/<son-module>/`, en réutilisant ces composants.
