# HeneriaBedWars

HeneriaBedWars est un plugin BedWars expérimental pour Spigot/Paper 1.21.x, développé en Java 21. Le dépôt contient la fondation technique, un système multi-fichiers de configuration et les traductions françaises/anglaises. Aucun gameplay BedWars n'est encore disponible.

## Prérequis et compilation

- JDK 21 ;
- connexion à Maven Central et au dépôt Paper lors du premier build ;
- Gradle Wrapper fourni par le dépôt.

Sous Linux ou macOS :

```bash
./gradlew clean build
```

Sous Windows :

```powershell
.\gradlew.bat clean build
```

Le plugin déployable est généré dans `bedwars-plugin/build/libs/HeneriaBedWars-0.1.0-SNAPSHOT.jar`.

## Modules

- `bedwars-api` : contrats publics stables, sans dépendance Paper ;
- `bedwars-core` : logique indépendante de Paper, services et cycle de vie ;
- `bedwars-plugin` : adaptateur Paper, bootstrap, configuration et commandes.

## Développement

Lire d'abord [AGENTS.md](AGENTS.md), puis la documentation de reprise dans [docs/ai](docs/ai). Utiliser `./gradlew spotlessApply` avant `./gradlew clean build`. Toute contribution doit rester dans le périmètre du ticket actif, ajouter des tests et mettre la documentation à jour.

## Configuration et commandes

Au premier démarrage, le plugin crée les neuf YAML principaux, `languages/fr_FR.yml`, `languages/en_US.yml` et les dossiers `arenas`, `languages` et `backups`, sans écraser les fichiers existants. `/bedwars reload` recharge tout de manière transactionnelle, `/bedwars config` affiche un diagnostic sans secret et `/bedwars language set <locale>` persiste la langue. L'alias `/hbw` accepte les mêmes sous-commandes.

Les arènes, équipes, lits, menus métier, achats, générateurs actifs, stockage SQL et parties ne sont pas implémentés.

Le framework interne de menus est disponible via `/bedwars gui` ou `/hbw gui` avec `heneriabedwars.admin.gui`. Il démontre navigation, pagination, confirmation, rafraîchissement et protection des inventaires, sans fonctionnalité BedWars.
