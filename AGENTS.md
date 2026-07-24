# Guide de contribution — HeneriaZombie

## Lecture obligatoire

Avant chaque ticket, lire intégralement :

1. `AGENTS.md` ;
2. `docs/AI_CONTEXT.md` ;
3. `docs/ARCHITECTURE.md` ;
4. `docs/DECISIONS.md` ;
5. `docs/ROADMAP.md` ;
6. les documents du système modifié ;
7. les dernières entrées de `docs/CHANGELOG.md`.

Inspecter ensuite Git et le code touché. La documentation du dépôt est la source de vérité.

## Règles

- Java 21 et Paper 1.21.x.
- `zombie-api` ne dépend d'aucune plateforme ni d'un autre module du projet.
- `zombie-core` peut dépendre de l'API, jamais de Bukkit/Paper.
- `zombie-plugin` est l'unique frontière Paper.
- Injection explicite par constructeur ; aucun singleton global mutable.
- Aucun accès disque ou SQLite bloquant sur le thread serveur.
- Les configurations sont versionnées, validées puis activées atomiquement.
- Aucun gameplay provisoire, aucun `TODO` sans ticket, aucun secret.
- Mettre à jour la documentation et le changelog avec le code réel.

## Définition de terminé

Un ticket est terminé lorsque compilation, tests, formatage, JAR, documentation et
`git diff --check` passent, puis que le changement est commité et publié. Toute vérification
manuelle non réalisable doit être signalée honnêtement.
