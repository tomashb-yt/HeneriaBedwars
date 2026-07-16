# Changelog

Toutes les évolutions importantes sont consignées ici selon une structure inspirée de Keep a Changelog.

## [Unreleased]

### Added

- Structure Gradle Kotlin DSL à trois modules et Wrapper reproductible.
- Bootstrap initial du plugin Paper et commande `/bedwars version`.
- Gestion déterministe du cycle de vie avec rollback.
- Registre de services typé et configuration générale minimale.
- Tests JUnit 5, formatage Spotless et documentation de reprise IA.
- Neuf fichiers YAML versionnés, deux catalogues de langue et création sûre des dossiers d'exécution.
- Configurations Java immuables, validation structurée et activation transactionnelle par snapshot.
- Rendu Spigot des couleurs MiniMessage usuelles, couleurs hexadécimales, codes legacy et placeholders sûrs.
- Commandes `/bedwars reload`, `/bedwars config` et `/bedwars language set <locale>`, permissions et complétion associées.
- Écriture YAML protégée, sauvegardes anti-collision et contrat de migrations futures.
- Framework GUI interne : modèle immutable, sessions par joueur/vue, navigation limitée, pagination, confirmations, cooldowns, sons et rafraîchissement central.
- Protection Bukkit des clics, drags, drops, touches numériques, déconnexions et arrêts, avec démonstration `/bedwars gui`.
- Registre d'items immuable et transactionnel, clés logiques, fallback, validation, héritage avec cycles/profondeur et rendu Bukkit indépendant par construction.
- Propriétés configurables d'items, textes traduits et placeholders sûrs, PDC contrôlé, intégration complète au GUI et commandes `/bedwars item` avec menu de prévisualisation.
- Modèle immutable des arènes, identifiants sûrs, statuts administratifs, validation structurée, registre atomique et service transactionnel.
- Stockage UTF-8 `arenas/<id>.yml`, écritures atomiques, reload partiel préservant les anciennes définitions illisibles et sauvegarde datée obligatoire avant suppression.
- Commandes et menus `/bedwars arena`, permissions détaillées, complétion des identifiants/mondes et items d'interface configurables FR/EN.
- Éditeur administratif complet via `/bedwars setup` et `/bedwars arena` : création, filtres, tri, monde, positions, capacités, équipes générales, limites, validation visuelle, activation et suppression.
- Saisie textuelle privée par session de chat avec validation, annulation, délai d'expiration et nettoyage à la déconnexion ou à l'arrêt.
- Sauvegarde automatique des arènes et révisions optimistes empêchant une vue obsolète d'écraser une modification plus récente.

### Fixed

- Enregistrement et comportement de `/bedwars` et de l'alias `/hbw` sur l'API Bukkit classique.
- Compatibilité de la fondation avec Spigot 1.21 et Paper 1.21.x sans appel `getPluginMeta()`.
- Aide principale, permission, console, tab-complétion et encodage des messages de diagnostic.
- Migration au démarrage du `config.yml` officiel Ticket 001 sans version, avec sauvegarde, fusion des nouveaux défauts et préservation des clés personnalisées.
