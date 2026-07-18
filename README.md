# HeneriaBedWars

## Parcours de configuration simplifié

L'assistant d'arène tient sur cinq lignes et conserve l'essentiel : carte, spawn d'attente, spawn spectateur, format puis équipes colorées directement cliquables pour régler leur spawn et leur lit.

Chaque carte créée possède `plugins/HeneriaBedWars/maps/templates/<id>/import/`. Pour utiliser une carte BedWars téléchargée, copiez le contenu du monde dans ce dossier afin que `level.dat` soit directement sous `import/`, puis utilisez **Importer une carte BedWars** dans la fiche de la carte. Le plugin ferme le monde, sauvegarde l'ancienne version, remplace les fichiers hors thread serveur et recharge la nouvelle carte.

HeneriaBedWars est un plugin BedWars expérimental pour Spigot/Paper 1.21.x, développé en Java 21. Le dépôt contient la fondation technique, la configuration, les menus administratifs, les arènes et un gestionnaire autonome de cartes modèles.

Le moteur d'instances clone une carte, assigne les équipes et exécute maintenant le premier cycle BedWars : lits ennemis, mort avec respawn, mort finale, élimination et victoire provisoire. Cette livraison reste expérimentale tant que la matrice complète n'a pas été validée sur un serveur Paper réel.

La fondation du Ticket 013 définit également les ressources et le calendrier centralisé des futurs générateurs. Elle ne crée pas encore d'items dans le monde et ne propose pas encore leur configuration en menu.

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

Les définitions administratives d'arènes sont stockées dans `arenas/<id>.yml`. `/bedwars setup`, `/bedwars arena` et `/bedwars arena menu` ouvrent l'éditeur complet : création par chat privé, filtres et tri, monde, positions, capacités, équipes générales, limites, validation, activation et suppression sauvegardée. Chaque modification est sauvegardée automatiquement et protégée par une révision contre les écrasements entre administrateurs. Une définition activée produit une instance jouable expérimentale avec équipes, lits, respawns et victoire; générateurs visibles, boutiques et matchmaking restent à venir.

Le framework interne de menus est disponible via `/bedwars gui` ou `/hbw gui` avec `heneriabedwars.admin.gui`. Il démontre navigation, pagination, confirmation, rafraîchissement et protection des inventaires, sans fonctionnalité BedWars.

Pour tester le runtime : activez une arène valide liée à une carte `BEDWARS`, puis utilisez `/bedwars game create <arène>`. Le joueur est téléporté dans le clone en état `WAITING`. `/bedwars game list`, `info`, `join`, `leave` et `destroy` permettent de contrôler l'instance. La destruction supprime automatiquement le monde temporaire.

`/bedwars` sans argument est strictement administratif et exige `heneriabedwars.admin.dashboard`. Un joueur ordinaire ne voit que `game join` et `game leave` dans la complétion. Dans le lobby d'attente, le livre ouvre un menu public d'informations et le lit quitte proprement la partie en restaurant l'état précédent. Le scoreboard est configurable dans `game.yml`, localisé et actualisé sans recréation complète.

Les apparences viennent de `items.yml` : matériaux, quantités, textes directs ou traduits, lore, glow, enchantements sûrs, flags, cuir, têtes hors ligne, custom model data, tags contrôlés et héritage. `/bedwars item list`, `/bedwars item give <clé>` et `/bedwars item preview` permettent de vérifier le registre sans recompilation.
