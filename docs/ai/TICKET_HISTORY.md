# Historique des tickets

## Ticket 007 — Gestionnaire autonome de mondes et cartes modèles

Correctif d'expérience après test en jeu : le point d'entrée joueur est désormais le simple `/bedwars`, qui ouvre un tableau de bord compact. Les listes d'arènes/cartes et les diagnostics utilisent des apparences v2 fusionnées sans écraser les configurations existantes. La création d'arène enchaîne sur le choix de carte et peut créer/associer une carte `BEDWARS` depuis le même écran. Les codes techniques et textes anglais ne sont plus présentés dans la validation.

La validation ne produit plus simultanément `MAP_TEMPLATE_MISSING` et `missing-world`. Une carte modèle valide peut rester déchargée : sa métadonnée est l'autorité administrative, tandis que le monde Bukkit n'est chargé que pour la construction ou la téléportation. Les commandes détaillées restent compatibles pour la console et les usages avancés mais sont masquées de l'aide et de la complétion principale des joueurs.

Deuxième passe après capture de l'éditeur : tous les réglages d'arène restants ont été convertis en assistant continu. L'écran indique la progression et la prochaine action; carte, attente, équipes, joueurs et spectateur sont numérotés. Les sous-menus joueurs/équipes détaillent chaque clic, les limites sont annoncées comme optionnelles et les confirmations réutilisent les nouvelles apparences. De nouvelles clés `assistant-*` garantissent que cette refonte est fusionnée même sur une installation ayant déjà reçu les premières clés v2.

Terminé le 2026-07-16 côté code et validation automatisée. `bedwars-core/map` ajoute modèle immutable, identifiants sûrs, types/états, registre copy-on-write, verrous, ports et service transactionnel. `bedwars-plugin/map` ajoute métadonnées YAML UTF-8 atomiques, fichiers confinés, générateur vide, gestion Bukkit, cycle de vie, commandes et menus.

La création fournit un monde vide préfixé avec plateforme optionnelle et réglages configurables. Chargement, téléportation, spawn, sauvegarde et déchargement restent sur le thread serveur. Duplication et suppression de fichiers sont asynchrones; les copies excluent les données propres au monde/joueur. La suppression exige une sauvegarde complète, refuse les joueurs et vérifie en direct les arènes liées ainsi que le lobby protégé.

Les arènes peuvent référencer une carte `BEDWARS` par `map.template-id`. L'ancien champ `map.template` reste lisible. Les arènes sont la source de vérité; les liens inverses sont resynchronisés. Une carte manquante, du mauvais type ou en erreur empêche la validation administrative.

Configuration : ajout de `worlds.yml`, dixième YAML principal, dossiers `maps/templates`, `maps/metadata`, `instances` et `backups/maps`, items de menus et traductions FR/EN. Les mondes de chunks restent dans le conteneur Bukkit sous `hbw_template_<id>`; les templates du dossier plugin portent un marqueur de propriété. `instances/` reste réservé et aucun gameplay n'est livré.

Validation : 137 tests automatisés réussis, 0 échec et 0 ignoré; Spotless, build propre et Shadow JAR contrôlés lors de la livraison. Tests en jeu non réalisés faute de serveur Minecraft. Commit prévu : `feat(world): add autonomous map template manager`.

## Ticket 006 — Éditeur complet des arènes via menus

Terminé le 2026-07-16 côté code et validation automatisée. L'objectif est de rendre toute la configuration administrative générale d'une arène accessible depuis `/bedwars setup`, `/bedwars arena` et `/bedwars arena menu`, sans créer de gameplay. `ArenaEditorMenuFactory` fournit accueil, diagnostic de configuration, liste paginée, filtres, tri, création, éditeur, mondes, joueurs, équipes générales, limites, validation visuelle et confirmations.

`TextInputService`, `TextInputManager` et `BukkitTextInputService` gèrent une saisie chat privée et bornée par joueur avec validation, mots d'annulation, timeout, déconnexion et arrêt. Le message intercepté est annulé avant traitement sur le thread serveur. `ArenaEditorStateStore` conserve filtre, tri, page et révisions observées. INFO, WARNING, ERROR et CRITICAL ont des items distincts et chaque diagnostic route vers la section concernée.

`ArenaDefinition` persiste une révision initiale 1. Chaque mutation réussie sauvegarde automatiquement, incrémente la révision et publie ensuite le registre; une sauvegarde échouée ne modifie rien. Menus et commandes utilisent le même `ArenaService`; une vue obsolète reçoit `CONFLICT`. Les anciennes arènes sans révision restent compatibles à la révision 1 et les limites peuvent être préparées point par point avant activation.

Configuration : `menus.yml` ajoute les tailles/slots `arena-editor` et les paramètres `text-input`; `items.yml` ajoute les apparences `admin.*`, `arena.*`, `world.*`, `players.*`, `teams.*`, `boundary.*` et `validation.*`; les catalogues FR/EN restent strictement symétriques. Commandes : ajout de `/bedwars setup`, et `/bedwars arena` ouvre désormais la liste pour un joueur. Permissions ajoutées : `heneriabedwars.admin.setup` et `heneriabedwars.admin.arena.teleport`; les permissions d'arène existantes sont revérifiées au clic.

Décisions : saisie par session de chat, autosauvegarde, conflit optimiste par révision, service unique pour commandes/menus, aucune lecture YAML depuis les menus et téléportation dédiée. Les opérations administratives importantes sont journalisées; l'audit persistant reste futur.

Validation : 113 tests automatisés réussis, 0 échec et 0 ignoré. Ils couvrent saisie, révisions, mutations, état d'éditeur, mapping visuel, routage, validation des slots et compatibilité YAML. Aucun serveur Minecraft ni MockBukkit n'a été utilisé : ouverture réelle, inventaires, chat Bukkit, permissions, téléportations, conflit à deux comptes et persistance après redémarrage restent à tester manuellement.

Limitations : aucune partie jouable, copie/reset de monde, équipe BedWars détaillée, couleur d'équipe, lit, générateur, boutique, PNJ ou instance temporaire. La saisie textuelle reste limitée au chat. Prochaine étape : Ticket 007, mondes templates et instances temporaires. Commit prévu : `feat(arena): add complete in-game arena editor`.

## Ticket 005 — Modèle administratif et stockage des arènes

Terminé le 2026-07-16 côté code et validation automatisée. `bedwars-core/arena` ajoute identifiants sûrs, positions/limites pures, définition et métadonnées immutables, six statuts administratifs, diagnostics, validation, registre copy-on-write, port de persistance et service transactionnel. `bedwars-plugin/arena` ajoute le dépôt YAML UTF-8, l'adaptation des mondes/positions Bukkit, le composant de cycle de vie, les commandes et les menus.

Une écriture doit réussir avant publication mémoire. Le reload traite les fichiers indépendamment et préserve une ancienne définition lorsque son YAML devient illisible. Les définitions structurées non activables restent visibles comme `INVALID`. La suppression passe par une confirmation et une sauvegarde datée sous `backups/arenas/`.

Commandes : `/bedwars arena create|list|info|menu|setworld|setwaiting|setspectator|setplayers|setteams|validate|enable|disable|delete`, avec permissions et complétion dédiées. La liste et le détail GUI utilisent uniquement les apparences `arena.*` de `items.yml`; les actions restent en Java.

Validation : 88 tests automatisés réussis, dont 18 nouveaux. Aucun test Paper/MockBukkit ou en jeu n'a été effectué. Les définitions ne lancent aucun gameplay. Prochaine étape : Ticket 006, éditeur complet d'arènes et préparation des mondes/templates. Commit prévu : `feat(arenas): add persistent arena definition system`.

## Ticket 004 — Système complet d'items configurables

Terminé le 2026-07-15 côté code et validation automatisée. `bedwars-core/item` ajoute `ItemKey`, `ItemText`, `ItemDefinition`, `ItemDefinitionTemplate`, `ItemContext`, `ItemRegistry`, `ItemInheritanceResolver` et exceptions/résultat de reload. `bedwars-plugin/item` ajoute `ItemDefinitionLoader`, `BukkitItemFactory`, `BukkitItemService`, `ItemContexts` et `ItemPreviewMenuFactory`.

`items.yml` définit fallback, boutons standards et démonstration. Matériau, quantité, texte direct/traduit, lore, glow, unbreakable, custom model data, flags, enchantements sûrs, cuir, tête, tags autorisés, placeholders requis et héritage sont validés avant activation. Le registre partage le snapshot transactionnel : cycle, parent inconnu ou fallback critique conservent l'ancien état. Les menus ouverts restent visibles et utilisent le nouveau registre au prochain refresh.

Le GUI accepte une clé statique/dynamique ou un `GuiItem`, jamais plusieurs sources. Bordure, boutons standards et démonstration utilisent le registre; les actions restent en Java. Commandes : `/bedwars item`, `list`, `give <clé>`, `preview`. Permissions : `heneriabedwars.admin.item`, `.give`, `.preview`. Le preview est paginé, affiche la clé et permet une copie seulement avec la permission et une place libre.

Validation : 70 tests automatisés réussis après correction d'une limite d'héritage initialement dépendante du cache. Aucun test Paper/MockBukkit ou en jeu n'a été effectué. Limites : pas d'éditeur, gameplay, boutique, arène, resource pack ou texture Base64; propriétaire statique de tête limité aux joueurs en ligne. Cible : Java 21, Spigot API 1.21, Paper 1.21.x. Prochaine étape : Ticket 005, modèle/cycle de vie/stockage des arènes. Commit prévu : `feat(items): add configurable item definition system`.

## Ticket 003 — Framework interne de menus

Terminé le 2026-07-15 côté code et tests automatisés. Le cœur fournit `Gui`, `GuiButton`, `GuiItem`, contextes/actions, `GuiSession`, `GuiSessionManager`, `Pagination`, `GuiSlots`, `ConfirmationGui` et `GuiActionExecutor`. Bukkit fournit `BukkitGuiService`, `GuiInventoryHolder`, `GuiListener`, `GuiItemRenderer`, boutons standards et `DemoMenuFactory`.

Le listener protège clics, Shift, touches numériques, double collecte, drops et drags; quit/kick/disable nettoient les sessions. `sessionId` et `viewId` empêchent une ancienne fermeture de supprimer une nouvelle vue. Une tâche centrale gère les auto-refresh. `/bedwars gui` et `/hbw gui` utilisent `heneriabedwars.admin.gui`; la console est refusée avec traduction.

La démonstration couvre informations, cinq clics, 50 éléments paginés, confirmation, sous-menu, retour, refresh, erreur contrôlée debug et fermeture. Une évolution sauvegarde puis complète les anciens `menus.yml` et catalogues Ticket 002. Total : 57 tests réussis. MockBukkit et tests en jeu non réalisés; validation Paper manuelle requise. Aucun menu métier, item system complet, texte avancé ou API GUI publique. Prochaine étape : Ticket 004, items configurables. Commit prévu : `feat(gui): add reusable inventory menu framework`.

## Correctif Ticket 002 — Migration du `config.yml` Ticket 001

Ajout le 2026-07-15 de `LegacyConfigurationMigrator`. L'ancien format officiel non versionné est identifié par une signature minimale, sauvegardé, complété avec les défauts Ticket 002 puis écrit atomiquement. Les valeurs existantes, clés inconnues et secrets sont préservés sans être journalisés. Les fichiers vides, corrompus ou non reconnaissables restent inchangés et bloquent toujours le démarrage.

Trois tests d'intégration portent le total à 42 et couvrent migration/restart, refus sécurisé et panne d'écriture avec sauvegarde. Le correctif répond à l'échec réel observé lors du passage du JAR Ticket 001 au Ticket 002.

## Ticket 002 — Configuration, messages et traductions

Terminé le 2026-07-15 côté code et validation automatisée. Ajout de neuf YAML principaux, `fr_FR`, `en_US`, création des dossiers runtime, configuration typée, validation, registre, snapshots transactionnels, écritures atomiques, sauvegardes et contrat de migration. Les commandes `reload`, `config`, `language` et `language set` utilisent les traductions, permissions spécialisées et la tab-complétion.

Classes centrales : `ConfigurationService`, `ConfigurationSnapshotFactory`, `ConfigurationRegistry`, `ConfigurationSnapshot`, les cinq records de réglages, `LanguageService`, `TranslationKey`, `MessageRenderer`, `SafeYamlWriter` et `BackupService`.

Validation : 39 tests réussis. Un problème de nettoyage de `@TempDir` dans le bac à sable Windows a été contourné par des dossiers sous `build/test-work`; aucun comportement de production n'est affecté. Tests en jeu non réalisés faute de serveur disponible.

Décisions : YAML Bukkit sans dépendance supplémentaire, records immuables, activation transactionnelle, sous-ensemble MiniMessage compatible Spigot, version 1 obligatoire et secrets masqués. Limites : aucun gameplay/menu/arène/SQL/PlaceholderAPI; boutiques, upgrades et générateurs préparatoires. Prochaine étape : Ticket 003, framework interne de menus.

## Ticket 001 — Initialisation et fondation

### Objectif
Créer un socle Paper compilable, modulaire, testable et documenté sans gameplay BedWars.

### Changements
Ajout du Wrapper Gradle Kotlin DSL, des modules API/cœur/plugin, du bootstrap, du cycle de vie avec rollback, du registre typé, de la journalisation, de la configuration, de `/bedwars version`, de Spotless et de JUnit 5.

### Fichiers principaux
`settings.gradle.kts`, `build.gradle.kts`, les trois scripts de modules, `HeneriaBedWarsPlugin`, `BedWarsBootstrap`, `LifecycleManager`, `ServiceRegistry`, `plugin.yml`, `config.yml`, `AGENTS.md` et `docs/ai/*`.

### Tests
13 tests unitaires passent : 5 pour le registre et 8 pour le cycle de vie. Le build propre, Spotless et Shadow JAR passent avec Java 21.

### Décisions
Java 21, Gradle Kotlin DSL, trois modules, séparation de Paper, bootstrap manuel, registre non nullable, manifeste `plugin.yml` et licence propriétaire temporaire.

### Limitations
Aucun gameplay, stockage, menu ou intégration optionnelle. Le démarrage sur un vrai serveur Paper reste à tester.

### Étape réalisée ensuite
Le Ticket 002 a livré la configuration complète, les messages et traductions sans modifier rétrospectivement le périmètre du Ticket 001.

### Message de commit prévu
`feat(core): initialize modular BedWars plugin foundation`

## Correctif Ticket 001 — Commandes Spigot/Paper

### Objectif
Rendre `/bedwars`, `/hbw` et `/bedwars version` portables entre Spigot 1.21 et Paper 1.21.x.

### Cause identifiée
La commande et son exécuteur existaient déjà, mais le manifeste ciblait `api-version: 1.21.11` et le démarrage ainsi que la commande appelaient `getPluginMeta()`, une API Paper absente du contrat Spigot. La commande racine n'affichait pas l'aide attendue, la complétion ne vérifiait pas la permission et les messages sources contenaient un encodage corrompu.

### Changements
Passage à Spigot API 1.21 en `compileOnly`, utilisation de `getDescription()`, manifeste `api-version: 1.21`, enregistrement Bukkit explicite, aide racine, alias, refus de permission, console, diagnostic et complétion. La construction des messages est testée dans `bedwars-core` sans Bukkit.

### Tests
18 tests automatisés passent, dont 5 consacrés aux commandes. Le build propre et le Shadow JAR passent. Aucun test en jeu n'a été réalisé dans cet environnement.

### Limitation et prochaine action
Remplacer l'ancien JAR, redémarrer complètement Spigot/Paper puis exécuter la matrice de tests en jeu avant de déclarer la compatibilité observée.

### Message de commit prévu
`fix(commands): register BedWars admin command`
