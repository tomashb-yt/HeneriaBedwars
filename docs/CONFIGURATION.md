# Configuration

**Statut :** configuration globale v1 opÃ©rationnelle au Ticket 002.

## Chargement

`config.yml` et `messages.yml` sont crÃ©Ã©s uniquement s'ils manquent. Un candidat est dÃ©sÃ©rialisÃ©,
validÃ© puis activÃ© atomiquement. Une erreur conserve le dernier snapshot valide. Les clÃ©s
MiniMessage livrÃ©es complÃ¨tent en mÃ©moire les clÃ©s utilisateur absentes sans modifier le fichier.

`/zombie reload` est refusÃ© pendant toute instance active. Le reload ne recrÃ©e ni lobby, ni monde,
ni pool d'exÃ©cution.

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
```

Les deux dossiers doivent Ãªtre des noms relatifs simples et distincts. `-1` signifie aucune limite
fonctionnelle fixe ; `0` est invalide.

## Chat, reconnexion et rÃ¨gles

`chat` active l'isolation des canaux lobby/instance et le canal administratif `!`.
`reconnect` configure le dÃ©lai, la rÃ©servation de place et le retour lobby aprÃ¨s expiration.
`world-rules` contrÃ´le apparitions naturelles, cycles, modifications de blocs, objets, PVP,
conservation d'inventaire et sauvetage du vide.

## ModÃ¨le de monde

Chaque modÃ¨le se trouve dans :

```text
<world-container>/zombie_templates/<mapId>/
```

Le dossier `zombie_templates` est crÃ©Ã© automatiquement dans la racine qui contient aussi `world`
et `zombie_lobby`. Son chemin absolu est journalisÃ© au dÃ©marrage. Il ne se trouve pas dans
`plugins/HeneriaZombie`.

Il s'agit d'un monde Paper valide contenant :

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

Le fichier doit Ãªtre nommÃ© `zombie-map.yml`. Les identifiants acceptent minuscules, chiffres,
underscore et tiret. Les liens symboliques sont refusÃ©s.

## Validation

Version, champs obligatoires, backend SQLite, chemins, cohÃ©rence du lobby, capacitÃ©s, dÃ©lais et
thÃ¨me sont validÃ©s. Un avertissement est journalisÃ© ; une erreur bloque l'activation.

