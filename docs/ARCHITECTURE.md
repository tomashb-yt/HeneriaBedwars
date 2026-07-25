# Architecture

**Statut :** fondation et instances isolées opérationnelles au Ticket 002.

## Modules

```text
zombie-api  <-  zombie-core  <-  zombie-plugin  -> Paper
```

- `zombie-api` expose uniquement des contrats publics stables et indépendants ;
- `zombie-core` contient agrégats, politiques, registres et services applicatifs sans Paper ;
- `zombie-plugin` adapte Paper : mondes, joueurs, événements, commandes et ordonnanceur.

Une dépendance remonte seulement vers la gauche. Les besoins inverses passent par un port du
consommateur, comme `WorldInstanceGateway`.

## Composition et durée de vie

`ZombieBootstrap` construit toutes les dépendances explicitement et les possède dans un
`ServiceRegistry` local. Il crée un pool I/O borné à deux threads et une seule tâche Paper
périodique pour l'expiration des reconnexions. L'arrêt annule la tâche, marque les instances
interrompues, renvoie les joueurs au lobby, décharge les mondes sans supprimer leurs fichiers,
libère l'API puis arrête le pool.

## Flux d'une instance

```text
commande
  -> InstanceCoordinator
  -> GameInstanceService
  -> WorldInstanceGateway
  -> copie I/O asynchrone
  -> chargement Paper sur thread serveur
  -> instance WAITING et accessible
```

`GameInstanceRegistry` est la source de vérité des instances. `PlayerSessionService` est la source
de vérité de l'appartenance joueur. Le monde courant d'un joueur ne remplace jamais la session
logique.

## Frontière de concurrence

- agrégats `GameInstance` et `PlayerSession` synchronisés pour leurs invariants ;
- registres concurrents et snapshots immuables pour la lecture ;
- réservation de capacité atomique pendant les créations concurrentes ;
- copie et suppression de dossiers sur le pool I/O ;
- création, téléportation et déchargement de mondes sur le thread Paper ;
- aucune instance joignable avant la fin du chargement.

## Isolation

Une politique pure compare deux sessions. `VisibilityService` applique `hidePlayer/showPlayer`,
ce qui isole également la tablist. `AsyncChatEvent` filtre ses viewers. `AudienceSelector` produit
les UUID exacts du lobby ou d'une instance ; `PaperAudienceService` les transforme en audience
Adventure compatible avec messages, sons, titres, action bars et boss bars.

Le lobby et chaque instance possèdent un scoreboard distinct. Les annonces globales de connexion,
déconnexion et mort sont supprimées puis redirigées vers le contexte exact.

## Configuration

`ConfigurationManager` installe les fichiers absents, charge un candidat immuable, le valide puis
échange atomiquement le snapshot. Les nouvelles clés de messages livrées sont fusionnées en
mémoire sans écraser les personnalisations. Le rechargement est refusé tant qu'une instance est
active, car modifier les chemins ou règles d'un monde vivant serait ambigu.

## Extensions futures

Le modèle `zombie-map.yml` du Ticket 002 est un adaptateur minimal. Le futur schéma de map
versionné devra s'intégrer derrière le catalogue sans déplacer de logique Paper dans le core.
Manches, zombies, matchmaking et GUI ne font pas partie de cette architecture livrée.
