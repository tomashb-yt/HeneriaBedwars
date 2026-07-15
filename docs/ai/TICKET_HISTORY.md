# Historique des tickets

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

### Prochaine étape
Ticket 002 — configuration complète, messages et traductions. Ne pas commencer ce périmètre dans le Ticket 001.

### Message de commit prévu
`feat(core): initialize modular BedWars plugin foundation`
