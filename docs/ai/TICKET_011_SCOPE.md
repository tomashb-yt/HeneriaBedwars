# Ticket 011 - Equipes BedWars et navigation publique

Le Ticket 011 porte deux axes séparés dans le code, les tests et le rapport final : les équipes BedWars détaillées, puis le premier navigateur public de parties. Il inclut également la résolution publique par carte, le nettoyage de session après déconnexion et la refonte localisée de l'aide des commandes de parties. Il ne doit exposer aucune action administrative dans le parcours joueur.

## Entrée joueur

- `/bedwars` reste réservé au dashboard administratif;
- `/bw` et `/bw play` ouvrent l'accueil public;
- `/bedwars play` ouvre le même accueil;
- `/bw leave` et `/bedwars game leave` quittent la partie courante;
- permission publique `heneriabedwars.play`, accordée par défaut.

Le menu principal propose jouer maintenant, choisir une partie, informations et fermer. Un joueur déjà en partie voit les informations de sa partie et ne peut jamais rejoindre une seconde instance.

## Identité publique et résolution d'une partie

Le nom principal visible par un joueur est toujours le nom d'affichage de la carte. La règle de repli est : `MapTemplate.displayName`, puis `ArenaDefinition.displayName`, puis `MapTemplate.id`. L'UUID de l'instance, son identifiant court, son monde temporaire et la révision de la carte sont réservés aux logs, diagnostics et commandes d'administration.

`/bw join <carte>` et `/bedwars game join <carte>` acceptent un identifiant public stable, sans sensibilité à la casse. La complétion ne propose qu'une fois chaque carte, par exemple `desert`, `castle` et `lighthouse`, jamais un UUID ou un identifiant court concurrent pour la même carte.

`PublicGameReferenceResolver` résout ce choix public vers la meilleure instance rejoignable. Son résultat explicite distingue au minimum `SUCCESS`, `MAP_NOT_FOUND`, `NO_AVAILABLE_INSTANCE`, `AMBIGUOUS_MAP`, `GAME_FULL` et `GAME_NOT_JOINABLE`. La saisie manuelle d'un identifiant technique reste un outil administratif séparé, jamais la voie normale du joueur.

## Navigateur public

La liste est paginée, actualisable et filtrable par `TOUTES`, `DISPONIBLES`, `EN_ATTENTE`, `DEMARRAGE` et `PRESQUE_PLEINES`. Les tris sont meilleur choix, plus peuplées, moins peuplées et carte. Aucun enum technique (`ERROR`, `RESETTING`, `DESTROYED`) n'est affiché.

Chaque entrée indique carte, joueurs, minimum, capacité, état localisé, countdown et raison d'indisponibilité. Les instances sont regroupées par carte : une seule entrée `Desert` précise le nombre de parties disponibles, les joueurs cumulés et la meilleure capacité. Le clic gauche rejoint la meilleure instance; le clic droit peut ouvrir la liste détaillée des instances si elle apporte une vraie valeur. Deux entrées identiques sans distinction ne sont jamais affichées.

Le design principal n'affiche ni UUID, ni nom du monde temporaire, ni dossier, ni révision, ni état technique. Une référence courte peut figurer seulement dans un lore d'assistance. Le clic droit ouvre le détail pré-join, qui peut proposer rejoindre, retour et fermer. Ce détail est distinct du livre reçu après avoir rejoint, lequel reste uniquement informatif.

## Sélection et concurrence

`PublicGameSelector`, `GameSelectionPolicy` et `GameSelectionResult` classent les instances rejoignables sans dépendre du GUI. La priorité va aux parties `WAITING` déjà peuplées, non pleines, puis aux parties `STARTING` dont le countdown reste supérieur au minimum configuré. Si aucune partie n'existe, la stratégie minimale affiche un état vide; la création automatique reste un chantier matchmaking futur.

Le clic relit toujours l'instance et délègue à `GameLobbyService.join`. Le nombre affiché n'est jamais la source de vérité : deux joueurs sur la dernière place produisent un succès et un refus `GAME_FULL`, sans dépasser la capacité.

## Déconnexion, restauration et reconnexion sûre

Dans `WAITING` et `STARTING`, une déconnexion ou une expulsion est un départ complet. Le traitement de `PlayerQuitEvent` et `PlayerKickEvent` restaure l'état du joueur avant la fin de l'événement : interactions runtime annulées, items BedWars retirés, scoreboard et boss bar retirés, joueur supprimé de l'instance et de ses index, snapshot restauré, countdown ajusté puis destruction planifiée si la partie devient vide. Aucune téléportation n'est tentée pour un joueur hors ligne.

Le cycle du snapshot est atomique et contrôlé via une opération de type `PlayerRestoreResult restoreAndRelease(UUID playerId)`: après une restauration réussie, le snapshot est libéré et un second appel ne peut pas dupliquer d'objets. En cas d'échec, le problème est journalisé et les objets runtime identifiables sont malgré tout retirés, sans effacer les objets normaux du joueur.

Un filet de sécurité au `PlayerJoinEvent` retire les objets runtime orphelins, les anciens scoreboard et boss bars lorsqu'aucune instance active ne correspond au joueur; il peut restaurer un snapshot orphelin. Ce nettoyage reconnaît exclusivement une clé PDC valide `heneriabedwars:runtime_item`, jamais le seul matériau (`BOOK`, `COMPASS`, `RED_BED`, etc.). La reconnexion réelle en `PLAYING` n'est pas introduite par ce ticket : sa politique sera explicite et documentée, mais aucun item runtime ne doit rester dans le profil du joueur.

## Commandes et aide localisée

Le parcours joueur présente uniquement `/bw play`, `/bw join <carte>` et `/bw leave`, avec une description courte, un exemple et une aide traduite. Les erreurs incomplètes sont contextuelles : une carte absente indique l'usage de `/bw join <carte>` et les cartes disponibles, sans afficher toute l'aide administrative.

L'aide administrative sépare création, surveillance et contrôle : `create <arène>`, `list`, `info <partie>`, `start <partie>` et `stop <partie>`. `stop` devient l'action normale d'arrêt propre et remplace `destroy` dans toute aide standard; une éventuelle destruction forcée reste cachée et fortement protégée. Tous les textes, usages et erreurs sont ajoutés de manière symétrique dans `fr_FR.yml` et `en_US.yml`; aucun texte important d'aide n'est codé en dur en Java. Les composants cliquables sont souhaitables seulement s'ils ne demandent pas une architecture lourde et conservent toujours une commande textuelle visible.

## Configuration et présentation

`game.yml` ajoutera `public-game-browser` avec activation, quick play, countdown minimum, préférence de population, taille, visibilité des parties pleines et auto-refresh central de 40 ticks. `items.yml` utilisera les familles `play.main.*`, `play.browser.*` et `play.info.*`. Les catalogues FR/EN fourniront toutes les clés `play.menu`, `play.browser`, `play.selection`, `play.info` et `game.help`.

## Validation attendue

Les tests couvriront `/bw`, `/bedwars play`, joueur non administrateur, joueur déjà en partie, liste vide, états disponible/plein/countdown, changement après ouverture, double clic, dernière place concurrente, quick play, filtres, tris, pagination, refresh, traductions, absence d'administration et délégation à `GameLobbyService`.

Ils vérifient aussi la complétion publique unique par carte, la résolution insensible à la casse, le regroupement de plusieurs instances d'une même carte, les erreurs `MAP_NOT_FOUND` et `NO_AVAILABLE_INSTANCE`, ainsi que l'absence d'identifiants techniques dans l'interface publique. Les scénarios quit/kick/rejoin couvrent la restauration immédiate de l'inventaire, la suppression ciblée des seuls items PDC runtime, la suppression des scoreboard et boss bars, le nettoyage des index et l'ajustement du countdown. Enfin, l'aide est contrôlée pour un joueur, un administrateur, la console, les permissions partielles, les arguments manquants et les arguments invalides.
