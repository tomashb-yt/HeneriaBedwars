# HeneriaBedWars

HeneriaBedWars est un plugin BedWars expérimental pour Paper 1.21.11, développé en Java 21. Le dépôt contient actuellement la fondation technique uniquement : aucun gameplay BedWars n'est encore disponible.

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

## État

La commande administrative `/bedwars version` (alias `/hbw version`) permet uniquement de diagnostiquer le chargement de la fondation. Arènes, équipes, lits, boutiques, générateurs, stockage et parties ne sont pas implémentés.
