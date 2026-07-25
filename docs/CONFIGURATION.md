# Configuration

**Statut :** configuration globale v1 opérationnelle au Ticket 002.

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
```

Les deux dossiers doivent être des noms relatifs simples et distincts. `-1` signifie aucune limite
fonctionnelle fixe ; `0` est invalide.

## Chat, reconnexion et règles

`chat` active l'isolation des canaux lobby/instance et le canal administratif `!`.
`reconnect` configure le délai, la réservation de place et le retour lobby après expiration.
`world-rules` contrôle apparitions naturelles, cycles, modifications de blocs, objets, PVP,
conservation d'inventaire et sauvetage du vide.

## Modèle de monde

Chaque modèle se trouve dans :

```text
<world-container>/zombie_templates/<mapId>/
```

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

Le fichier doit être nommé `zombie-map.yml`. Les identifiants acceptent minuscules, chiffres,
underscore et tiret. Les liens symboliques sont refusés.

## Validation

Version, champs obligatoires, backend SQLite, chemins, cohérence du lobby, capacités, délais et
thème sont validés. Un avertissement est journalisé ; une erreur bloque l'activation.
