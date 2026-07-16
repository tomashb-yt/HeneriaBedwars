# État actuel

- Dernier ticket terminé : Ticket 008.
- Version : `0.1.0-SNAPSHOT`.
- Cibles : Java 21, Spigot/Paper 1.21.x.

## Disponible

- éditeur graphique de cartes v4 : bibliothèque paginée avec filtres/tri par administrateur, création simple ou avancée, progression et prochaine action;
- édition guidée du nom, type, point d'arrivée et règles du monde; sauvegarde, ouverture/fermeture, visite, duplication, archive complète et suppression sécurisée;
- associations carte–arène, création d'une arène déjà liée, validation localisée et suivi visible des opérations longues;
- détection des constructions à sauvegarder, nettoyage des sessions à la déconnexion et navigation déterministe;

- tableau de bord administratif v2 ouvert directement par `/bedwars`, commandes avancées masquées de l'aide joueur et création guidée d'une carte `BEDWARS` associée à une nouvelle arène;
- validation visuelle compacte, localisée et orientée action, sans exposer les codes internes; une carte modèle valide n'a pas besoin de rester chargée pour que l'arène soit administrativement valide;
- éditeur d'arène présenté comme un assistant continu avec pourcentage, prochaine action et étapes numérotées; les menus carte, joueurs, équipes, positions, limites, activation et suppression expliquent leurs effets au lieu d'exposer des réglages bruts;
- navigation administrative déterministe : les sous-menus reviennent à l'éditeur, l'éditeur à la liste et la liste au tableau de bord; une suppression d'arène réussie ouvre une nouvelle session au tableau de bord et oublie toute vue ou révision devenue obsolète;
- apparence guidée v3 étendue aux listes vides, au résumé de configuration et à l'éditeur de carte, avec informations utiles, prérequis et effets des clics sans détails techniques superflus;
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

Les 144 tests automatisés passent, sans échec ni test ignoré. Aucun serveur Minecraft n'est disponible dans l'environnement Codex pour certifier les tests en jeu.
