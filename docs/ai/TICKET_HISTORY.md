# Historique des tickets

## Ticket 017 — Profils joueurs et statistiques SQLite — 2026-07-18

Le cœur ajoute un modèle de résultat terminé et un profil agrégé sans dépendance Bukkit. Une victoire capture les participants avant le recyclage du clone, puis `SqliteStatisticsRepository` enregistre la partie et tous les profils dans une transaction exécutée hors du thread serveur. `processed_matches` rend l'opération idempotente même après répétition d'un événement ou redémarrage.

Le profil contient parties, victoires, défaites, taux de victoire, kills, morts, final kills, ratio K/D, lits détruits, temps de jeu et séries actuelle/record. `/bw stats` et `/bedwars stats` donnent une vue personnelle localisée avec la permission publique `heneriabedwars.statistics.view`. MySQL reste préparatoire et utilise un repli mémoire explicite.

Validation complète : 210 tests réussis sans échec, erreur ni test ignoré; build propre, Spotless et Shadow JAR validés. La validation Paper doit confirmer la création du fichier, une victoire réelle, le redémarrage et l'absence de double comptage. Commit prévu : `feat(statistics): add persistent player profiles`.

## Ticket 016 — Combat 1.8 et dégâts BedWars — 2026-07-18

Le profil `legacy_1_8` agit maintenant réellement dans les clones runtime : vitesse d'attaque immédiate, balayage et boucliers neutralisés, point de dégâts historique pour les épées et knockback configurable avec variations sprint/projectile. `CombatPolicy` centralise les refus hors `PLAYING`, spectateur, respawn, protection, autre instance et friendly-fire.

Le dernier attaquant conserve le crédit d'une chute pendant une fenêtre configurable et les messages de mort affichent les couleurs d'équipe ainsi que `FINAL KILL`. `respawn.enabled` est désormais respecté. Le snapshot pré-partie capture/restaure vitesse d'attaque et fenêtre d'invulnérabilité. Validation : 204 tests automatisés réussis; build, Spotless et Shadow JAR validés. Le ressenti multijoueur reste à confirmer sur Paper.

## Ticket 015 — Équipement, outils et améliorations — 2026-07-18

La boutique possède un cinquième rayon Outils. Les armures mailles/fer/diamant et les cisailles sont conservées jusqu'à la fin de la partie; pioches et haches suivent quatre niveaux et régressent d'un palier à la mort. Le respawn reconstruit une armure aux couleurs de l'équipe, une épée en bois et les achats permanents.

La fiche d'équipe configure maintenant un PNJ d'améliorations distinct. Son menu compact propose Tranchant I, Protection I–IV et Hâte I–II avec niveau, prochain prix et solde. Les achats atomiques sont partagés avec l'équipe et appliqués immédiatement. `upgrades.yml`, `shops.yml`, les items et les langues évoluent sans écraser les personnalisations existantes.

Validation : 200 tests automatisés réussis, aucun échec ni erreur, build complet, Spotless et Shadow JAR validés. La matrice Paper multijoueur reste obligatoire.

## Correctif gameplay Tickets 012–014 — 2026-07-18

Le monde runtime force maintenant le PVP et n'autorise la destruction que des blocs posés pendant cette instance; la carte modèle reste intacte. La mort est immédiatement basculée en spectateur temporaire et le titre/actionbar suit chaque seconde de l'échéance de respawn. Les messages runtime adoptent une présentation Heneria cohérente.

La boutique v2 affiche rayons, prix et soldes plus lisiblement. La laine blanche logique devient la couleur de l'équipe lors du rendu et de l'achat; les minerais générés portent un nom Heneria localisé. Après victoire, le manager attend le retour au lobby, supprime entièrement le clone, libère l'arène et `/bw join` en crée un nouveau à la demande.

Validation : 196 tests automatisés réussis, aucun échec ni erreur; compilation complète, Spotless et Shadow JAR validés. La matrice Paper à deux joueurs reste obligatoire.

## Correctif Tickets 013–014 — menus, PNJ, rythme et hologrammes — 2026-07-18

Après validation en jeu, la fiche d'équipe a été reconstruite sur six lignes avec trois colonnes stables spawn/lit/boutique. Les boutons suivent état, définir, visiter, retirer; les actions impossibles ne remplissent plus l'écran de barrières. La cause des PNJ absents était l'ancien `shops.yml`, qui ne faisait pas partie des fichiers évolutifs : il est maintenant sauvegardé puis complété sans écrasement, et le PNJ est créé indépendamment du catalogue puis réparé au démarrage.

Les drops sont centrés sur le bloc, marqués par PDC, privés de gravité et stabilisés par la boucle globale. Les items joueurs restent intacts. Le rythme utilise un facteur borné équipes/joueurs au passage `PLAYING`; diamant et émeraude affichent un hologramme dont le compteur lit directement la prochaine échéance runtime.

Validation : 193 tests automatisés réussis, aucun échec ni erreur; build complet, Spotless et Shadow JAR validés. L'apparition réelle des PNJ, l'immobilité des drops et le rendu des hologrammes restent à confirmer sur Paper.

## Ticket 014 — PNJ de boutique et achats d'objets — 2026-07-18

La fiche d'équipe possède désormais une troisième zone guidée pour définir, visiter ou retirer la position de sa boutique. Cette donnée suit `ArenaService`, la révision optimiste et le YAML de l'arène. Lors de l'initialisation du clone, le plugin crée un villageois invulnérable et sans IA, lié à la partie et à l'équipe par PDC.

En `PLAYING`, un clic ouvre un menu à quatre catégories. Les quatorze offres par défaut sont définies dans `shops.yml`; prix, quantité, monnaie, matériau, traduction et ordre sont rechargeables. Le moteur pur refuse les joueurs absents, hors partie active ou spectateurs. L'adaptateur simule l'inventaire complet avant d'appliquer simultanément paiement et produit, puis publie `ShopPurchaseEvent` après réussite. Équipement avancé et améliorations restent au Ticket 015.

Validation : 190 tests automatisés réussis, sans échec, erreur ni test ignoré; build complet, tests, Spotless et Shadow JAR validés. Le parcours visuel et multijoueur reste à confirmer sur Paper.

## Correctif Ticket 013 — points combinés, menu v8 et alertes de lit — 2026-07-18

Le contrôle des positions distingue désormais la ressource du bloc : fer et or peuvent partager exactement les mêmes coordonnées, mais deux générateurs identiques au même bloc restent refusés. Le menu v8 organise le parcours en guide, quatre boutons de placement et liste de vérification; il affiche les comptes par ressource, le monde modèle et les regroupements sur un point.

Le scoreboard v7 possède une hiérarchie visuelle plus compacte, applique la vraie couleur Minecraft de l'équipe et indique clairement si son lit est vivant ou détruit. Une destruction de lit déclenche immédiatement le rafraîchissement du scoreboard et avertit les membres concernés par le broadcast général, un message personnel dans le chat, un titre, une actionbar et un son.

Validation : 187 tests automatisés réussis, sans échec, erreur ni test ignoré; build complet, Spotless et Shadow JAR validés. Les écrans et alertes restent à confirmer sur un serveur Paper réel.

## Ticket 013 — phase 2, générateurs configurables et drops — 2026-07-18

Les arènes persistent désormais leurs générateurs fer, or, diamant et émeraude. L'assistant principal ouvre une vue dédiée : un administrateur se place dans le monde modèle pour ajouter un point, peut le visiter, le déplacer avec Shift+gauche ou le supprimer après confirmation. Toutes les mutations utilisent `ArenaService`, la sauvegarde atomique et la révision optimiste existantes.

Lors de la création d'une instance, les coordonnées du modèle sont copiées dans le clone. Le ticker central appelle le moteur pur; l'adaptateur Bukkit limite la production aux parties `PLAYING`, compte les items compatibles proches, respecte la capacité, fusionne les piles et fractionne les nouveaux drops. Les anciennes arènes restent compatibles avec une liste vide et les valeurs des nouveaux points viennent de `generators.yml`.

Validation automatisée : 186 tests réussis, sans échec, erreur ni test ignoré. Les nouveaux scénarios couvrent le service, les doublons, le remappage runtime et le round-trip YAML. La validation Paper en jeu reste requise pour confirmer visuellement les drops dans un clone réel. Commit prévu : `feat(generators): add configurable resource generators`.

## Ticket 013 — phase 1, moteur pur des générateurs — 2026-07-18

Ajout de `GeneratorId`, des quatre ressources standard, des stratégies d'empilement et de `GeneratorDefinition`. Chaque `GameInstance` peut enregistrer avant le début ses `RuntimeGenerator`, dont les échéances et compteurs restent strictement dans le scope de la partie.

`GameGeneratorService` parcourt toutes les parties actives depuis un appel central, refuse les mondes absents, respecte la capacité locale fournie par un port, plafonne les émissions et fait tourner le premier générateur visité. Les longues pauses sautent les échéances anciennes après une seule émission. Cette phase ne contient volontairement ni YAML, ni GUI, ni drop Bukkit.

Validation : 183 tests automatisés réussis, 0 échec, 0 erreur et 0 test ignoré; build propre, Spotless, `git diff --check` et Shadow JAR contrôlés. Neuf scénarios sont propres au moteur de générateurs.

## Ticket 012 — première implémentation lits, respawns et éliminations — 2026-07-18

Le modèle administratif enregistre désormais les deux blocs et la direction d'un lit tout en détectant les anciennes sélections incomplètes. Le clone vérifie physiquement ces blocs et construit un index O(1). La destruction ennemie est atomique, le lit allié et les autres blocs de carte restent protégés, et les explosions ne peuvent supprimer silencieusement un lit.

`GameDeathService` décide respawn ou mort finale; `GameRespawnService` utilise le ticker central et applique une protection temporaire. Les final deaths alimentent les statistiques, éliminent une équipe sans membre capable de revenir et déclenchent `ENDING` pour la dernière équipe participante. Messages, scoreboard de jeu, spectateur et nettoyage différé sont branchés. Le ticket reste en validation jusqu'au parcours Paper multijoueur.

Validation : 174 tests automatisés réussis, 0 échec, 0 erreur et 0 test ignoré; build propre, Spotless, `git diff --check` et Shadow JAR contrôlés. Un refus Windows du renommage atomique de dossiers de carte dispose désormais d'un repli standard sûr. Aucun serveur Paper n'étant disponible, le parcours multijoueur réel reste requis.

## Correctif équipes, traductions et début de partie — 2026-07-17

Correction issue de trois captures en jeu : AQUA et les autres équipes héritaient toutes d'un nom violet, l'état prêt exposait une clé brute, la visite du lit annonçait seulement le nom technique du monde et la fiche compacte manquait de repères. Les entrées/titres v6 suivent maintenant `TeamColor`, la disponibilité est localisée, les messages nomment l'équipe et la fiche retrouve cinq lignes avec deux colonnes constantes et un guide central.

Le passage en `PLAYING` ne se limite plus à l'ancien message de développement. Le runtime résout le spawn de l'équipe attribuée et l'adaptateur Bukkit téléporte le joueur dans le clone chargé en mode survie. Les mécaniques BedWars postérieures au placement restent volontairement hors de ce correctif.

Validation : 166 tests automatisés réussis, aucun échec ni test ignoré, build propre, Spotless et Shadow JAR validés. Le parcours complet reste à confirmer sur un serveur Paper réel.

## Correctif UX et remplacement de cartes — 2026-07-17

L'assistant d'arène a été recentré sur les opérations réellement nécessaires : carte, spawn d'attente, spawn spectateur, format, équipes colorées, validation et activation. Les fiches d'équipe sont accessibles directement et leur retour ramène à l'assistant. Les réglages secondaires ne surchargent plus la vue principale.

Chaque carte dispose maintenant d'un dossier d'import documenté. Le menu vérifie `level.dat`, ferme le monde, sauvegarde la version actuelle, remplace le dossier de façon confinée et asynchrone, restaure l'ancienne cible si l'échange échoue puis recharge le monde comme carte `BEDWARS`. Les liens d'arène et métadonnées restent conservés. Validation : 165 tests réussis, aucun échec ni test ignoré, build propre, Spotless et Shadow JAR validés; essai Paper réel restant.

## Correctif de préparation Ticket 012 — configuration des équipes

Ajouté le 2026-07-17 après constat en jeu que les sous-commandes d'équipe étaient invisibles et que le clic du menu modifiait directement le spawn. Le menu général affiche maintenant un résumé spawn/lit et ouvre une fiche colorée par équipe. Cette fiche sépare définir, téléporter et retirer; chaque mutation sauvegarde puis reste sur la fiche avec la nouvelle révision.

`setbed` et l'action GUI utilisent le bloc réellement regardé, vérifient un lit complet dans le monde modèle et normalisent sa partie pied. Le service refuse les positions déjà associées à une autre équipe. Les commandes `list`, `setspawn`, `clearspawn`, `teleport`, `setbed`, `clearbed` et `teleportbed` disposent d'une aide et d'une complétion imbriquée. Ce correctif ne livre pas encore le gameplay lit → mort → réapparition → élimination du Ticket 012.

Validation : 162 tests automatisés réussis, aucun échec, Spotless et Shadow JAR validés. Les clics et la détection physique du lit restent à confirmer sur un serveur Paper réel.

## Correctif Ticket 010.1 - Accès, items du lobby et scoreboard

Ajustement d'interface : le menu ouvert par le livre ne duplique plus le départ. Il présente informations générales, état/countdown, joueurs et fermeture. Le navigateur public `/bw`, quick play, filtres et sélection par clic sont explicitement reportés au Ticket 011 et documentés dans `TICKET_011_SCOPE.md`.

Terminé le 2026-07-17 côté code et validation automatisée. Le dashboard `/bedwars` exige désormais `heneriabedwars.admin.dashboard`; les permissions publiques `heneriabedwars.game.join|leave` et la complétion ne révèlent aucune commande de configuration. Le message joueur explique le parcours public temporaire.

La cause des items inactifs était un listener `ignoreCancelled=true` sans filtrage off-hand : une interaction protégée pouvait être annulée avant l'action. Les nouveaux PDC `runtime_item`/`runtime_game_id`, le registre d'actions, le cooldown et la garde d'opération assurent un seul leave via `GameLobbyService`. Le livre ouvre un menu public séparé.

Le scoreboard effaçait tous ses scores à chaque rafraîchissement et rendait `GameState.name()`. Il utilise désormais des templates `game.yml`, des labels localisés et une session personnelle à entrées stables. Le masquage Paper possède un fallback Spigot. Validation : 161 tests automatisés; aucun test en jeu faute de serveur.

## Ticket 010 - Lobby de partie, file d'attente et compte a rebours

Termine le 2026-07-17 cote code et validation automatisee. `GameLobbyService` centralise l'entree, la sortie, la deconnexion, le lancement et l'arret pre-game; `GameCountdownService` porte le compte a rebours sans tache par partie. Le cycle `WAITING -> STARTING -> PLAYING` est automatique au minimum de joueurs, s'annule si ce minimum est perdu et peut etre force par un administrateur pour les tests.

Le plugin capture l'etat initial d'un joueur en memoire avant le lobby runtime et le restaure lors de sa sortie. Pendant l'attente, les protections sont limitees aux membres de l'instance, un rescue du vide est applique et les items configures permettent de quitter ou consulter la partie. Scoreboard, bossbar, titres, actionbar et sons sont relies aux events Java internes, sans gameplay BedWars cache apres `PLAYING`.

`game.yml` ajoute les reglages de protections, inventory, countdown, affichages et nettoyage. `/bedwars game start <id> [--force]` et `/bedwars game stop <id>` utilisent des identifiants courts non ambigus; le tableau de bord contient une liste runtime avec actions guidees et confirmation d'arret. Les permissions `game.start`, `game.force-start` et `game.stop` sont declarees.

Validation intermediaire : 154 tests automatises reussis, aucun echec ni ignore; Spotless et compilation passent. Les interactions Bukkit reelles (teleportation, snapshot, protections, bossbar, inventaires et nettoyage de mondes) restent a verifier manuellement sur Paper. Commit prevu : `feat(game): add waiting lobby and countdown lifecycle`.

## Ticket 009 — Runtime des parties (Game Instance Engine)

Terminé le 2026-07-16 côté code et validation automatisée. `bedwars-core/game` introduit `GameId`, `GameInstance`, `GameInstanceManager`, `RuntimeArena`, `RuntimePlayer`, `RuntimeTeam`, les ports monde/joueur et la machine `CREATING → WAITING → STARTING → PLAYING → ENDING → RESETTING → DESTROYED`. Les index empêchent une seconde instance sur une arène et l'appartenance d'un joueur à plusieurs parties. Une copie échouée détruit l'instance provisoire et libère la réservation.

`bedwars-plugin/game` clone le monde modèle hors thread, charge le clone `hbw_game_<UUID>` sur le thread serveur, applique les règles sûres, téléporte au point d'attente, évacue, décharge sans sauvegarde et supprime les dossiers contrôlés. Les restes d'un crash sont nettoyés avant la première création. `/bedwars game create|list|info|join|leave|destroy` fournit le contrôle administratif.

Correctif de parcours : une arène active affiche désormais « Tester l'arène » dans l'éditeur. Le clic gauche crée l'instance si nécessaire puis téléporte le joueur; le clic droit conserve l'accès à la désactivation. `/bedwars game join <arène>` accepte aussi directement l'identifiant administratif et crée automatiquement l'instance absente.

Les événements Java internes couvrent création, attente, démarrage, fin, destruction et mouvements de joueurs. `HeneriaBedWarsApi` expose désormais des façades et snapshots en lecture seule via le registre de services Bukkit. Aucun gameplay, lit actif, générateur, boutique, victoire, SQL, Redis ou proxy n'est livré. Validation : 149 tests automatisés réussis, 0 échec, 0 erreur et 0 ignoré; Spotless, build propre, `git diff --check` et Shadow JAR contrôlés. Les opérations Paper restent à tester manuellement faute de serveur Minecraft dans l'environnement. Commit prévu : `feat(runtime): add game instance engine`.

## Ticket 008 — Finalisation de l'éditeur graphique des cartes

Terminé le 2026-07-16 côté code et validation automatisée. `MapMenuFactory` fournit une interface v4 cohérente avec le tableau de bord : bibliothèque paginée, filtres, tris et direction persistants par joueur; états vides explicites; création BedWars directe ou choix avancé `LOBBY`/`BEDWARS`/`GENERIC`; confirmation, création du monde et téléportation automatique.

L'éditeur central affiche état, progression, sauvegarde, associations et prochaine action. Il gère nom visible, type sécurisé, point d'arrivée, heure, cycles, météo, difficulté, PVP, créatures, feu, dégâts environnementaux et autosauvegarde. Validation et solutions sont traduites. Les associations permettent de lier/délier une arène ou d'en créer une déjà liée. La suppression revient au tableau de bord et oublie les états obsolètes.

`MapOperationTracker` rend sauvegarde complète, duplication et suppression visibles; `MapOperationLock` empêche les chevauchements. Les archives et copies restent hors du thread serveur. `MapDirtyListener` marque les cartes modifiées. Les états sont nettoyés à la déconnexion et à l'arrêt. Validation : 144 tests automatisés réussis, Spotless, build propre et Shadow JAR contrôlés. Aucun serveur Minecraft n'étant disponible, les interactions Paper restent à tester manuellement. Commit prévu : `feat(world): complete guided map template editor`.

## Ticket 007 — Gestionnaire autonome de mondes et cartes modèles

Correctif d'expérience après test en jeu : le point d'entrée joueur est désormais le simple `/bedwars`, qui ouvre un tableau de bord compact. Les listes d'arènes/cartes et les diagnostics utilisent des apparences v2 fusionnées sans écraser les configurations existantes. La création d'arène enchaîne sur le choix de carte et peut créer/associer une carte `BEDWARS` depuis le même écran. Les codes techniques et textes anglais ne sont plus présentés dans la validation.

La validation ne produit plus simultanément `MAP_TEMPLATE_MISSING` et `missing-world`. Une carte modèle valide peut rester déchargée : sa métadonnée est l'autorité administrative, tandis que le monde Bukkit n'est chargé que pour la construction ou la téléportation. Les commandes détaillées restent compatibles pour la console et les usages avancés mais sont masquées de l'aide et de la complétion principale des joueurs.

Deuxième passe après capture de l'éditeur : tous les réglages d'arène restants ont été convertis en assistant continu. L'écran indique la progression et la prochaine action; carte, attente, équipes, joueurs et spectateur sont numérotés. Les sous-menus joueurs/équipes détaillent chaque clic, les limites sont annoncées comme optionnelles et les confirmations réutilisent les nouvelles apparences. De nouvelles clés `assistant-*` garantissent que cette refonte est fusionnée même sur une installation ayant déjà reçu les premières clés v2.

Troisième passe après test de suppression : une arène supprimée ne laisse plus son éditeur dans l'historique. La suppression réussie oublie sa révision observée et recrée une session GUI directement sur le tableau de bord de configuration. Tous les retours de l'assistant sont désormais déterministes, y compris les confirmations ouvertes directement. Les listes d'arènes/cartes, les états vides, le résumé de configuration, l'absence d'arène et l'éditeur de carte partagent des apparences v3 concises qui expliquent action, effet et blocages sans afficher les détails internes.

Terminé le 2026-07-16 côté code et validation automatisée. `bedwars-core/map` ajoute modèle immutable, identifiants sûrs, types/états, registre copy-on-write, verrous, ports et service transactionnel. `bedwars-plugin/map` ajoute métadonnées YAML UTF-8 atomiques, fichiers confinés, générateur vide, gestion Bukkit, cycle de vie, commandes et menus.

La création fournit un monde vide préfixé avec plateforme optionnelle et réglages configurables. Chargement, téléportation, spawn, sauvegarde et déchargement restent sur le thread serveur. Duplication et suppression de fichiers sont asynchrones; les copies excluent les données propres au monde/joueur. La suppression exige une sauvegarde complète, refuse les joueurs et vérifie en direct les arènes liées ainsi que le lobby protégé.

Les arènes peuvent référencer une carte `BEDWARS` par `map.template-id`. L'ancien champ `map.template` reste lisible. Les arènes sont la source de vérité; les liens inverses sont resynchronisés. Une carte manquante, du mauvais type ou en erreur empêche la validation administrative.

Configuration : ajout de `worlds.yml`, dixième YAML principal, dossiers `maps/templates`, `maps/metadata`, `instances` et `backups/maps`, items de menus et traductions FR/EN. Les mondes de chunks restent dans le conteneur Bukkit sous `hbw_template_<id>`; les templates du dossier plugin portent un marqueur de propriété. `instances/` reste réservé et aucun gameplay n'est livré.

Validation : 138 tests automatisés réussis, 0 échec et 0 ignoré; Spotless, build propre et Shadow JAR contrôlés après la troisième passe. Tests en jeu non réalisés faute de serveur Minecraft. Commit prévu pour la passe GUI : `fix(gui): reset navigation after arena deletion`.

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
