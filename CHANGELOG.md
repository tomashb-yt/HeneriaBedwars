# Changelog

Toutes les évolutions importantes sont consignées ici selon une structure inspirée de Keep a Changelog.

## [Unreleased]

### Added

- Structure Gradle Kotlin DSL à trois modules et Wrapper reproductible.
- Bootstrap initial du plugin Paper et commande `/bedwars version`.
- Gestion déterministe du cycle de vie avec rollback.
- Registre de services typé et configuration générale minimale.
- Tests JUnit 5, formatage Spotless et documentation de reprise IA.

### Fixed

- Enregistrement et comportement de `/bedwars` et de l'alias `/hbw` sur l'API Bukkit classique.
- Compatibilité de la fondation avec Spigot 1.21 et Paper 1.21.x sans appel `getPluginMeta()`.
- Aide principale, permission, console, tab-complétion et encodage des messages de diagnostic.
