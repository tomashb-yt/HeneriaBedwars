# Guide de reprise — HeneriaBedWars

## Présentation

HeneriaBedWars est un futur plugin BedWars modulaire pour Spigot/Paper 1.21.x. Il utilise Java 21, Gradle Kotlin DSL et le package racine `fr.heneria.bedwars`. La version actuelle est `0.1.0-SNAPSHOT`. Les Tickets 001 à 008 livrent la fondation, les configurations, traductions, menus, items, arènes et l'éditeur guidé complet des cartes modèles ; aucun gameplay n'est encore actif.

## Lecture obligatoire

Avant chaque ticket, lire dans cet ordre :

1. `AGENTS.md` ;
2. `docs/ai/PROJECT_CONTEXT.md` ;
3. `docs/ai/ARCHITECTURE.md` ;
4. `docs/ai/CURRENT_STATE.md` ;
5. `docs/ai/ROADMAP.md` ;
6. `docs/ai/DECISIONS.md` ;
7. `docs/ai/CONFIGURATION.md` ;
8. `docs/ai/KNOWN_ISSUES.md` ;
9. les dernières entrées de `docs/ai/TICKET_HISTORY.md`.

Inspecter ensuite Git, tous les Markdown pertinents et les fichiers touchés. Ne supprimer ni écraser une ressource existante sans analyse. Vérifier qu'aucun secret n'est ajouté.

## Commandes utiles

```bash
./gradlew clean
./gradlew build
./gradlew test
./gradlew shadowJar
./gradlew spotlessCheck
./gradlew spotlessApply
```

Sous Windows, remplacer `./gradlew` par `.\gradlew.bat`. Le JAR déployable est produit par `:bedwars-plugin:shadowJar`.

Commandes disponibles : `/bedwars` ou `/hbw`, puis `version`, `reload`, `config`, `language`, `gui`, `item`, `arena`, `setup` et `map`. Les permissions sont documentées dans `docs/ai/API.md`. Après modification du manifeste, remplacer le JAR et redémarrer complètement le serveur ; `/bedwars reload` recharge les configurations, items, métadonnées de cartes et arènes sans charger automatiquement les mondes.

Le Ticket 007 sépare les métadonnées `maps/metadata/`, les marqueurs de propriété `maps/templates/`, les mondes de travail Bukkit préfixés dans le conteneur de mondes du serveur et les sauvegardes `backups/maps/`. Ne jamais accepter un chemin arbitraire ou suivre un lien symbolique. Toute suppression passe par `MapTemplateService.prepareDelete` sur le thread serveur puis `completeDelete` hors thread ; les relations d'arènes actives sont la source de vérité.

Le Ticket 005 stocke une définition UTF-8 par fichier dans `arenas/`. Une suppression doit toujours passer par `ArenaRepository.deleteWithBackup`. Un fichier illisible au reload conserve sa définition active connue, tandis qu'une arène structurée mais invalide reste visible avec le statut `INVALID`.

Au démarrage, l'ancien `config.yml` officiel du Ticket 001 sans `config-version` est reconnu par sa signature minimale, sauvegardé puis migré vers la version 1. Ne jamais élargir cette détection à n'importe quel YAML non versionné : un fichier vide, corrompu ou non reconnaissable doit rester intact et être refusé.

Le Ticket 008 centralise l'administration des cartes dans le menu v4. Les commandes restent disponibles pour le diagnostic avancé. Toute sauvegarde complète, duplication ou suppression de dossier doit rester asynchrone, visible dans `MapOperationTracker` et protégée par `MapOperationLock`. Les associations d'arènes restent la source de vérité et les états d'éditeur sont nettoyés à la déconnexion et à l'arrêt.

## Règles architecturales

- `bedwars-api` ne dépend ni de Paper, ni de `bedwars-core`, ni de `bedwars-plugin`.
- `bedwars-core` peut dépendre de l'API mais doit rester indépendant de Bukkit/Paper.
- `bedwars-plugin` est la seule frontière Bukkit et peut dépendre des deux autres modules.
- Aucune logique métier importante dans les listeners, commandes ou classe principale.
- Aucune requête SQL dans un menu et aucune opération lourde sur le thread serveur.
- Aucun accès d'une API publique aux implémentations internes.
- Aucune nouvelle dépendance sans justification dans `DECISIONS.md`.
- Aucune modification silencieuse d'une API publique.
- Aucun `TODO` sans référence de ticket, aucune fonctionnalité dupliquée et aucune exception importante ignorée.
- Préférer l'injection par constructeur ; ne pas introduire de singleton global mutable.

## Définition de terminé

Un ticket est terminé uniquement si le code compile, tous les tests et le formatage passent, le JAR attendu est produit, la documentation et la roadmap reflètent le code réel, l'historique du ticket et les décisions sont à jour, aucun secret ou fichier temporaire n'est ajouté et `git diff --check` ne remonte aucune erreur.
