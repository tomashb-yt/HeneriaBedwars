# Configuration

## Cycle de vie

Les ressources par défaut sont embarquées dans `bedwars-plugin/src/main/resources`. Au premier démarrage, seules les ressources absentes sont copiées en UTF-8; aucun fichier existant n'est écrasé. Les dossiers `arenas`, `languages` et `backups` sont créés. Chaque YAML doit contenir `config-version: 1`.

`/bedwars reload` lit tous les fichiers, valide les références croisées et construit les records Java dans une structure temporaire. Le snapshot actif n'est remplacé que si aucune erreur `ERROR` ou `CRITICAL` n'existe. Les `WARNING` appliquent le défaut documenté. Le chargement reste synchrone car les fichiers sont petits et aucune API Bukkit n'est appelée hors du thread serveur.

`YamlConfiguration` tolère les clés personnalisées et aucune lecture ne réécrit un fichier. Une écriture demandée, actuellement le changement de langue, utilise un fichier temporaire et un remplacement atomique si possible. Cette réécriture ne garantit pas la conservation parfaite des commentaires.

## `config.yml`

| Clé | Type | Défaut / contrainte | Rechargeable | Sensible | État |
|---|---|---|---|---|---|
| `config-version` | entier | `1`, obligatoire | oui | non | actif |
| `plugin.language` | chaîne | `fr_FR`, fichier présent dans `languages/` | oui | non | actif |
| `plugin.debug` | booléen | `false` | oui | non | actif pour les diagnostics |
| `plugin.check-updates` | booléen | `false` | oui | non | préparatoire |
| `commands.main-command` | chaîne | `bedwars` | redémarrage | non | documentaire; `plugin.yml` reste autoritaire |
| `commands.aliases` | liste | `[hbw]` | redémarrage | non | documentaire; `plugin.yml` reste autoritaire |
| `security.confirm-dangerous-actions` | booléen | `true` | oui | non | préparatoire |
| `security.prevent-reload-during-games` | booléen | `true` | oui | non | préparatoire |
| `performance.warn-main-thread-task-duration-ms` | entier | `50`, supérieur à 0 | oui | non | préparatoire typé |

## `gameplay.yml`

Toutes les valeurs sont chargées dans `GameplaySettings`, mais aucun comportement de jeu n'est encore appliqué.

| Clé | Type | Défaut / contrainte |
|---|---|---|
| `config-version` | entier | `1` |
| `combat.profile` | chaîne | `legacy_1_8` |
| `combat.attack-cooldown-enabled` | booléen | `false` |
| `combat.shields-enabled` | booléen | `false` |
| `combat.friendly-fire` | booléen | `false` |
| `respawn.enabled` | booléen | `true` |
| `respawn.delay-seconds` | entier | `5`, positif ou nul |
| `respawn.protection-seconds` | entier | `3`, positif ou nul |
| `void.minimum-y` | entier | `-64` |
| `blocks.break-only-player-placed` | booléen | `true` |
| `blocks.restore-after-game` | booléen | `true` |

## `lobby.yml`

Toutes les valeurs sont rechargeables et préparatoires; aucune protection de lobby n'est enregistrée.

| Clé | Type | Défaut |
|---|---|---|
| `config-version` | entier | `1` |
| `main-lobby.configured` | booléen | `false` |
| `main-lobby.world` | chaîne | vide |
| `main-lobby.x/y/z` | nombre | `0.0` |
| `main-lobby.yaw/pitch` | nombre | `0.0` |
| `protection.cancel-block-break` | booléen | `true` |
| `protection.cancel-block-place` | booléen | `true` |
| `protection.cancel-damage` | booléen | `true` |
| `protection.cancel-hunger` | booléen | `true` |
| `items.enabled` | booléen | `true` |

## `storage.yml`

Ces valeurs sont chargées dans `StorageSettings`; aucune connexion SQLite, MySQL ou Redis n'est ouverte.

| Clé | Type | Défaut / contrainte | Sensible |
|---|---|---|---|
| `config-version` | entier | `1` | non |
| `storage.type` | chaîne | `sqlite`; `sqlite` ou `mysql` | non |
| `sqlite.file` | chaîne | `data.db` | non |
| `mysql.host` | chaîne | `localhost` | non |
| `mysql.port` | entier | `3306`, 1 à 65535 | non |
| `mysql.database` | chaîne | `heneriabedwars` | non |
| `mysql.username` | chaîne | `root` | non |
| `mysql.password` | chaîne | vide | oui, toujours masqué |
| `mysql.use-ssl` | booléen | `false` | non |
| `mysql.connection-timeout-ms` | entier | `10000`, supérieur à 0 | non |
| `redis.enabled` | booléen | `false` | non |
| `redis.host` | chaîne | `localhost` | non |
| `redis.port` | entier | `6379`, 1 à 65535 | non |

## `menus.yml` et `items.yml`

Ces fichiers préparent le Ticket 003. Aucun menu n'est encore ouvert.

| Fichier / clé | Type | Défaut / contrainte |
|---|---|---|
| `menus.yml: config-version` | entier | `1` |
| `global.default-size` | entier | `54`, multiple de 9 entre 9 et 54 |
| `global.fill-empty-slots` | booléen | `true` |
| `global.close-on-critical-error` | booléen | `true` |
| `global.play-click-sounds` | booléen | `true` |
| `pagination.previous-slot` | entier | `45`, dans l'inventaire |
| `pagination.next-slot` | entier | `53`, dans l'inventaire |
| `pagination.back-slot` | entier | `49`, dans l'inventaire |
| `items.yml: config-version` | entier | `1` |
| `items.menu-border.material` | chaîne | `GRAY_STAINED_GLASS_PANE`, matériau Bukkit valide |
| `items.menu-border.amount` | entier | `1`, de 1 à 99 |
| `items.menu-border.name` | chaîne | espace |
| `items.menu-border.lore` | liste | vide |
| `items.menu-border.glow` | booléen | `false` |
| `items.menu-border.custom-model-data` | entier ou null | `null` |

## Fichiers préparatoires

`shops.yml`, `upgrades.yml` et `generators.yml` ont `config-version: 1` et `enabled: false`. Leurs définitions ne déclenchent ni achat, ni amélioration, ni génération. `arenas/` reste vide jusqu'au ticket des arènes.

## Langues et messages

`languages/fr_FR.yml` et `languages/en_US.yml` contiennent exactement le même ensemble de clés et `config-version: 1`. Toutes les clés de `TranslationKey` sont requises. Une clé manquante ou une différence entre les catalogues refuse le reload. Une locale absente dans `config.yml` utilise `fr_FR` avec un warning; une locale inconnue refuse l'activation.

Le format recommandé est le sous-ensemble MiniMessage : couleurs nommées, `<bold>`, `<italic>`, `<underlined>` et `<#RRGGBB>`. Les codes `&c` et `&#RRGGBB` sont aussi acceptés. Les placeholders sont insérés après les couleurs afin qu'une valeur comme `<red>Alex` ne soit jamais interprétée. Placeholders généraux : `{player}`, `{plugin_version}`, `{java_version}`, `{server_version}`, `{service_count}`, `{language}`, `{state}`; les commandes ont aussi leurs compteurs dédiés. Un placeholder inconnu reste visible.

## Versions, migrations et sauvegardes

La version courante est 1. `ConfigurationMigration` définit les migrations séquentielles. Avant une migration ou un remplacement explicitement forcé, `BackupService` crée `backups/yyyy-MM-dd_HH-mm-ss/<fichier>` avec suffixe anti-collision. Les sauvegardes ne sont jamais créées à chaque lecture.

### Compatibilité Ticket 001

Le seul fichier historique officiellement migré est `config.yml`, car le Ticket 001 ne créait pas les autres YAML. Un fichier sans `config-version` est reconnu uniquement s'il est un YAML lisible avec une chaîne `plugin.language` non vide et un booléen `plugin.debug`. Cette signature étroite évite de traiter arbitrairement un fichier personnalisé comme officiel.

Une fois reconnu, le plugin :

1. charge le nouveau `config.yml` embarqué;
2. ajoute uniquement les clés par défaut absentes en conservant valeurs et clés inconnues;
3. crée une sauvegarde de l'original;
4. écrit le résultat via un fichier temporaire et un remplacement atomique si disponible;
5. charge normalement le snapshot version 1.

L'ancien `storage.type` éventuellement présent dans ce fichier est conservé, même si le réglage actif se trouve désormais dans `storage.yml`. La réécriture YAML ne peut pas garantir la conservation des commentaires. Si la sauvegarde ou l'écriture échoue, l'original n'est pas remplacé. Un fichier vide, corrompu, ou sans la signature Ticket 001 est refusé et laissé intact. Une configuration déjà versionnée n'est jamais remigrée.
