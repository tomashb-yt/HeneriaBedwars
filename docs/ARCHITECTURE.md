# HeneriaBedWars — Architecture cible

Statut : proposition d’architecture, phase 0  
Date : 12 juillet 2026  
Périmètre : Paper 1.21.11, Java 21

## 1. Cadrage vérifié

Le dossier de travail était entièrement vide au début de cette phase : aucun dépôt Git, système de build, code, ressource ou test n’existait. Java Temurin 21.0.8 est disponible. Gradle et Maven ne sont pas installés globalement ; le projet devra donc fournir son propre Gradle Wrapper.

Le socle retenu est :

- Java 21 avec toolchain et release fixés à 21 ;
- Paper API 1.21.11-R0.1-SNAPSHOT ;
- api-version 1.21.11 ;
- Gradle multi-projet en Kotlin DSL ;
- plugin.yml classique ;
- un seul JAR final déployable ;
- aucune dépendance NMS ;
- Adventure et MiniMessage pour les composants texte ;
- API Paper native Mannequin pour les PNJ avec profil et skin.

Paper 1.21.11 est la dernière révision de la famille 1.21 et utilise Java 21. Compiler contre 1.21.11 signifie que la cible garantie est 1.21.11, et non toutes les versions 1.21.0 à 1.21.11. Une compatibilité dès 1.21.0 imposerait une compilation contre la première API et ferait perdre des API récentes, notamment le support natif choisi pour les PNJ.

Le manifeste expérimental paper-plugin.yml n’est pas nécessaire. Les commandes Brigadier seront enregistrées depuis JavaPlugin avec le Lifecycle API de Paper.

Références officielles :

- https://docs.papermc.io/paper/getting-started/
- https://docs.papermc.io/paper/dev/project-setup/
- https://docs.papermc.io/paper/dev/command-api/basics/registration/
- https://docs.papermc.io/paper/dev/scheduler/
- https://jd.papermc.io/paper/1.21.11/

## 2. Invariants d’architecture

1. Le métier ne dépend jamais de Bukkit ou Paper.
2. Aucun objet Player, World, Entity, Inventory, Location ou ItemStack ne traverse une opération asynchrone.
3. Toutes les mutations d’une partie sont sérialisées sur le thread serveur.
4. JDBC, fichiers, copie de mondes, compression et résolution distante s’exécutent hors du thread serveur.
5. Une partie utilise des révisions immuables d’arène, de mode et de catalogues.
6. Un rechargement ne modifie jamais les règles, prix ou timings d’une partie déjà lancée.
7. Toute ressource temporaire appartient à un scope fermé de façon idempotente.
8. Les index de longue durée stockent des UUID et identifiants métier, jamais des objets Paper.
9. Aucun générateur, joueur, hologramme ou cosmétique ne possède sa propre tâche répétitive.
10. Toute écriture de résultat ou de récompense est idempotente.
11. Une arène invalide ne peut jamais être publiée ni allouée.
12. Aucun service global mutable ni service locator statique n’est autorisé.

L’injection se fait par constructeurs. Le composition root assemble explicitement les implémentations, sans Spring, scan de classpath ou réflexion implicite.

## 3. Organisation Gradle

Le dépôt contiendra les projets suivants :

    bedwars-model
    bedwars-api
    bedwars-spi
    bedwars-domain
    bedwars-application
    bedwars-storage-sql
    bedwars-content-standard
    bedwars-content-signature
    bedwars-platform-paper
    bedwars-bootstrap
    bedwars-testkit

### bedwars-model

Types immuables partagés, sans logique de plateforme :

- ArenaId, ArenaRevisionId, MatchId, PlayerId, TeamId ;
- ModeKey, FeatureKey et identifiants namespacés ;
- BlockPoint, Pose, Cuboid, WorldRef ;
- durées, couleurs, résultats typés et erreurs structurées.

### bedwars-api

Façade stable destinée aux consommateurs :

- BedWarsApi ;
- ArenaService, MatchService, ProfileService, LeaderboardService ;
- snapshots immuables ;
- événements publics avant/après les actions autorisées ;
- version sémantique de l’API.

L’API ne retourne aucun agrégat mutable et ne donne jamais accès aux registres internes.

### bedwars-spi

Contrats d’extension destinés aux modes et contenus :

- ModeProvider et ModeRegistrar ;
- RulesetFactory ;
- WinCondition, RespawnPolicy, EliminationPolicy ;
- ArenaRequirementProvider ;
- ItemProvider et ItemBehaviorFactory ;
- UpgradeProvider ;
- GeneratorProvider ;
- TimelineEventProvider ;
- CosmeticProvider ;
- ChallengeProvider.

Les identifiants sont namespacés, par exemple heneria:classic ou heneria:gravity_bomb. Les registres rejettent les doublons et dépendances manquantes, puis deviennent immuables avant l’ouverture des parties.

Les contenus internes sont enregistrés explicitement par le composition root. Pour un addon externe, BedWarsApi est publié avec ServicesManager ; l’addon déclare HeneriaBedWars comme dépendance et remet son ModeProvider pendant la fenêtre d’enregistrement. Cette fenêtre se ferme au ServerLoadEvent, après validation de toutes les dépendances. Une inscription tardive est refusée avec une erreur explicite. Chaque partie crée ensuite ses propres instances de features : aucun état de match mutable n’est partagé entre modes ou parties.

### bedwars-domain

Cœur Java pur :

- arènes et révisions ;
- équipes, participants et lits ;
- machine à états d’une partie ;
- générateurs et échéances ;
- achats et améliorations ;
- statistiques temporaires ;
- profils, progression et récompenses ;
- défis, succès et cosmétiques ;
- validation pure des configurations.

### bedwars-application

Cas d’usage et orchestration :

- création, édition, validation, publication et suppression d’arène ;
- file d’attente, allocation, join, leave et reconnexion ;
- start, force start, stop et fin automatique ;
- achat, activation d’objet et upgrade ;
- sauvegarde et projections ;
- administration et diagnostics.

Les ports sortants couvrent la base, les mondes, les joueurs, l’UI, l’horloge, le scheduling, les skins, la télémétrie et les journaux d’audit.

### bedwars-storage-sql

Adaptateurs JDBC pour SQLite et MySQL :

- migrations versionnées ;
- transactions ;
- repositories ;
- batchs de statistiques ;
- ledger de récompenses ;
- projections de leaderboard ;
- checkpoints de reconnexion.

### bedwars-content-standard

Contenu BedWars classique, enregistré exclusivement via le SPI :

- règles classiques ;
- ressources et générateurs ;
- équipes standard ;
- timeline Diamond II/III, Emerald II/III, destruction des lits, Sudden Death et dragons ;
- shop, upgrades et objets classiques.

### bedwars-content-signature

Contenu propre à Heneria, isolé du noyau :

- Portable Forge ;
- Builder Wand ;
- Gravity Bomb ;
- Ice Wand ;
- Smoke Bomb ;
- Healing Beacon ;
- Shockwave Pearl ;
- Explosive Arrow ;
- Anti Bridge Trap ;
- Builder Boots.

Cette séparation prouve qu’un nouveau contenu peut être ajouté sans modifier le moteur.

### bedwars-platform-paper

Adaptateurs Paper :

- listeners globaux ;
- commandes Brigadier ;
- GUI et sessions d’édition ;
- inventaires, PDC et objets spéciaux ;
- mondes, blocs, entités et téléportations ;
- PNJ Mannequin, Interaction et TextDisplay ;
- scoreboard, BossBar et TAB ;
- sons, particules et hologrammes ;
- mode spectateur ;
- dispatcher vers le thread serveur.

### bedwars-bootstrap

Point d’entrée très mince :

- HeneriaBedWarsPlugin ;
- BedWarsCompositionRoot ;
- PluginLifecycleCoordinator ;
- RuntimeConfiguration ;
- publication de BedWarsApi via ServicesManager ;
- construction du JAR final.

### bedwars-testkit

Outils de test réutilisables :

- horloge et scheduler déterministes ;
- repositories mémoire ;
- faux monde et faux joueurs métier ;
- builders d’arènes et parties ;
- assertions de fermeture des scopes ;
- scénarios de charge.

### Graphe de dépendances

Les flèches suivantes signifient « dépend de » :

    api -----------------------> model
    spi -----------------------> model
    domain --------------------> model, spi
    application ---------------> model, api, spi, domain
    content-standard ----------> model, spi, domain
    content-signature ---------> model, spi, domain
    storage-sql ---------------> model, domain, application
    platform-paper ------------> model, api, spi, domain, application
    bootstrap -----------------> tous les modules d’exécution
    testkit -------------------> model, spi, domain, application

Les modules model, spi et domain ne voient jamais Paper. Les frontières seront vérifiées automatiquement par ArchUnit.

## 4. Packages

Le namespace racine sera fr.heneria.bedwars.

    fr.heneria.bedwars.model
    fr.heneria.bedwars.api.arena
    fr.heneria.bedwars.api.match
    fr.heneria.bedwars.api.profile
    fr.heneria.bedwars.api.event
    fr.heneria.bedwars.spi.mode
    fr.heneria.bedwars.spi.rule
    fr.heneria.bedwars.spi.item
    fr.heneria.bedwars.spi.upgrade
    fr.heneria.bedwars.spi.timeline
    fr.heneria.bedwars.spi.cosmetic
    fr.heneria.bedwars.spi.validation
    fr.heneria.bedwars.domain.arena
    fr.heneria.bedwars.domain.match
    fr.heneria.bedwars.domain.team
    fr.heneria.bedwars.domain.generator
    fr.heneria.bedwars.domain.shop
    fr.heneria.bedwars.domain.profile
    fr.heneria.bedwars.domain.progression
    fr.heneria.bedwars.domain.challenge
    fr.heneria.bedwars.domain.cosmetic
    fr.heneria.bedwars.application.port.in
    fr.heneria.bedwars.application.port.out
    fr.heneria.bedwars.application.usecase
    fr.heneria.bedwars.application.orchestration
    fr.heneria.bedwars.storage.sql
    fr.heneria.bedwars.paper.command
    fr.heneria.bedwars.paper.listener
    fr.heneria.bedwars.paper.gui
    fr.heneria.bedwars.paper.world
    fr.heneria.bedwars.paper.npc
    fr.heneria.bedwars.paper.hud
    fr.heneria.bedwars.bootstrap

Chaque package public aura un package-info documenté. Les classes décrivent leurs invariants et responsabilités ; les commentaires ne répètent pas mécaniquement le code.

## 5. Modèle d’arène

Arena est un agrégat stable identifié par ArenaId. Son nom d’affichage est séparé de son slug technique afin d’éviter les collisions et traversées de chemins.

Une arène possède :

- un brouillon éditable ;
- des ArenaRevision immuables ;
- au plus une révision publiée ;
- une version optimiste ;
- une référence et une empreinte du monde modèle ;
- une politique d’instanciation ;
- un état opérationnel.

États opérationnels :

    DRAFT -> PUBLISHED -> DISABLED -> PUBLISHED
       \---------------------------> DELETED

La validité n’est pas un booléen que l’administrateur peut forcer. Elle est calculée pour une révision. La publication atomique n’est autorisée que si la validation ne contient aucune erreur.

Une partie capture la révision publiée. Une modification ultérieure ne peut donc pas altérer une partie active.

## 6. Setup entièrement en jeu

La commande /bw setup <arène> ouvre une SetupSession et remet une Nether Star « Configurateur BedWars ».

L’outil est identifié par Persistent Data Container avec :

- type d’outil ;
- ArenaId ;
- identifiant de session ;
- nonce.

Le nom et le lore ne servent jamais à l’authentification. Des listeners globaux empêchent drop, déplacement, stockage, craft, échange, perte à la mort et utilisation sans permission. Si l’objet disparaît malgré tout pendant une session valide, il est recréé. Aucune référence Player n’est conservée dans SetupSession.

Le menu principal contient :

- informations et état ;
- joueurs minimum/maximum ;
- nombre et taille des équipes ;
- lobby principal, attente, spectateur et zone d’attente ;
- équipes ;
- générateurs ;
- PNJ ;
- bordures, hauteur et profondeur ;
- mode et catalogues ;
- timeline ;
- rapport de validation ;
- capturer le monde modèle ;
- publier ou désactiver.

Les équipes Rouge, Bleu, Vert, Jaune, Aqua, Blanc, Rose et Gris sont créées par défaut. Chaque écran configure spawn, lit, forge, couleur, armure et cuir.

Chaque action suit ce flux :

    GUI -> commande applicative -> modification du draft
        -> validation ciblée -> autosave asynchrone

Deux administrateurs ne peuvent pas modifier simultanément la même révision. Un lease d’édition avec expiration contrôlée évite les écrasements. La sauvegarde utilise aussi une version optimiste.

## 7. Validation

ArenaValidationEngine agrège :

- validateurs génériques ;
- exigences du mode ;
- inspection du monde Paper ;
- contrôle de l’empreinte du template.

ValidationIssue contient :

- code stable ;
- chemin de propriété ;
- sévérité INFO, WARNING ou ERROR ;
- clé de traduction ;
- détails structurés ;
- correction suggérée lorsqu’elle est sûre.

Contrôles principaux :

- monde modèle présent et capturable ;
- spawns présents, sûrs et dans les limites ;
- zone d’attente cohérente ;
- spawn spectateur présent ;
- nombre d’équipes et capacité cohérents ;
- lit valide pour chaque équipe ;
- forge d’équipe présente ;
- générateurs centraux requis et types valides ;
- Shop et Upgrade Shop présents par équipe ;
- positions distinctes et chunks disponibles ;
- bordure, hauteur maximale et profondeur cohérentes ;
- timeline et catalogues résolus ;
- aucune clé d’extension manquante.

Les contrôles purs s’exécutent hors du thread serveur sur un snapshot. Les contrôles de blocs et mondes reviennent sur le thread Paper. Une arène invalide n’apparaît jamais dans l’allocateur.

## 8. Parties et multiarène

La machine à états principale est :

    PROVISIONING
       -> WAITING
       -> COUNTDOWN
       -> STARTING
       -> PLAYING
       -> ENDING
       -> RESTORING
       -> TERMINATED

FAILED est une sortie contrôlée depuis les états applicables. start, stop, finish et close sont idempotents.

Le sudden death est une phase de gameplay au sein de PLAYING, pilotée par la timeline, et non une transition qui contourne les règles de fin.

Composants d’orchestration :

- MatchDirectory : MatchId vers MatchHandle ;
- PlayerMatchIndex : UUID vers MatchId ;
- ArenaCatalog : révisions publiées ;
- MatchQueue : files d’attente par mode ;
- MatchAllocator : choix mode, arène et instance ;
- MatchDispatcher : sérialisation des commandes ;
- MatchScope : possession des ressources temporaires.

États d’un participant :

    CONNECTED
    DISCONNECTED_GRACE
    RESPAWNING
    ALIVE
    ELIMINATED
    SPECTATING
    LEFT

La politique de reconnexion est configurable. Un checkpoint contient uniquement des données sérialisables : partie, équipe, position, inventaire logique, armure, effets, vie, état du lit et expiration. Lors du retour, le joueur et la génération de la partie sont revalidés avant restauration.

## 9. Mondes

Une arène publiée est un template immuable. Chaque partie reçoit un clone isolé, permettant plusieurs parties simultanées sur la même arène.

Cycle d’un WorldLease :

1. réservation de l’arène et d’un nom de monde sûr ;
2. copie du template hors thread serveur ;
3. chargement Paper sur le thread serveur ;
4. configuration gamerules et attribution à la partie ;
5. évacuation des joueurs à la fin ;
6. suppression des viewers, entités temporaires et tickets de chunks ;
7. déchargement sur le thread serveur ;
8. suppression du clone hors thread serveur ;
9. fermeture idempotente du lease.

Un petit pool de clones préchauffés, borné par configuration, peut réduire le temps d’attente. Le setup utilise un monde d’édition verrouillé et jamais une instance active.

Les clones orphelins sont détectés au démarrage avec un manifeste signé par le plugin. Aucun dossier inconnu n’est supprimé.

## 10. Horloge, tâches et concurrence

GameLoopCoordinator est l’unique boucle de jeu répétitive globale. Il traite uniquement les échéances arrivées à terme :

- compte à rebours ;
- respawns ;
- générateurs ;
- timeline ;
- expirations d’objets ;
- HUD cadencé ;
- fin automatique.

Les listeners sont enregistrés une fois par type d’événement puis routent via les index. Les événements tels que dégâts, blocs, vide et déconnexion restent réactifs et ne nécessitent aucun scan.

Les échéances utilisent une file de priorité ou une roue temporelle bornée. Il n’existe pas de tâche répétitive par joueur, générateur, hologramme, PNJ ou cosmétique.

Exécuteurs séparés et bornés :

- base de données ;
- fichiers et mondes ;
- résolution de profils/skins ;
- calculs purs coûteux.

CompletableFuture.commonPool n’est jamais utilisé. Les files ont une limite et une politique de backpressure. Chaque callback asynchrone transporte MatchId et une génération afin qu’un résultat tardif soit ignoré après fermeture.

Le temps monotone réel sert aux délais visibles ; les ticks servent aux mécaniques liées à la simulation.

## 11. MatchScope et absence de fuite

MatchScope possède :

- abonnements métier ;
- handles de tâches ponctuelles ;
- BossBars ;
- scoreboards et viewers ;
- PNJ, hologrammes et entités d’effet ;
- tickets de chunks ;
- menus ouverts ;
- caches temporaires ;
- WorldLease.

Sa fermeture retire les ressources dans l’ordre inverse de création. Elle peut être appelée plusieurs fois sans effet secondaire.

Ordre d’arrêt du plugin :

1. état QUIESCING et refus de nouveaux joins ;
2. arrêt contrôlé des parties ;
3. retour des joueurs au lobby ;
4. fermeture de tous les MatchScope ;
5. flush SQL borné ;
6. déchargement des mondes runtime ;
7. arrêt des exécuteurs ;
8. fermeture du datasource ;
9. vidage des index et unregister de l’API.

Les maps longues durées contiennent uniquement des UUID, identifiants et snapshots. Les caches sont bornés et observables.

## 12. Générateurs et timeline

GeneratorNode contient :

- type de ressource ;
- position ;
- niveau ;
- intervalle ;
- capacité locale ;
- stratégie d’empilement ;
- hologramme, particules et sons ;
- règles d’amélioration.

Les générateurs sont pilotés par les échéances, pas par des tâches individuelles. Les drops proches sont fusionnés, plafonnés et produits uniquement si le monde est actif.

TimelineEngine utilise des définitions versionnées et configurables :

- Diamond II ;
- Diamond III ;
- Emerald II ;
- Emerald III ;
- Beds Destroyed ;
- Sudden Death ;
- Dragons.

Les événements publient des événements métier typés. Le scoreboard lit la prochaine échéance depuis une projection, sans recalculer toute la timeline.

## 13. Shops, upgrades et objets

Les catalogues sont immuables et versionnés. Une partie capture :

- ShopCatalogRevision ;
- UpgradeCatalogRevision ;
- ItemCatalogRevision ;
- TimelineRevision.

Le shop gère catégories, favoris, recherche, achat rapide, prix multiples et prévisualisation. Un achat est une transaction atomique sur le thread serveur :

1. validation du joueur et de la partie ;
2. validation de la disponibilité et du prix ;
3. retrait exact des ressources ;
4. application d’un GrantPlan ;
5. événement métier ;
6. rafraîchissement différentiel du GUI.

Les catégories standard couvrent blocs, armes, armures, outils, arcs, potions, utilitaires, spéciaux, favoris et recherche. Tous les objets classiques demandés sont des définitions du module standard.

Les upgrades d’équipe utilisent UpgradeDefinition, prérequis, niveaux, prix et TeamUpgradeState. Forge, Sharpness, Protection, Heal Pool, traps, Haste, Dragon Buff et Maniac Miner sont fournis par le module standard.

Chaque objet spécial fournit une définition immuable et une factory d’instance par partie. AbilityContext expose une vue immuable et une façade d’actions validées, jamais l’agrégat mutable ou le serveur global. Cooldowns, charges, ciblage, dégâts, rollback et ressources temporaires sont gérés par le MatchScope.

## 14. Lobby, PNJ et interface joueur

LobbyService couvre :

- spawn principal ;
- PNJ Join ;
- sélecteur d’arène ;
- sélecteur de mode ;
- gadgets et cosmétiques ;
- statistiques ;
- leaderboard ;
- retour depuis une partie.

Paper 1.21.11 permet d’utiliser Mannequin avec un profil pour la skin, Interaction pour la zone cliquable et TextDisplay pour le nom. Aucun paquet NMS ou plugin NPC externe n’est nécessaire.

WaitingLobbyPresenter gère scoreboard, BossBar, sons, compte à rebours et téléportation.

HudProjection produit un modèle immuable contenant :

- temps ;
- équipes restantes ;
- lits ;
- prochaines améliorations diamant/émeraude ;
- kills et final kills ;
- coins et XP.

ScoreboardRenderer et PlayerListRenderer conservent le dernier état envoyé à chaque viewer et n’appliquent que les différences. Les mises à jour sont cadencées, sans reconstruction complète ni clignotement.

SpectatorService fournit :

- téléporteur vers les joueurs ;
- masquage et absence de collision ;
- vision nocturne ;
- inventaire spectateur ;
- retour lobby ;
- restauration garantie de la visibilité et des attributs à la sortie.

## 15. Profils, statistiques et progression

Le profil n’est pas un agrégat géant :

- PlayerProfile : XP, niveau, prestige, coins et identité ;
- PlayerModeStatistics : kills, deaths, wins, losses, beds, final kills et temps joué ;
- ChallengeProgress ;
- CosmeticCollection ;
- FavoriteShopLayout.

KDR et winrate sont dérivés afin d’éviter les valeurs incohérentes. MatchStatAccumulator reste en mémoire pendant la partie. À la fin, MatchResult et ParticipantResult sont écrits dans une transaction identifiée par MatchId. Rejouer cette transaction ne double jamais les statistiques ou récompenses.

RewardLedger journalise les gains de coins, XP, niveaux, prestiges, défis, succès et récompenses avec une clé d’idempotence.

Les défis quotidiens et hebdomadaires sont déterministes pour une période et un fuseau configurés. Les missions et succès consomment les mêmes événements métier que les statistiques.

Les cosmétiques sont des features événementielles avec budget d’effets :

- Victory Dance ;
- Kill Effects ;
- Projectile Trails ;
- Death Cries ;
- Lobby Gadgets ;
- Sprays ;
- Pets ;
- Kill Messages ;
- Bed Break Effects ;
- cages ;
- titres ;
- emotes.

Les pets restent limités au lobby. Sons et particules sont filtrés par monde, distance et visibilité.

## 16. Persistance

Le YAML est limité aux paramètres d’amorçage qui ne peuvent pas être stockés dans la base qu’ils configurent :

- backend SQLite ou MySQL ;
- URL, hôte et base ;
- credentials ou variables d’environnement ;
- identifiant de serveur ;
- tailles et timeouts des exécuteurs ;
- locale par défaut.

Les arènes, modes actifs, catalogues, timings et paramètres de gameplay sont en base et modifiables dans les GUI. Les fichiers de monde restent sur disque mais sont capturés et gérés en jeu.

Tables principales :

    arena
    arena_revision
    catalog_revision
    player_profile
    player_mode_stats
    challenge_progress
    cosmetic_unlock
    favorite_shop_layout
    match_result
    match_participant_result
    reward_ledger
    leaderboard_projection
    reconnect_checkpoint
    admin_audit
    flyway_schema_history

Les révisions d’arène et de catalogue utilisent un document JSON versionné dans une colonne texte portable, entouré de métadonnées relationnelles. Les profils, résultats, statistiques et projections sont normalisés.

Garanties communes :

- HikariCP borné ;
- Flyway avec migrations spécifiques au dialecte lorsque nécessaire ;
- transactions et requêtes préparées ;
- batchs ;
- timeouts explicites ;
- version optimiste ;
- aucune bascule silencieuse de MySQL vers SQLite.

SQLite :

- WAL ;
- foreign_keys activé ;
- busy_timeout ;
- un seul writer et pool minimal.

MySQL :

- Connector/J ;
- utf8mb4 ;
- isolation et timeouts explicites ;
- pool aligné sur la capacité réelle de la base.

L’autosave repose sur le dirty tracking :

- modifications d’arène sauvegardées avec debounce et aux actions critiques ;
- profils regroupés en batch ;
- sauvegarde aux transitions de match ;
- résultat sauvegardé à la fin ;
- flush au quit et au shutdown avec délai borné.

Aucune requête SQL n’est effectuée par kill, tick de scoreboard ou particule.

## 17. Commandes et permissions

Arbre Brigadier :

    /bw create <nom>
    /bw delete <arène>
    /bw setup <arène>
    /bw join <arène|mode>
    /bw leave
    /bw start <partie>
    /bw stop <partie>
    /bw reload
    /bw list
    /bw editor
    /bw test <arène>

Les suggestions sont contextuelles. Les erreurs sont typées et localisées. La console peut exécuter les commandes qui n’exigent pas une position de joueur.

/bw reload recharge transactionnellement des snapshots immuables. Il n’appelle jamais le reload Bukkit/Paper. En cas d’erreur, l’ancienne configuration reste active.

/bw test crée une instance isolée sans statistiques ni récompenses. /bw delete refuse une arène utilisée, demande confirmation et produit une trace d’audit.

Permissions explicites :

    heneriabedwars.play
    heneriabedwars.command.join
    heneriabedwars.command.leave
    heneriabedwars.admin.create
    heneriabedwars.admin.delete
    heneriabedwars.admin.setup
    heneriabedwars.admin.start
    heneriabedwars.admin.stop
    heneriabedwars.admin.reload
    heneriabedwars.admin.test
    heneriabedwars.admin.stats
    heneriabedwars.admin.debug

Aucun wildcard global n’est accordé par défaut.

## 18. Administration et observabilité

Le GUI d’administration couvre :

- création, modification et suppression ;
- validation et publication ;
- lancement, arrêt et force start ;
- liste des instances et joueurs ;
- statistiques et leaderboards ;
- reload transactionnel ;
- mode debug ;
- santé de la base et des files ;
- rapport de ressources.

AdminAudit enregistre auteur, action, cible, résultat et horodatage.

Diagnostics internes :

- parties et mondes actifs ;
- durée du GameLoop ;
- échéances en retard ;
- profondeur des queues ;
- latence SQL ;
- profils dirty ;
- scopes et entités temporaires ;
- erreurs par type.

La télémétrie externe est désactivée par défaut. Les logs contiennent ArenaId et MatchId pour corrélation, sans données sensibles.

## 19. Qualité et documentation

Le build imposera :

- compilation Java 21 avec avertissements stricts ;
- formatage automatique ;
- Checkstyle ;
- Javadoc des API publiques et invariants importants ;
- tests ArchUnit ;
- tests JUnit ;
- couverture JaCoCo par module ;
- analyse des dépendances ;
- archives reproductibles ;
- dependency locking et vérification des checksums ;
- aucun TODO ou code désactivé dans une étape déclarée terminée.

Un JAR ombré est produit par bedwars-bootstrap. Shadow minimize n’est pas utilisé aveuglément, car il peut supprimer drivers JDBC, migrations ou classes chargées par mécanisme de service.

Documents prévus :

- architecture et ADR ;
- guide administrateur ;
- guide setup en jeu ;
- référence des commandes et permissions ;
- guide de création de mode ;
- guide de configuration des catalogues ;
- procédure SQLite/MySQL ;
- matrice de tests et capacité.

## 20. Stratégie de tests

### Tests purs

- invariants et transitions ;
- validation d’arènes ;
- victoire, élimination et respawn ;
- achats et upgrades ;
- timeline et générateurs ;
- KDR, winrate, XP et prestiges ;
- défis et récompenses ;
- idempotence ;
- horloge déterministe.

### Tests de contrats

Chaque repository exécute la même suite contre :

- SQLite temporaire ;
- MySQL 8.4 via Testcontainers.

Les migrations sont testées depuis une base vide et depuis chaque version publiée.

### Tests Paper

MockBukkit sert uniquement aux portions qu’il implémente correctement. Les GUI complexes, PDC, mondes, téléportations, mannequins, hologrammes et concurrence sont validés sur un vrai serveur Paper 1.21.11 lancé par une tâche Gradle.

### Tests de charge et mémoire

- dizaines de parties métier simulées en parallèle ;
- 300 participants simulés pour le dispatcher et les projections ;
- au moins 1 000 cycles création/fin de partie ;
- création et destruction répétée de mondes runtime ;
- JFR et heap dump comparés après GC stabilisé ;
- vérification que les scopes, index, viewers, tâches, mondes et connexions reviennent à zéro ;
- budget mesuré du GameLoop et des mises à jour UI.

La capacité de plusieurs centaines de joueurs sera annoncée seulement après mesure sur un profil matériel et un scénario documentés.

## 21. Barrière obligatoire avant chaque phase

Au début de chaque phase, la baseline de la phase précédente est recompilée et sa suite complète est rejouée. Le développement ne commence que sur une baseline verte.

Une phase ne peut être déclarée terminée que si :

1. le périmètre ne contient ni stub, ni TODO, ni branche temporaire ;
2. clean check réussit ;
3. les tests unitaires et ArchUnit réussissent ;
4. les contrats SQLite/MySQL réussissent si le stockage est touché ;
5. le serveur Paper démarre sans erreur si la plateforme est touchée ;
6. le scénario fonctionnel de la phase est reproduit ;
7. l’arrêt ne laisse ni tâche, monde, scope, viewer, thread ou connexion ;
8. la documentation et le changelog de phase sont à jour.

Si une barrière échoue, la phase suivante ne commence pas.

## 22. Plan de développement

### Phase 0 — Architecture

Livrable : ce document. Aucun code, build ou ressource exécutable n’est créé.

### Phase 1 — Fondation reproductible

- dépôt Git local ;
- Gradle Wrapper et Kotlin DSL ;
- modules et conventions ;
- plugin.yml minimal ;
- quality gates et CI locale ;
- démarrage/arrêt propre d’un plugin vide ;
- test réel Paper 1.21.11.

Critère fonctionnel : le JAR unique se charge puis s’arrête sans erreur ni thread survivant.

### Phase 2 — Model, SPI, domaine et API

- types immuables ;
- registres et gel ;
- arènes révisionnées ;
- machine à états ;
- équipes, participants et lits ;
- validation pure ;
- API en snapshots ;
- testkit déterministe.

Critère fonctionnel : scénarios métier complets sans Paper, y compris erreurs et idempotence.

### Phase 3 — Persistance complète

- migrations SQLite/MySQL ;
- repositories ;
- transactions ;
- autosave et dirty tracking ;
- résultats et ledger idempotents ;
- projections de leaderboard ;
- tests de contrat.

Critère fonctionnel : mêmes scénarios et résultats sur les deux backends.

### Phase 4 — Plateforme Paper et cycle de vie

- composition root ;
- commandes Brigadier ;
- listeners globaux ;
- dispatcher ;
- GameLoop ;
- MatchScope ;
- diagnostics de base.

Critère fonctionnel : démarrage, API, commandes, reload applicatif et arrêt propre sur vrai Paper.

### Phase 5 — Setup, GUI et publication d’arène

- commandes create/delete/setup/editor/test ;
- Nether Star protégée ;
- tous les menus d’édition ;
- équipes automatiques ;
- validation complète ;
- capture et révision de monde ;
- publication atomique.

Critère fonctionnel : une arène complète est créée et publiée sans éditer de YAML.

### Phase 6 — Provisioning, lobby d’attente et multiarène

- clones et WorldLease ;
- queue et allocateur ;
- plusieurs instances ;
- join/leave ;
- compte à rebours ;
- stop/fin/retour lobby ;
- reconnexion ;
- spectateur.

Critère fonctionnel : plusieurs parties isolées démarrent, se terminent et restaurent toutes leurs ressources.

### Phase 7 — Gameplay classique

- équipes et lits ;
- respawn et final kills ;
- élimination et victoire ;
- protection de map ;
- bordures et vide ;
- générateurs ;
- timeline complète ;
- sudden death et dragons.

Critère fonctionnel : une partie classique est jouable de l’attente jusqu’au podium final.

### Phase 8 — Shop, upgrades et objets classiques

- catégories, recherche et favoris ;
- tous les objets classiques listés ;
- outils évolutifs ;
- potions et utilitaires ;
- toutes les améliorations d’équipe ;
- pièges.

Critère fonctionnel : catalogues complets, achats atomiques et effets vérifiés.

### Phase 9 — Contenu signature

- dix objets exclusifs ;
- ciblage, cooldowns et rollback ;
- limites anti-abus ;
- interactions croisées et tests.

Critère fonctionnel : chaque objet possède un scénario nominal, limites, annulation et nettoyage.

### Phase 10 — Lobby et présentation

- lobby principal ;
- PNJ Join, Shop et Upgrade ;
- sélecteurs ;
- scoreboard, BossBar et TAB ;
- hologrammes, sons et particules ;
- statistiques et leaderboard visibles.

Critère fonctionnel : parcours joueur complet du lobby à la partie et retour.

### Phase 11 — Progression, défis et récompenses

- XP, niveaux et prestiges ;
- coins, badges et couleurs ;
- quotidiens, hebdomadaires, succès et missions ;
- récompenses idempotentes.

Critère fonctionnel : progression persistante et resets temporels testés.

### Phase 12 — Cosmétiques

- toutes les catégories demandées ;
- déblocage et sélection ;
- effets événementiels ;
- budgets et nettoyage.

Critère fonctionnel : chaque cosmétique est visible au bon moment et ne fuit aucune ressource.

### Phase 13 — Administration complète

- GUI opérationnel ;
- statistiques globales ;
- audit ;
- debug ;
- santé et diagnostics ;
- contrôles de sécurité.

Critère fonctionnel : exploitation courante possible sans console ni édition de gameplay en YAML.

### Phase 14 — Durcissement et capacité

- soak tests ;
- profilage JFR ;
- heap analysis ;
- charge multiarène ;
- optimisation mesurée ;
- documentation finale ;
- packaging de release.

Critère fonctionnel : budgets publiés, aucune régression, installation reproductible et matrice de compatibilité finale.

## 23. Décisions explicitement exclues

- aucun reload complet Bukkit ;
- aucun accès NMS ou réflexion sur les internals ;
- aucun singleton global mutable ;
- aucun stockage principal des arènes en YAML ;
- aucune I/O sur le thread serveur ;
- aucune tâche répétitive par entité fonctionnelle ;
- aucun fallback silencieux de base ;
- aucune modification en direct d’une révision utilisée ;
- aucune promesse de capacité sans mesure.

Cette architecture constitue le contrat de développement. Toute modification future devra être expliquée par un ADR avant d’affecter le code.
