# Définitions de zombies

Les fichiers résident dans `plugins/HeneriaZombie/zombies/*.yml`, utilisent `schema-version: 1`
et sont installés au premier démarrage. Les identifiants stables respectent
`[a-z0-9_]{1,64}`.

Sections prises en charge :

- `entity`, `category` et `appearance` ;
- `attributes` avec progression par manche et plafonds ;
- `behavior`, ciblage, portée et cooldown ;
- `navigation` et secours anti-blocage ;
- `damage`, immunités, multiplicateurs et tirs à la tête ;
- `spawn-rules`, manches, poids, plafond vivant, zones et pools ;
- `rewards`, `abilities` et `environment`.

Types livrés : `classic_zombie`, `sprinter_zombie`, `armored_zombie` et `toxic_zombie`.
`/zzombie reload` charge un candidat hors thread, le valide entièrement puis l'active en une seule
opération. Une erreur indique le fichier et le chemin fautif et conserve le dernier registre valide.
