# État actuel

- Dernier ticket terminé : Ticket 007.
- Version : `0.1.0-SNAPSHOT`.
- Cibles : Java 21, Spigot/Paper 1.21.x.

## Disponible

- gestion autonome de cartes modèles `LOBBY`, `BEDWARS` et `GENERIC`, identifiants sûrs, métadonnées immutables/révisionnées et registre ordonné;
- création de mondes vides préfixés, plateforme de sécurité configurable, chargement, téléportation, spawn, sauvegarde, déchargement et autosauvegarde centrale optionnelle;
- duplication asynchrone avec exclusions, suppression asynchrone après sauvegarde complète, confinement des chemins et refus des liens symboliques;
- commandes et menus `/bedwars map`; association des arènes à une carte `BEDWARS` via `/bedwars arena setmap` et l'éditeur;

- bootstrap et cycle de vie déterministes;
- API publique minimale de statut;
- création non destructive de neuf YAML principaux, deux langues et trois dossiers runtime;
- validation INFO/WARNING/ERROR/CRITICAL, défauts contrôlés et masquage des secrets;
- réglages immuables `PluginSettings`, `GameplaySettings`, `LobbySettings`, `StorageSettings` et `MenuSettings`;
- reload transactionnel et accès par registre;
- messages FR/EN, couleurs nommées/hex/legacy et placeholders non interprétés;
- commandes `version`, `reload`, `config`, `language` et `language set`, aussi via `/hbw`;
- permissions et complétion filtrée;
- sauvegarde et contrat de migration prêts, sans migration artificielle.
- migration réelle et ciblée du `config.yml` officiel Ticket 001 non versionné vers la version 1, avec sauvegarde préalable et fusion non destructive des défauts.
- framework GUI interne fonctionnel : modèle immutable, sessions, holder robuste, navigation, pagination, confirmations, refresh, sons, permissions, anti-spam et erreurs centralisées;
- menu de démonstration administrateur via `/bedwars gui` et `/hbw gui`.
- registre d'items immuable intégré au snapshot de configuration, définitions YAML, validation, fallback, héritage borné, placeholders, traductions et construction Bukkit;
- chaque rendu produit un `ItemStack` distinct avec glow, enchantements sûrs, flags, unbreakable, custom model data, cuir, tête de joueur contextuelle et PDC contrôlé;
- le framework GUI et sa démonstration utilisent des clés d'items; `/bedwars item list|give|preview` fournit les outils administratifs de test.
- définitions d'arènes immutables, registre atomique, validation monde/positions/capacités et statuts administratifs distincts du cycle des parties;
- stockage `arenas/<id>.yml`, écriture atomique, reload tolérant fichier par fichier et sauvegarde datée obligatoire avant suppression;
- commandes `/bedwars arena create|list|info|menu|setworld|setwaiting|setspectator|setplayers|setteams|validate|enable|disable|delete`, permissions, complétion et menus administratifs.
- menu principal `/bedwars setup` et liste `/bedwars arena` avec pagination, filtres, tri, actualisation et création par saisie chat privée;
- éditeur général des arènes : nom coloré, monde, lobby d'attente, spectateur, capacité, équipes générales, limites, validation visuelle, activation et suppression;
- sauvegarde automatique après chaque modification, révision optimiste et refus des vues obsolètes;
- téléportations administratives protégées par permission et persistance des modifications après redémarrage.

## Non disponible

Aucune équipe BedWars détaillée ou couleur d'équipe, aucune instance temporaire de partie ou remise à zéro après match, aucun lit, générateur actif, boutique, PNJ, achat, amélioration, lobby protégé, base de données, resource pack ou PlaceholderAPI. Les cartes sont des modèles administratifs persistants, pas des parties jouables.

Les 136 tests automatisés passent, sans échec ni test ignoré. Aucun serveur Minecraft n'est disponible dans l'environnement Codex pour certifier les tests en jeu.
