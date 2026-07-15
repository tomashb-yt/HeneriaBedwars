# Historique des tickets

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
