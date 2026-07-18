# Décisions d'architecture

## ADR-113 à ADR-118 — profils, classements et progression

- **ADR-113** — L'UUID reste l'identité durable; le pseudo courant est un index révisable enregistré uniquement après une vraie connexion Bukkit.
- **ADR-114** — La recherche normalise avec `Locale.ROOT`; un renommage retire l'ancien index et un pseudo réutilisé appartient au dernier UUID connecté.
- **ADR-115** — Les anciennes statistiques sans pseudo restent classables avec un UUID court, mais ne deviennent recherchables par nom qu'après reconnexion.
- **ADR-116** — Les catégories de classement sont une enum fermée traduite vers des colonnes SQL constantes; limite maximale 100, top joueur fixé à 10.
- **ADR-117** — L'expérience et le niveau sont dérivés des agrégats, donc reproductibles et sans migration de compteur. Le palier du niveau `n` commence à `100 × (n-1)²` XP.
- **ADR-118** — Le Ticket 018 n'accorde aucune récompense et ne modifie pas l'API Java publique; permissions de profils tiers et classements restent distinctes.

## ADR-107 à ADR-112 — statistiques persistantes

- **ADR-107** — Les agrégats, résultats de match et ports de persistance appartiennent au cœur pur; JDBC reste exclusivement dans le module plugin.
- **ADR-108** — SQLite via Xerial `sqlite-jdbc` est le premier backend actif et le seul ajout de dépendance du ticket; il fournit un stockage embarqué portable sans service externe.
- **ADR-109** — Toutes les opérations JDBC passent par un exécuteur mono-thread dédié; le thread Bukkit ne bloque jamais sur un accès disque ou SQL.
- **ADR-110** — L'UUID runtime de partie est une clé idempotente persistée dans `processed_matches`; insertion du match et agrégation des joueurs partagent une transaction.
- **ADR-111** — `GameVictoryEvent` est le point de capture final, avant destruction de l'instance. Les abandons administratifs et parties inachevées ne modifient pas les profils.
- **ADR-112** — Le Ticket 017 expose seulement le profil personnel par commande. Classements, recherche d'autres joueurs et mutation publique restent hors périmètre.

## ADR-101 à ADR-106 — Combat 1.8 et dégâts BedWars

- **ADR-101** — Toute autorisation de dégâts runtime passe par `CombatPolicy`, pur et indépendant de Bukkit.
- **ADR-102** — Le profil `legacy_1_8` est réalisé par vitesse d'attaque élevée, suppression du balayage/bouclier et correction de dégâts d'épée; il ne remplace jamais les items du Ticket 015.
- **ADR-103** — Le knockback utilise `EntityKnockbackByEntityEvent` et son vecteur final, sans annulation du dégât ni tâche différée par coup.
- **ADR-104** — La protection de respawn, le friendly-fire et l'appartenance à la même instance sont vérifiés avant attribution d'un coup.
- **ADR-105** — Le crédit d'une chute lit `CombatTracker` dans une fenêtre configurable; le runtime reste la source du kill avant agrégation finale par le Ticket 017.
- **ADR-106** — Vitesse d'attaque et fenêtre d'invulnérabilité d'origine font partie du snapshot pré-partie et sont restaurées à la sortie.

## ADR-096 à ADR-100 — Équipement et améliorations

- **ADR-096** — La progression d'équipement appartient au `RuntimePlayer`; armure et cisailles sont permanentes pendant la partie, pioches et haches perdent un niveau à chaque mort.
- **ADR-097** — Les outils s'achètent séquentiellement afin que le catalogue reste configurable sans permettre de sauter un palier.
- **ADR-098** — Les niveaux Tranchant, Protection et Hâte appartiennent au `RuntimeTeam` et ne sont jamais persistés dans l'arène.
- **ADR-099** — Le paiement d'une amélioration et l'incrément de niveau sont coordonnés par `TeamUpgradePurchaseService`; le menu et le listener ne portent aucune règle métier.
- **ADR-100** — Le loadout Bukkit est reconstruit depuis le snapshot runtime après téléportation de début/respawn et après achat, garantissant la même source de vérité pour tous les joueurs.

## ADR-090 à ADR-095 — Boucle jouable et recyclage

- **ADR-090** — Le PVP d'un clone BedWars est toujours actif; le réglage PVP du modèle reste réservé à son monde administratif.
- **ADR-091** — La carte runtime est immuable par défaut. Seuls les blocs enregistrés dans `GameInstance` après un placement réussi peuvent être cassés ou détruits par une explosion.
- **ADR-092** — Le respawn immédiat repose sur la gamerule du clone avec un repli Spigot au tick suivant; son affichage lit l'unique échéance `RuntimePlayer.respawnAt` depuis le ticker central.
- **ADR-093** — `WHITE_WOOL` est l'offre logique de laine d'équipe et devient dynamiquement la laine du `RuntimeTeam` au rendu comme à l'échange atomique.
- **ADR-094** — Les noms Heneria sont des métadonnées visuelles; la monnaie continue d'être reconnue par son matériau vanilla pour conserver compatibilité et empilement.
- **ADR-095** — La fin de match attend le retour lobby avant la destruction du monde. Une arène active sans instance reste publiquement sélectionnable et est clonée à la demande.

## ADR-085 à ADR-089 — Stabilisation générateurs et réparation des PNJ

- **ADR-085** — `shops.yml` et `generators.yml` rejoignent l'évolution non destructive des ressources version 1; les personnalisations existantes restent prioritaires.
- **ADR-086** — Un PNJ configuré existe même avec un catalogue vide; l'indisponibilité des offres appartient au menu, pas au cycle de vie de l'entité.
- **ADR-087** — Seuls les items portant l'identité PDC de leur partie et générateur peuvent être fusionnés, ancrés ou comptés dans la capacité.
- **ADR-088** — Le facteur de rythme est capturé à `PLAYING`, borné entre `minimum-factor` et `maximum-factor`, puis la première échéance repart de cet instant.
- **ADR-089** — Les hologrammes sont des `TextDisplay` runtime actualisés par le ticker central depuis l'échéance métier; aucune tâche ou horloge parallèle n'est autorisée.

## ADR-080 à ADR-084 — Boutiques runtime

Accepté pour le Ticket 014.

- **ADR-080** — La position d'une boutique appartient à l'équipe administrative; l'entité PNJ reste exclusivement runtime dans le clone.
- **ADR-081** — Les PNJ sont identifiés par PDC `shop_game_id` et `shop_team_id`, jamais par leur nom, leur profession ou leur apparence.
- **ADR-082** — Le catalogue actif est un snapshot immuable construit depuis `shops.yml`; une offre invalide est isolée et journalisée sans casser les autres.
- **ADR-083** — `ShopPurchaseService` porte toutes les règles d'autorisation; le listener et le menu ne mutent pas directement le runtime métier.
- **ADR-084** — Le retrait de monnaie et l'ajout du produit forment un unique échange d'inventaire simulé puis appliqué; un refus ne consomme rien.

## ADR-071 à ADR-074 — Fondation des générateurs

Accepté pour la phase 1 du Ticket 013.

- **ADR-071** — Une partie capture des `GeneratorDefinition` immuables; seul leur calendrier et leurs compteurs sont runtime.
- **ADR-072** — Aucun générateur ne possède de tâche : un coordinateur global traite les échéances des instances actives.
- **ADR-073** — Une échéance en retard émet au maximum une fois et saute les intervalles passés afin d'interdire les rafales de rattrapage.
- **ADR-074** — La capacité locale vient d'un port de lecture plateforme; un budget global avec rotation garantit une charge bornée et équitable.

## ADR-075 à ADR-078 — Générateurs administratifs et Bukkit

Accepté pour la phase 2 du Ticket 013.

- **ADR-075** — Les positions de générateurs appartiennent à `ArenaDefinition` et suivent la même persistance atomique et la même révision optimiste que les autres réglages d'arène.
- **ADR-076** — Le remappage modèle → clone conserve les coordonnées et remplace implicitement le monde; aucun nom de monde runtime n'est persisté.
- **ADR-077** — La création, le déplacement et la suppression passent par l'assistant et `ArenaService`; le menu ne lit et n'écrit jamais directement le YAML.
- **ADR-078** — L'adaptateur Bukkit émet uniquement en `PLAYING`, compte les items compatibles dans un rayon borné, fusionne les piles si demandé et fractionne les drops trop grands.
- **ADR-079** — L'unicité d'un point porte sur la ressource et le bloc; fer et or peuvent partager une position, tandis qu'un doublon fer + fer au même bloc reste refusé.

## ADR-062 à ADR-070 — Gameplay des lits

Accepté pour la phase de validation Ticket 012.

- **ADR-062** — Un lit administratif complet possède un pied, une tête et une direction.
- **ADR-063** — Le runtime, et non la présence du bloc seule, porte la transition irréversible vivant → détruit.
- **ADR-064** — Les coordonnées du modèle sont remappées sans nom de monde vers le clone.
- **ADR-065** — Seul un joueur ennemi en `PLAYING` obtient la destruction atomique.
- **ADR-066** — La décision de mort appartient à `GameDeathService`, jamais au listener Bukkit.
- **ADR-067** — La décision de respawn est capturée au moment de la mort et avancée par le ticker central.
- **ADR-068** — Une équipe est éliminée quand aucun membre ne peut encore revenir.
- **ADR-069** — Une seule équipe participante restante déclenche la victoire provisoire et `ENDING`.
- **ADR-070** — Les blocs du modèle restent protégés; seuls les lits ennemis indexés sont destructibles.

## ADR — Le début de partie positionne les équipes sans démarrer le gameplay BedWars

Décision : `GameStartEvent` déclenche la préparation Bukkit et la téléportation au spawn de l'équipe sélectionnée dans le clone runtime. La résolution de destination reste dans `GameInstance`; l'accès au monde et au joueur reste dans l'adaptateur plugin. Cette étape appartient au cycle de vie de la partie et ne change pas l'état des lits, n'écoute aucune mort et ne décide aucun vainqueur.

## ADR — Import de carte par dépôt géré, sans sélection de chemin

Décision : chaque modèle possède `maps/templates/<id>/import/`; l'administrateur y dépose le contenu du monde puis déclenche le remplacement dans le GUI. Le plugin n'accepte jamais un chemin saisi, refuse les liens symboliques, sauvegarde la cible et effectue l'échange sous verrou. Cette approche rend le remplacement compréhensible dans les fichiers tout en préservant le confinement du Ticket 007 et la séparation thread Bukkit / accès disque.

## ADR-059 à ADR-061 — Configuration guidée des équipes

Accepté.

- **ADR-059** — Une équipe s'édite dans une fiche dédiée; un clic sur son entrée ne modifie jamais silencieusement une position.
- **ADR-060** — La frontière Bukkit valide le lit physique et normalise sa partie pied; `ArenaService` conserve la persistance, la révision optimiste et l'unicité entre équipes.
- **ADR-061** — Les commandes d'équipe restent des outils de secours complets et autocomplétés, tandis que le parcours recommandé reste le menu `/bedwars`.

## ADR-051 à ADR-058 - Correctif lobby Ticket 010.1

Accepté.

- **ADR-051** — `/bedwars` sans argument et son dashboard sont strictement administratifs avec une permission dédiée.
- **ADR-052** — Les commandes publiques `game join|leave` utilisent `heneriabedwars.game.*`, séparé de `heneriabedwars.admin.*`.
- **ADR-053** — Un item runtime est identifié par les PDC `runtime_item` et `runtime_game_id`, jamais par son apparence.
- **ADR-054** — Les actions d'items passent par `GameLobbyService`; le listener ne téléporte ni ne restaure directement.
- **ADR-055** — Le scoreboard runtime est piloté par les templates validés de `game.yml`.
- **ADR-056** — Les états et messages runtime affichés sont localisés avant rendu.
- **ADR-057** — Une session personnelle conserve objectif, équipes et entrées stables et ne met à jour que les lignes modifiées.
- **ADR-058** — Le masquage des scores est une capacité Paper optionnelle avec fallback Spigot sans erreur.

## ADR-040 — Instance runtime distincte des définitions persistantes

Accepté. `ArenaDefinition` et `MapTemplate` restent administratifs; `GameInstance` ne les modifie jamais et possède un UUID ainsi qu'un clone jetable.

## ADR-041 — Machine d'état fermée et index atomiques

Accepté. Les transitions autorisées sont déclarées dans `GameInstance`. `GameInstanceManager` réserve l'arène avant le clonage, indexe un joueur une seule fois et libère tous les index après destruction ou rollback.

## ADR-042 — Ports asynchrones pour monde et joueur

Accepté. `RuntimeWorldService` et `RuntimePlayerGateway` utilisent `CompletionStage`. L'adaptateur Bukkit copie/supprime hors thread et replannifie chargement, téléportation et déchargement sur le thread serveur. Cette frontière permet de futurs adaptateurs proxy sans ajouter de dépendance maintenant.

## ADR-043 — API publique par snapshots

Accepté. `HeneriaBedWarsApi` expose trois façades en lecture seule et des records immuables. L'implémentation est publiée par le `ServicesManager` Bukkit; aucune classe de `bedwars-core`, collection mutable ou objet Bukkit n'est exposé.

## ADR-039 — Éditeur graphique des cartes

Accepté. Le menu v4 est une façade : toutes les mutations passent par `MapTemplateService` ou `ArenaService`. `MapEditorStateStore` conserve uniquement des données bornées par UUID et les nettoie à la déconnexion. Progression guidée et validation technique restent séparées. `MapOperationTracker` expose sauvegarde, duplication et suppression sans remplacer `MapOperationLock`. Les copies de fichiers sont asynchrones, les arènes restent la source de vérité des associations et l'application des réglages Bukkit est compensée en cas d'échec.

## ADR-029 — Définition d'arène distincte d'une partie

Accepté. `ArenaDefinition` décrit uniquement une configuration administrative persistante. `ArenaStatus` ne réutilise aucun futur état `WAITING/PLAYING/RESETTING` et aucune arène activée ne lance de gameplay.

## ADR-030 — Un YAML UTF-8 sûr par arène

Accepté. `ArenaId` limite les noms à `[a-z0-9_-]{2,32}` et le dépôt écrit `arenas/<id>.yml` par remplacement atomique. Le cœur dépend seulement d'`ArenaRepository`.

## ADR-031 — Publication mémoire après persistance

Accepté. Toute création ou modification est sauvegardée avant le remplacement copy-on-write d'`ArenaRegistry`. Une panne d'écriture laisse l'ancienne définition intacte.

## ADR-032 — Reload partiel et diagnostic visible

Accepté. Une définition structurée mais non activable reste visible comme `INVALID`. Un YAML illisible conserve l'ancienne définition du même id au reload ; au premier chargement il est journalisé et ignoré sans désactiver le plugin.

## ADR-033 — Sauvegarde obligatoire avant suppression

Accepté. Le seul contrat de suppression est `deleteWithBackup`. Le fichier est copié sous `backups/arenas/<date>/` avant sa suppression, avec suffixe anti-collision.

## ADR-022 — Clés logiques et définitions d'items immuables

Accepté. Les items sont adressés par des clés minuscules pointées, normalisées par `ItemKey`. `ItemDefinition` ne contient aucune section YAML ni donnée Bukkit mutable.

## ADR-023 — Reconstruction de chaque ItemStack

Accepté. Seules les définitions validées sont conservées; chaque appel à `ItemService.build` crée un stack et une meta indépendants. Aucun cache d'`ItemStack` traduit ou dynamique n'est partagé.

## ADR-024 — Héritage simple borné et fusion explicite

Accepté. Profondeur maximale 16, parent unique, parent inconnu/cycle refusé. Le lore enfant remplace le lore parent; flags et placeholders requis sont réunis sans doublon; enchantements et tags fusionnent avec priorité enfant.

## ADR-025 — Actions GUI séparées des items

Accepté. `items.yml` décrit uniquement l'apparence et des métadonnées non exécutables. Slots, permissions et actions restent dans les définitions GUI Java.

## ADR-026 — PDC contrôlé par liste blanche

Accepté. `heneriabedwars:item_key` est interne; seuls les tags configurables `category` et `action` peuvent être copiés dans des clés PDC dédiées. Le slot/session reste l'autorité pour les clics GUI.

## ADR-027 — Registre d'items dans le snapshot transactionnel

Accepté. Traductions, héritages et propriétés sont validés avant l'échange de `ConfigurationSnapshot`. Une erreur d'héritage ou critique conserve intégralement l'ancien registre et les menus ouverts ne sont pas fermés.

## ADR-028 — Têtes sans résolution réseau synchrone

Accepté. Le joueur du contexte est appliqué par UUID; un propriétaire statique n'est appliqué que s'il est déjà en ligne. Les textures Base64 et téléchargements de skin sont exclus de ce ticket.

## ADR-017 — Identification GUI par holder et identifiants de vue

Accepté. Chaque inventaire utilise `GuiInventoryHolder(sessionId, viewId, menuId)`, jamais le titre. `viewId` est renouvelé à chaque navigation, empêchant une fermeture retardée de supprimer la vue suivante.

## ADR-018 — Une session GUI active par joueur

Accepté. `GuiSession` est indexée par UUID joueur sans référence forte au joueur dans le cœur. Remplacement et nettoyage restent déterministes.

## ADR-019 — Framework GUI interne avant API publique

Accepté. Les modèles restent internes jusqu'à plusieurs usages métier réels; `bedwars-api` reste inchangé et aucune surface instable n'est figée.

## ADR-020 — Inventaires exclusivement sur le thread serveur

Accepté. Toute ouverture hors thread est replanifiée et les auto-refresh utilisent une tâche Bukkit centrale, jamais une tâche par bouton ou vue.

## ADR-021 — Navigation par historique limité

Accepté. Une deque bornée conserve des définitions `Gui` immuables; le remplacement sans historique est explicite. Retour et racine sont prédictibles, la mémoire bornée par `menus.yml`.

## ADR-001 — Utilisation de Java 21

### Statut
Accepté.

### Contexte
Paper 1.21.x requiert un runtime Java moderne.

### Décision
Compiler et exécuter avec Java 21.

### Raisons
Version LTS, records et langage moderne, compatibilité Paper.

### Consequences
Les serveurs et outils de build doivent fournir un JDK 21.

## ADR-002 — Utilisation de Gradle Kotlin DSL

### Statut
Accepté.

### Contexte
Le build doit être reproductible et multi-modules.

### Décision
Utiliser Gradle 8.14.5 via Wrapper et des scripts `.gradle.kts`.

### Raisons
Configuration typée, Wrapper portable et bonne gestion des modules.

### Conséquences
Le Wrapper est la seule commande de build prise en charge.

## ADR-003 — Architecture multi-modules

### Statut
Accepté.

### Contexte
L'API, le métier et Paper évoluent à des rythmes différents.

### Décision
Créer `bedwars-api`, `bedwars-core` et `bedwars-plugin`.

### Raisons
Frontières lisibles et tests indépendants du serveur.

### Conséquences
Les dépendances inverses ou cycliques sont interdites.

## ADR-004 — Séparation de Paper et de la logique métier

### Statut
Accepté.

### Contexte
Le métier doit être testable sans serveur Minecraft.

### Décision
Limiter Paper au module plugin et utiliser des abstractions dans le cœur.

### Raisons
Tests rapides, faible couplage et évolutivité.

### Conséquences
Les futurs accès serveur passeront par des adaptateurs explicites.

## ADR-005 — Documentation obligatoire entre les tickets

### Statut
Accepté.

### Contexte
Plusieurs humains et IA doivent reprendre le projet sans perte de contexte.

### Décision
Un ticket ne peut être terminé sans mise à jour de `AGENTS.md` et `docs/ai`.

### Raisons
Le code seul ne capture ni limites ni intentions.

### Conséquences
La documentation fait partie des critères de build fonctionnels du projet.

## ADR-006 — Pas de framework d'injection lourd

### Statut
Accepté.

### Contexte
Le socle possède peu de composants.

### Décision
Utiliser l'injection par constructeur et un registre léger ; ni Spring ni Guice.

### Raisons
Initialisation explicite et temps de démarrage réduit.

### Conséquences
Le bootstrap reste le composition root manuel.

## ADR-007 — Aucun accès nullable silencieux aux services obligatoires

### Statut
Accepté.

### Contexte
Un service obligatoire absent est une erreur de programmation.

### Décision
`require` lève `MissingServiceException`; `find` renvoie `Optional` pour les services réellement facultatifs.

### Raisons
Les échecs sont immédiats et explicites.

### Conséquences
Le registre ne renvoie jamais `null`.

## ADR-008 — Manifeste plugin.yml stable

### Statut
Accepté.

### Contexte
Le Ticket 001 n'a besoin que d'une commande Bukkit simple.

### Décision
Utiliser `plugin.yml` avec `api-version: 1.21` et enregistrer la commande via l'API Bukkit classique.

### Raisons
Solution stable commune à Spigot et Paper, suffisante pour ce périmètre.

### Conséquences
Les commandes simples ne doivent pas dépendre d'une API Paper. Une migration de manifeste nécessitera un ADR et devra préserver le JAR Spigot si cette cible reste prise en charge.

## ADR-009 — Licence propriétaire temporaire

### Statut
Accepté.

### Contexte
Aucune licence de redistribution n'a été choisie par le propriétaire.

### Décision
Interdire temporairement la redistribution sans autorisation.

### Raisons
Éviter d'imposer automatiquement MIT, GPL ou Apache.

### Conséquences
Une future licence permanente devra remplacer `LICENSE` et cet ADR.

## ADR-010 — Compilation agrégée du JAR final

### Statut
Accepté temporairement.

### Contexte
L'environnement Windows isolé de validation refuse à `javac` la relecture des dossiers de classes locaux entre tâches, bien que ces classes soient valides. Il verrouille aussi les JAR lors de l'analyse incrémentale Gradle.

### Décision
Conserver les dépendances de projets normales, désactiver les caches et la compilation incrémentale, compiler les sources principales avec les tests du cœur et agréger les sources API/cœur lors de la compilation du module plugin final.

### Raisons
Le build complet et ses tests restent reproductibles dans l'environnement pris en charge, sans modifier les contrats ni embarquer Paper.

### Conséquences
Le module final recompile API et cœur avant de produire le Shadow JAR. Cette mesure pourra être retirée dans un ticket ultérieur après validation sur une CI Windows/Linux non isolée.

## ADR-011 — YAML Bukkit sans dépendance supplémentaire

### Statut
Accepté.

### Décision
Utiliser `YamlConfiguration` uniquement dans `bedwars-plugin`; le cœur manipule des documents aplatis et immuables.

### Conséquences
Le JAR reste compatible Spigot/Paper sans bibliothèque embarquée. Les commentaires sont conservés tant que le fichier n'est pas réécrit; le changement de langue réécrit `config.yml` et ne garantit donc pas leur conservation parfaite.

## ADR-012 — Configurations Java immuables et snapshots transactionnels

### Statut
Accepté.

### Décision
Convertir les valeurs principales en records et construire un `ConfigurationSnapshot` complet avant activation atomique.

### Conséquences
Un reload invalide conserve intégralement l'état précédent; aucune valeur YAML mutable n'est exposée au reste du plugin.

## ADR-013 — MiniMessage limité rendu sans Adventure obligatoire

### Statut
Accepté.

### Décision
MiniMessage est le format recommandé, avec prise en charge explicite des couleurs nommées, décorations, hexadécimal et codes `&`. Le rendu produit les codes Minecraft legacy sans charger Adventure.

### Conséquences
Le plugin démarre sur Spigot 1.21. Les événements, gradients, hover/click et balises MiniMessage avancées ne sont pas pris en charge.

## ADR-014 — Version 1 obligatoire et migrations séquentielles futures

### Statut
Accepté.

### Décision
Chaque YAML contient `config-version: 1`. Une version absente ou inconnue refuse l'activation. `ConfigurationMigration` et `BackupService` préparent les migrations sans simuler de migration inutile.

### Conséquences
Toute migration future devra sauvegarder le fichier, écrire de façon sûre et avancer version par version.

## ADR-015 — Secrets exclus des diagnostics

### Statut
Accepté.

### Décision
`ConfigurationProblem` masque les clés contenant password, token, secret ou credential. `/bedwars config` n'affiche que le type de stockage.

### Conséquences
Les mots de passe ne sont jamais inclus dans les rapports ni dans les logs debug.

## ADR-016 — Migration étroite du format Ticket 001 non versionné

### Statut
Accepté.

### Contexte
Le Ticket 001 créait un `config.yml` officiel sans `config-version`, ce qui empêchait le Ticket 002 de démarrer lors d'une mise à jour normale.

### Décision
Considérer ce fichier comme version source 0 seulement s'il est lisible et contient la signature `plugin.language` chaîne non vide plus `plugin.debug` booléen. Sauvegarder, fusionner uniquement les défauts absents, puis écrire de façon sûre en version 1.

### Conséquences
Les installations Ticket 001 sont mises à niveau sans suppression de données. Un YAML vide, corrompu ou non reconnaissable n'est jamais « réparé » automatiquement. Les commentaires peuvent être reformattés par `YamlConfiguration`, limitation explicitement documentée.

## ADR-032 à ADR-037 — Édition administrative des arènes

### Statut
Accepté.

### Décisions

- **ADR-032** — La saisie administrative utilise une session de chat unique par joueur. Le message est annulé de façon asynchrone puis traité sur le thread serveur; timeout, annulation, déconnexion et arrêt terminent la session.
- **ADR-033** — Chaque modification d'arène est sauvegardée automatiquement. L'état mémoire n'est publié qu'après réussite de la persistance.
- **ADR-034** — Une révision persistée, initialisée à 1 et incrémentée par mutation réussie, fournit un verrouillage optimiste. Une révision attendue obsolète produit `CONFLICT` sans écriture.
- **ADR-035** — Commandes et menus utilisent la même instance injectée d'`ArenaService` afin de conserver validation, sauvegarde et conflits identiques.
- **ADR-036** — Les menus ne lisent et n'écrivent jamais directement `arenas/*.yml`; `ArenaRepository` reste derrière le service.
- **ADR-037** — Les téléportations administratives exigent `heneriabedwars.admin.arena.teleport`; aucune téléportation n'est une conséquence implicite d'une sauvegarde.

### Conséquences
L'éditeur reste testable sans Bukkit pour ses règles principales et deux administrateurs ne peuvent pas écraser silencieusement leurs modifications. Les opérations création, suppression, activation, monde, positions, capacité et équipes sont journalisées simplement; un audit persistant reste futur.

## ADR-038 à ADR-044 — Cartes modèles autonomes

### Statut
Accepté.

### Décisions

- **ADR-038** — Une carte modèle possède un identifiant normalisé `[a-z0-9_-]{2,32}`; tous les chemins sont reconstruits depuis cet identifiant et confinés aux racines configurées.
- **ADR-039** — Bukkit charge les mondes modèles dans son conteneur de mondes avec le préfixe contrôlé `hbw_template_`. `maps/templates/<id>/managed-world.txt` matérialise la propriété du plugin sans prétendre que Bukkit peut charger un monde depuis le dossier de données du plugin.
- **ADR-040** — Métadonnées, dossier de monde et état chargé sont distincts. Les états transitoires ne sont pas restaurés après redémarrage; la présence réelle dans Bukkit détermine `LOADED` ou `UNLOADED`.
- **ADR-041** — Chargement, sauvegarde, déchargement et téléportation utilisent le thread serveur. Les copies et suppressions lourdes utilisent le scheduler asynchrone, avec un verrou par carte.
- **ADR-042** — Une suppression produit obligatoirement une sauvegarde complète avec manifeste avant toute suppression. Une carte liée à une arène active ou protégée comme lobby est refusée.
- **ADR-043** — Les arènes sont la source de vérité des associations; les liens inverses de carte sont des données dérivées et réparables.
- **ADR-044** — Le Ticket 007 réserve `instances/` mais ne crée aucune instance de partie. Les cartes modèles ne sont jamais modifiées automatiquement par un match.

### Conséquences
Les opérations restent sûres face aux traversées de chemin, liens symboliques, copies partielles et vues concurrentes. Une installation qui change le préfixe ou les dossiers doit redémarrer complètement et ne doit pas déplacer manuellement un monde chargé.

## ADR-045 — Navigation administrative déterministe

### Statut
Accepté.

### Décision
Les retours des menus métier ciblent explicitement leur parent logique au lieu de dépendre uniquement de l'historique de session. Après suppression réussie d'une arène, le GUI ouvre une nouvelle session dont le tableau de bord est la racine et supprime la révision observée de cette arène.

### Conséquences
Une vue ouverte directement par commande, devenue obsolète après reload ou issue d'une confirmation ne peut plus renvoyer vers un éditeur supprimé. Le bouton retour conserve le même sens visuel dans tout l'assistant : sous-menu vers éditeur, éditeur vers liste, liste vers configuration.

## ADR-046 a ADR-050 - Runtime pre-game Ticket 010

### Statut

Accepte.

### Decisions

- **ADR-046** - Les snapshots de joueur avant partie restent uniquement en memoire. Ils sont restaures avant sortie, avec le monde de secours si le monde d'origine est absent; aucune reprise apres crash n'est simulee.
- **ADR-047** - `GameLobbyService` est la facade unique pour rejoindre, quitter, deconnecter, demarrer ou arreter une partie avant gameplay. Commandes, menus et listener n'appellent pas directement les transitions de partie.
- **ADR-048** - `GameCountdownService` ne planifie aucune tache. Une seule tache Bukkit centrale le fait avancer et nettoie les instances vides, ce qui borne le cout quand le nombre de parties augmente.
- **ADR-049** - Les protections de lobby sont evaluees par appartenance joueur et etat `WAITING`/`STARTING`; elles ne bloquent jamais globalement un monde administratif ou le lobby principal.
- **ADR-050** - Le passage `PLAYING` est autorise et observable mais ne demarre aucune mecanique BedWars. Les events de lobby sont internes Java et ne constituent pas une API Bukkit ou addon stable.

### Consequences

Le pre-game devient testable sans Paper pour les invariants de temps, d'index et de transitions. Les details Bukkit (inventaire, teleportation, affichage, protections) demandent toujours une matrice manuelle sur un serveur Paper. Le Ticket 011 peut ajouter les equipes detaillees sans casser le cycle de vie ou la restauration joueur.
