# Changelog

Toutes les évolutions importantes sont consignées ici selon une structure inspirée de Keep a Changelog.

## [Unreleased]

### Added

- Ticket 011 (livraison initiale) : définitions persistantes d'équipes, capacités runtime, sélection métier, `/bw`/`/bedwars play` par carte et déduplication des complétions publiques.

- Correctif 010.1 : menu public d'informations, registre d'actions runtime par PDC et templates configurables du scoreboard d'attente.
- Sessions de sidebar personnelles et stables, placeholders localisés et masquage optionnel des nombres sur Paper avec fallback Spigot.

- Ticket 009 : moteur de `GameInstance`, machine d'état, index par arène/joueur, runtime players/teams, timers/statistiques et événements Java internes.
- Clonage asynchrone des cartes modèles en mondes `hbw_game_*`, téléportation, évacuation, déchargement sans sauvegarde, suppression et récupération des dossiers orphelins.
- Commandes `/bedwars game`, permissions dédiées et API publique immuable `GameApi`/`PlayerGameApi`/`ArenaGameApi` publiée via Bukkit.

- Ticket 008 : éditeur graphique v4 des cartes avec bibliothèque filtrable/triable, création guidée, réglages du monde, point d'arrivée, progression et validation localisée.
- Associations carte–arène dans les deux sens, création d'arène liée, suivi des opérations longues et détection des constructions non sauvegardées.
- Sauvegarde complète manuelle asynchrone, protections des cartes liées/lobby et navigation déterministe.

- Parcours d'administration simplifié : `/bedwars` ouvre directement un tableau de bord visuel, création guidée carte–arène et menus v2 ajoutés sans écraser les personnalisations existantes.
- Assistant continu dans l'éditeur d'arène : progression, prochaine action, étapes carte/attente/équipes/joueurs/spectateur, limites optionnelles et sous-menus explicatifs harmonisés.
- Gestionnaire autonome de cartes modèles : modèle immutable, registre, métadonnées YAML atomiques, mondes vides Bukkit, chargement, sauvegarde, déchargement, téléportation et autosauvegarde optionnelle.
- Duplication confinée hors thread serveur, exclusions des données joueurs, suppression avec sauvegarde complète obligatoire et refus des cartes liées à une arène ou au lobby.
- Commandes et menus `/bedwars map`, permissions détaillées, liaisons arène–carte `BEDWARS`, configuration `worlds.yml` et ressources FR/EN.

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

- Les déconnexions restaurent le snapshot avant la sauvegarde Bukkit; une reconnexion retire exclusivement les items runtime marqués PDC restés d'une ancienne session.

- `/bedwars` et sa complétion ne révèlent plus le tableau de bord ni les commandes de configuration aux joueurs ordinaires.
- Les items quitter/informations répondent aux clics air/bloc, gauche/droit, ignorent l'off-hand et refusent les anciens identifiants d'instance.
- Le scoreboard n'affiche plus les enums runtime bruts et n'efface plus toutes ses lignes à chaque rafraîchissement.

- Validation des arènes sans doublon carte/monde, compatibilité des cartes modèles déchargées et diagnostics entièrement reformulés dans la langue active sans codes techniques visibles.

- Enregistrement et comportement de `/bedwars` et de l'alias `/hbw` sur l'API Bukkit classique.
- Compatibilité de la fondation avec Spigot 1.21 et Paper 1.21.x sans appel `getPluginMeta()`.
- Aide principale, permission, console, tab-complétion et encodage des messages de diagnostic.
- Migration au démarrage du `config.yml` officiel Ticket 001 sans version, avec sauvegarde, fusion des nouveaux défauts et préservation des clés personnalisées.
