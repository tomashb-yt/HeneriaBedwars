# Configuration

**Statut :** configuration globale v1, GUI et maps éditoriales opérationnelles.

## Chargement

`config.yml` et `messages.yml` sont créés uniquement s'ils manquent. Un candidat est désérialisé,
validé puis activé atomiquement. Une erreur conserve le dernier snapshot valide. Les clés
MiniMessage livrées complètent en mémoire les clés utilisateur absentes sans modifier le fichier.

`/zombie reload` est refusé pendant toute instance active. Le reload ne recrée ni lobby, ni monde,
ni pool d'exécution.

## Lobby et instances

```yaml
lobby:
  world: zombie_lobby
  spawn:
    world: zombie_lobby
    x: 0.5
    y: 65.0
    z: 0.5
    yaw: 0.0
    pitch: 0.0

instances:
  worlds-directory: zombie_instances
  templates-directory: zombie_templates
  delete-world-after-game: true
  preserve-failed-worlds: true
  unload-delay-seconds: 5
  creation-timeout-seconds: 60
  maximum-concurrent-games: -1
  prevent-entry-without-session: true
  default-map-maximum-players: 4
```

Les deux dossiers doivent être des noms relatifs simples et distincts. `-1` signifie aucune limite
fonctionnelle fixe ; `0` est invalide.

## Chat, reconnexion et règles

`chat` active l'isolation des canaux lobby/instance et le canal administratif `!`.
`reconnect` configure le délai, la réservation de place et le retour lobby après expiration.
`world-rules` contrôle apparitions naturelles, cycles, modifications de blocs, objets, PVP,
conservation d'inventaire et sauvetage du vide.

Si Paper refuse de charger ou créer `lobby.world`, le plugin utilise `server.fallback-world` et
journalise un avertissement au lieu de se désactiver. Le monde dédié sera retenté au prochain
démarrage.

## GUI

`guis.yml` est installé dans `plugins/HeneriaZombie` et complété en mémoire par les valeurs
embarquées. Il configure thèmes, titres, tailles, matériaux, textes, lore, slots, permissions,
sons et actions. Un candidat invalide conserve le snapshot précédent.

```yaml
gui:
  session-timeout-seconds: 300
  input-timeout-seconds: 60
  confirmation-delay-ticks: 20
  refresh:
    maps-menu-ticks: 100
    instances-menu-ticks: 20
    diagnostics-menu-ticks: 40
```

Les lectures sont asynchrones et les rendus utilisent seulement le cache validé.

## Modèle de monde

Chaque modèle se trouve dans :

```text
<world-container>/zombie_templates/<mapId>/
```

Le dossier `zombie_templates` est créé automatiquement dans la racine qui contient aussi `world`
et `zombie_lobby`. Son chemin absolu est journalisé au démarrage. Il ne se trouve pas dans
`plugins/HeneriaZombie`.

Il suffit de déposer un monde Paper valide :

```text
zombie_templates/
└── crypt/
    ├── level.dat
    ├── region/
    └── ...
```

Le nom du dossier devient le `mapId` et doit contenir uniquement minuscules, chiffres, underscore
ou tiret. Le plugin lit le spawn vanilla de `level.dat` hors thread serveur. La capacité vaut
`instances.default-map-maximum-players`.

Un fichier facultatif `zombie-map.yml` peut surcharger cette capacité et le spawn :

```yaml
schema-version: 1
map-id: crypt
maximum-players: 4
spawn:
  x: 0.5
  y: 65.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0
```

Les liens symboliques sont refusés. `/zombie map list` rescane immédiatement les dossiers et
`/zombie map preview crypt` ouvre une copie temporaire sans modifier le modèle.

## Validation

Version, champs obligatoires, backend SQLite, chemins, cohérence du lobby, capacités, délais et
thème sont validés. Un avertissement est journalisé ; une erreur bloque l'activation.

## Définitions éditoriales

## Boucle de jeu

`game` configure joueurs minimum, compte à rebours, arrivée en cours et écran de fin. `players`
configure points, état à terre, hémorragie, réanimation et santé restaurée. `rounds` configure
limite, délais, formule de population, santé et budget d'apparition. Ces valeurs sont résolues
dans un `RoundConfiguration` immuable au démarrage ; un reload n'altère jamais une manche active.

L'éditeur gère automatiquement `plugins/HeneriaZombie/maps/<mapId>/map.yml` et son
`map.yml.bak`. Le schéma est en version 2 et n'est pas destiné à l'édition manuelle. Écriture
temporaire, backup et remplacement atomique s'exécutent hors thread serveur. Ce dossier est
distinct de `zombie_templates`, qui contient les mondes sources clonables.
