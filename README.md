# HeneriaBedWars

HeneriaBedWars est un plugin BedWars expérimental pour Spigot/Paper 1.21.x, développé en Java 21. Le dépôt contient la fondation technique, la configuration, les menus administratifs, les arènes et un gestionnaire autonome de cartes modèles vides. Aucun gameplay BedWars n'est encore disponible.

Le moteur d'instances peut désormais cloner une carte d'arène, charger un monde temporaire, accueillir des joueurs et le nettoyer. Cela reste une infrastructure : aucune partie BedWars jouable n'est encore lancée.

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

Au premier démarrage, le plugin crée les dix YAML principaux, `languages/fr_FR.yml`, `languages/en_US.yml` et les dossiers runtime nécessaires, sans écraser les fichiers existants. `/bedwars reload` recharge les configurations et métadonnées, `/bedwars config` affiche un diagnostic sans secret et `/bedwars language set <locale>` persiste la langue. L'alias `/hbw` accepte les mêmes sous-commandes.

`/bedwars map` ouvre l'éditeur guidé des cartes modèles. La bibliothèque propose filtres, tri et création simple ou avancée. Une carte peut ensuite être ouverte et visitée, renommée, typée, réglée, liée à une arène, validée, sauvegardée, archivée, dupliquée ou supprimée avec les protections nécessaires. Le point d'arrivée et l'état « changements à sauvegarder » sont visibles directement. Les commandes détaillées restent disponibles pour l'administration avancée. Les dossiers `instances/game-<UUID>/` contiennent désormais les manifestes temporaires du moteur de parties. Voir [docs/ai/WORLD_SYSTEM.md](docs/ai/WORLD_SYSTEM.md).

Les définitions administratives d'arènes sont stockées dans `arenas/<id>.yml`. `/bedwars setup`, `/bedwars arena` et `/bedwars arena menu` ouvrent l'éditeur complet : création par chat privé, filtres et tri, monde, positions, capacités, équipes générales, limites, validation, activation et suppression sauvegardée. Chaque modification est sauvegardée automatiquement et protégée par une révision contre les écrasements entre administrateurs. Une définition activée peut maintenant produire une instance technique isolée, mais pas encore une partie jouable : équipes BedWars configurables, lits, générateurs et matchmaking restent à venir.

Le framework interne de menus est disponible via `/bedwars gui` ou `/hbw gui` avec `heneriabedwars.admin.gui`. Il démontre navigation, pagination, confirmation, rafraîchissement et protection des inventaires, sans fonctionnalité BedWars.

Pour tester le runtime : activez une arène valide liée à une carte `BEDWARS`, puis utilisez `/bedwars game create <arène>`. Le joueur est téléporté dans le clone en état `WAITING`. `/bedwars game list`, `info`, `join`, `leave` et `destroy` permettent de contrôler l'instance. La destruction supprime automatiquement le monde temporaire.

`/bedwars` sans argument est strictement administratif et exige `heneriabedwars.admin.dashboard`. Un joueur ordinaire ne voit que `game join` et `game leave` dans la complétion. Dans le lobby d'attente, le livre ouvre un menu public d'informations et le lit quitte proprement la partie en restaurant l'état précédent. Le scoreboard est configurable dans `game.yml`, localisé et actualisé sans recréation complète.

Les apparences viennent de `items.yml` : matériaux, quantités, textes directs ou traduits, lore, glow, enchantements sûrs, flags, cuir, têtes hors ligne, custom model data, tags contrôlés et héritage. `/bedwars item list`, `/bedwars item give <clé>` et `/bedwars item preview` permettent de vérifier le registre sans recompilation.
