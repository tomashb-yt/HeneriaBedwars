# État actuel

## Ticket 014 — boutiques et achats en validation

- emplacement et orientation de la boutique configurables dans la fiche de chaque équipe;
- PNJ villageois protégé créé dans le clone et identifié par partie/équipe via PDC;
- catalogue `shops.yml` configurable avec quatorze offres réparties en quatre catégories;
- menu compact affichant prix, quantité, solde et disponibilité, entièrement FR/EN;
- achats limités aux joueurs vivants en `PLAYING`, refusés aux spectateurs;
- échange atomique : aucune monnaie consommée si l'inventaire ne peut pas recevoir l'article;
- événement Java interne `ShopPurchaseEvent` après réussite;
- validation Paper réelle encore requise; armures, outils évolutifs et améliorations restent au Ticket 015.

## Ticket 013 — générateurs configurables en validation

- ressources `IRON`, `GOLD`, `DIAMOND` et `EMERALD` indépendantes de Bukkit;
- définition runtime immuable avec niveau, intervalle, quantité, capacité et empilement;
- état par `GameInstance`, calendrier déterministe et métriques d'émission/blocage;
- coordinateur global borné avec rotation équitable et sans tâche par générateur;
- positions administratives persistées dans chaque YAML d'arène et conservées par révision optimiste;
- assistant graphique pour ajouter, visiter, déplacer ou supprimer les quatre ressources;
- fer et or partageables sur le même bloc, avec doublons d'une même ressource toujours refusés;
- initialisation dans le clone et drops Bukkit uniquement pendant `PLAYING`, avec capacité locale et fusion des piles;
- validation Paper réelle encore requise; hologrammes, sons, particules et niveaux automatiques restent futurs.

## Ticket 012 — implémentation en validation

- sélection administrative enregistrant pied, tête et direction du lit;
- index runtime des deux blocs, destruction atomique et protection du lit allié;
- blocs de carte protégés pendant `PLAYING`, explosions filtrées sur les lits;
- mort avec lit vivant → spectateur temporaire → respawn centralisé → protection;
- lit détruit → mort finale → spectateur permanent → élimination d'équipe;
- dernière équipe active → `ENDING` → annonce → restauration et suppression du clone;
- scoreboard de jeu et statistiques runtime kills/final kills/lits;
- scoreboard v7 plus lisible avec couleur réelle de l'équipe et état vivant/détruit du lit;
- destruction du lit signalée aux victimes par broadcast, message personnel, titre, actionbar et son;
- validation multijoueur Paper encore requise, donc Ticket 012 non déclaré définitivement terminé.

## Correctif équipes et lancement — 2026-07-17

- couleurs des cartes et titres identiques à la couleur métier de chaque équipe;
- état prêt rendu avec des traductions déjà garanties dans les catalogues existants;
- messages distincts pour visiter le spawn ou le lit d'une équipe;
- fiche équipe sur cinq lignes, guide central et deux colonnes d'actions toujours alignées;
- téléportation de chaque joueur au spawn de son équipe lors de `STARTING → PLAYING`;
- aucune destruction de lit, réapparition, élimination ou victoire n'est ajoutée par ce correctif.

## Correctif UX et import de cartes — 2026-07-17

- assistant principal d'arène réduit à cinq lignes et centré sur carte, attente, spectateur et équipes;
- huit équipes colorées configurables directement depuis la vue principale;
- dossier `maps/templates/<id>/import/` créé pour chaque carte avec instructions locales;
- remplacement de monde BedWars depuis le menu, sauvegarde préalable, copie asynchrone, rollback et rechargement;
- tests automatisés dédiés au stockage et au cycle d'import; validation Paper réelle encore requise.

- Dernier ticket terminé : Ticket 011. Les Tickets 012 à 014 sont implémentés et restent en validation Paper.
- Version : `0.1.0-SNAPSHOT`.
- Cibles : Java 21, Spigot/Paper 1.21.x.

## Disponible

- Ticket 014 : configuration guidée d'un PNJ de boutique par équipe, apparition dans le clone, menu d'achats par catégories et catalogue YAML rechargeable;

- correctif de préparation du Ticket 012 : menu général des équipes avec résumé des spawns/lits, fiche colorée par équipe, sauvegarde automatique et retour sur la même fiche;
- actions GUI distinctes pour définir, visiter et retirer le spawn; sélection d'un vrai lit regardé à moins de 8 blocs, contrôle des deux moitiés, du monde modèle et des doublons;
- commandes de secours `/bedwars arena team list|setspawn|clearspawn|teleport|setbed|clearbed|teleportbed`, aide et autocomplétion des arènes/équipes;

- Correctif 010.1 : `/bedwars` strictement réservé au dashboard administrateur, aide publique adaptée et complétion joueur limitée à `game join|leave`;
- items d'attente interactifs identifiés par PDC avec UUID d'instance, filtrage off-hand, cooldown et protection contre les anciens items;
- menu public d'informations uniquement composé du résumé, de l'état, de la liste des joueurs et de la fermeture; le départ reste sur l'item rouge et `GameLobbyService`;
- scoreboard configurable/localisé, lignes stables sans recréation, labels runtime lisibles et masquage des nombres quand Paper le permet;

- Ticket 010 : lobby d'attente runtime `WAITING`/`STARTING`, compte a rebours automatique, annulation au-dessous du minimum, acceleration a partie pleine, passage controle vers `PLAYING`, nettoyage d'instance vide et arret administratif;
- snapshots joueurs uniquement en memoire avec restauration de la position, inventaire, etat et scoreboard avant sortie; protections et rescue du vide limites aux membres d'un lobby runtime;
- scoreboard, bossbar configurable, titres, actionbar, sons, items quitter/information et menu d'administration des parties;
- `game.yml`, ids courts non ambigus, commandes `game start|stop` et permissions associees.

- moteur `GameInstance` avec UUID, machine d'état complète, monde temporaire, joueurs, équipes, timers et statistiques en mémoire;
- `GameInstanceManager` indexé par partie, joueur et arène, empêchant les doubles occupations et compensant les créations échouées;
- clonage asynchrone des cartes `BEDWARS`, chargement Bukkit sur le thread serveur, téléportation au point d'attente, évacuation, déchargement et suppression;
- événements Java internes de création, attente, démarrage, fin, destruction, entrée et sortie;
- API publique immuable enregistrée dans le `ServicesManager` Bukkit et commandes `/bedwars game`;
- une arène active peut être lancée/rejointe depuis son éditeur en un clic; `/bedwars game join <arène>` crée également l'instance automatiquement si nécessaire;

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
- API publique de statut et vues immuables des parties, joueurs et arènes runtime;
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

Le cycle automatisé du Ticket 012 et les générateurs du Ticket 013 sont implémentés mais restent en validation Paper multijoueur. Les boutiques, PNJ, achats, améliorations, reconnexion, base de données, resource pack et PlaceholderAPI ne sont pas encore disponibles.

Les 187 tests automatisés passent, sans échec, erreur ni test ignoré. Aucun serveur Minecraft n'est disponible dans l'environnement Codex pour certifier les tests en jeu.
## Ticket 011 - livraison initiale

Les arènes portent désormais des définitions d'équipes immuables et persistantes (identifiant, nom, couleur, ordre, capacité et spawn). Les anciens YAML à base de `teams.count` et `teams.players-per-team` sont relus avec des équipes générées déterministes, sans perdre la définition; les spawns restent à renseigner avant validation complète.

Le runtime construit ses équipes depuis l'arène, respecte les capacités et permet une sélection métier uniquement pendant `WAITING`. Le parcours public `/bw` et `/bedwars play` résout une carte de façon insensible à la casse, choisit l'instance rejoignable la plus peuplée et ne complète jamais les UUID ou identifiants courts. `/bw leave` conserve la sortie sûre existante.

Au quit/kick, le snapshot est restauré avant la sauvegarde du profil Bukkit. Au join, une protection retire seulement les items portant le PDC `heneriabedwars:runtime_item` lorsqu'aucune instance ne correspond au joueur; les objets normaux ne sont pas ciblés.
