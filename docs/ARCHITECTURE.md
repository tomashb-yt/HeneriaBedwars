# Architecture

**Statut :** fondation, instances, GUI, éditeur et boucle de manches opérationnels au Ticket 005.

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

## Flux d'un aperçu de map

```text
zombie_templates/<mapId>/level.dat
  -> détection et lecture NBT sur le pool I/O
  -> copie temporaire par PaperWorldInstanceService
  -> chargement Paper sur le thread serveur
  -> téléportation de l'administrateur au spawn vanilla
  -> déchargement et suppression forcée à la sortie
```

`MapPreviewService` possède ces copies sans créer de `GameInstance`. Il autorise uniquement leur
administrateur dans la protection des mondes et nettoie une visite lors de la commande de sortie ou
d'une déconnexion.

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

## Frontière GUI

`GuiService` possède les sessions, la navigation et le rafraîchissement. Les registres injectés
d'écrans et d'actions rendent les modules extensibles. `GuiListener` ne contient aucune logique
métier. `GuiConfigurationService` installe et valide `guis.yml` sur le pool I/O, puis échange son
snapshot atomiquement. Les écrans délèguent les mutations aux services existants.

## Frontière éditeur

Le core possède définition immuable, registre, sessions, historique et validation.
`MapPersistence` est son port asynchrone. Le plugin adapte ce port en YAML, traduit les clics Paper
en `MapPoint` et compose les écrans avec le framework GUI. Une mutation remplace d'abord le
snapshot mémoire puis sérialise les écritures de la map sur le pool I/O.

## Moteur d'ennemis

Le domaine de partie est possédé par un `ZombieGame` par UUID d'instance. `ZombieGameService`
isole les agrégats ; `PaperGameRuntime` exécute un tick groupé. `ZombieDefinitionRegistry`,
`ZombieInstance`, `ZombieTracker`, `ZombieTargetSelector`, `ZombieDamageService` et les contrats
de barricade/dégâts sont dans `zombie-core`.

`PaperZombieEngine` est l'unique fabrique et adaptateur d'entités. Il utilise les API Paper stables
et l'IA native, sans NMS. `ZombieDefinitionLoader` effectue les accès disque hors thread et remplace
atomiquement le registre. Voir `ZOMBIE_ENGINE.md` et `ZOMBIE_AI.md`.

Le manifeste facultatif `zombie-map.yml` reste l'adaptateur technique de clonage. Une définition
éditoriale validée et un monde source homonyme alimentent désormais une partie de test. Le
matchmaking et l'activation publique des maps restent à livrer.
