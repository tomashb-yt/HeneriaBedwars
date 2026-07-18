# Configuration

## Ticket 016 — `gameplay.yml`

Le profil `legacy_1_8` active les adaptations de combat uniquement pour les membres d'une instance `PLAYING`. Les nouvelles clés de combat sont ajoutées non destructivement aux anciens fichiers avec sauvegarde. Les valeurs sont lues à chaque événement; vitesse d'attaque et fenêtre d'invulnérabilité sont appliquées au début/respawn puis restaurées à la sortie.

## Ticket 015 — `shops.yml` et `upgrades.yml`

Une offre d'équipement ajoute `kind: ARMOR|PICKAXE|AXE|SHEARS` et un `tier` positif. `ITEM` reste la valeur par défaut et n'utilise aucun niveau. La catégorie `TOOLS` rejoint les quatre rayons historiques. Les paliers d'outil doivent être présents dans l'ordre; un achat ne peut pas sauter le niveau précédent.

`upgrades.runtime-enabled` active le second PNJ. La clé historique `enabled` reste lisible dans les anciens fichiers mais ne pilote pas le runtime. Chaque entrée `upgrades.definitions.<type>` définit `currency`, la liste `prices`, `translation-key` et `order`. Les types livrés sont `sharpness`, `protection` et `haste`. La position du PNJ vit sous `teams.<id>.upgrade-shop` dans le YAML d'arène. Les niveaux achetés ne sont jamais persistés.

`upgrades.yml`, `shops.yml`, `items.yml` et les deux langues évoluent non destructivement au démarrage, avec sauvegarde préalable.

## Correctif gameplay 012–014

`lobby.yml > main-lobby` devient la destination prioritaire après une partie. Si ce point n'est pas configuré ou si son monde est absent, le joueur revient au spawn du monde `worlds.yml > fallback-world`. Un départ volontaire conserve la restauration exacte de la position précédente.

Les nouvelles apparences `shop.*-v2`, traductions `shop.menu-v2`, `shop.purchase-v2`, `game.design-v15` et noms `generator.item.*` sont fusionnés sans écraser les personnalisations existantes. `shops.yml` continue d'utiliser `WHITE_WOOL` pour l'offre logique : la couleur réelle est choisie à l'achat depuis l'équipe.

## Ticket 014 — `shops.yml`

`shops.runtime-enabled` active la création des PNJ et le catalogue runtime. Chaque entrée `shops.offers.<id>` définit `category`, `material`, `amount`, `currency`, `price`, `translation-key` et `order`. Les catégories sont `BLOCKS`, `COMBAT`, `RANGED`, `UTILITY`; les monnaies sont `IRON`, `GOLD`, `DIAMOND`, `EMERALD`. Les identifiants suivent `[a-z0-9_-]{2,32}` et les valeurs numériques doivent être positives. Une offre invalide est ignorée avec avertissement, sans invalider les autres offres.

La position du PNJ est persistée dans `arenas/<id>.yml`, sous l'équipe concernée via la section `shop`. Le monde enregistré doit être le monde modèle de l'arène; au runtime seules les coordonnées et l'orientation sont remappées dans le clone.

Depuis le correctif 013/014, un ancien `shops.yml` reçoit au redémarrage les clés embarquées absentes après sauvegarde de l'original. Une valeur déjà personnalisée, y compris `runtime-enabled`, n'est jamais remplacée.

## Ticket 013 — générateurs

`generators.yml` définit `generators.merge-radius` et, pour `iron`, `gold`, `diamond` et `emerald`, le matériau Bukkit, l'intervalle en ticks, la quantité, la capacité locale et la stratégie d'empilement. Ces valeurs servent uniquement lors de l'ajout d'un nouveau point depuis l'assistant; le résultat complet est ensuite persisté dans `arenas/<id>.yml` sous `generators.definitions`.

`menus.yml` ajoute `arena-editor.assistant-v5.generators-slot`. Les apparences `arena.editor.assistant-generators-v7` et `arena.generator.*-v7` sont fusionnées dans les installations existantes. Les anciennes arènes sans section `generators` restent valides avec une liste vide.

Le correctif UX v8 ajoute de nouvelles clés d'items et de langue sans écraser les personnalisations v7. Le menu affiche les comptes par ressource, le monde attendu, un état vide guidé et le nombre de minerais regroupés sur chaque bloc. Plusieurs ressources différentes peuvent partager un point.

La clé historique `generators.enabled` n'active ni ne désactive globalement le système : une arène produit uniquement les générateurs explicitement placés dans son assistant et uniquement pendant `PLAYING`.

`generators.pacing.enabled` active le rythme adaptatif. `minimum-factor` vaut `0.85` et limite l'accélération à 15 %; `maximum-factor` vaut `1.60` et limite le ralentissement à 60 %. Le facteur dépend du rapport entre équipes configurées et joueurs présents au début du gameplay. `generators.holograms.enabled` et `height` contrôlent les compteurs diamant/émeraude. Ces nouvelles clés sont fusionnées au redémarrage avec sauvegarde de l'ancien fichier.

## Gameplay Ticket 012

`game.yml` ajoute les effets de lits, la politique `AT_DEATH`, le crédit de kill, la reconnexion désactivée et `game.ending.duration-seconds`. Le scoreboard v7 utilise les nouveaux blocs `waiting-v7.lines`, `starting-v7.lines` et `playing-v7.lines`, ainsi que `title-v7`, afin que les installations existantes reçoivent le nouveau design sans écraser leurs anciennes clés. Les placeholders de jeu incluent `{team_color}`, `{team_name}`, `{bed_status}`, `{remaining_teams}`, `{kills}` et `{beds_destroyed}`. Les délais de respawn et de protection continuent d'utiliser les valeurs typées historiques de `gameplay.yml`.

Les nouvelles clés sont fusionnées au démarrage dans les installations existantes. Une ancienne équipe qui ne conserve que le pied du lit est considérée incomplète : ouvrir sa fiche et sélectionner de nouveau le lit enregistre les deux moitiés et la direction.

## Assistant d'arène v5 et import de carte

`menus.yml` utilise `arena-editor.assistant-v5` pour la vue essentielle sur cinq lignes. Les clés règlent les slots `information`, `world`, `waiting`, `spectator`, `teams`, la liste `team-slots`, puis `validation`, `enable`, `delete`, `back` et `close`. Le nouveau namespace est fusionné dans les anciennes installations sans écraser leur ancien bloc `arena-editor.editor`.

Une création ou un reload assure l'existence de `maps/templates/<id>/import/LISEZ-MOI.txt`. Le contenu du monde externe doit être copié dans `import/` avec `level.dat` à sa racine. Aucun réglage n'autorise un chemin externe; les dossiers du monde actif restent sous le conteneur Bukkit et les archives sous `backups/maps/`.

## Cycle de vie

Les ressources par défaut sont embarquées dans `bedwars-plugin/src/main/resources`. Au premier démarrage, seules les ressources absentes sont copiées en UTF-8; aucun fichier existant n'est écrasé. Les dossiers `arenas`, `languages`, `backups`, `maps/templates`, `maps/metadata` et `instances` sont créés. Chaque YAML doit contenir `config-version: 1`.

`/bedwars reload` lit tous les fichiers, valide les références croisées et construit les records Java dans une structure temporaire. Le snapshot actif n'est remplacé que si aucune erreur `ERROR` ou `CRITICAL` n'existe. Les `WARNING` appliquent le défaut documenté. Le chargement reste synchrone car les fichiers sont petits et aucune API Bukkit n'est appelée hors du thread serveur.

`YamlConfiguration` tolère les clés personnalisées et aucune lecture ne réécrit un fichier. Une écriture demandée, actuellement le changement de langue, utilise un fichier temporaire et un remplacement atomique si possible. Cette réécriture ne garantit pas la conservation parfaite des commentaires.

Au premier démarrage du Ticket 003, `menus.yml`, `languages/fr_FR.yml` et `languages/en_US.yml` existants reçoivent uniquement les nouvelles clés embarquées absentes. Chaque fichier modifié est sauvegardé avant écriture; aucune valeur personnalisée existante n'est remplacée. Cette évolution ciblée permet une mise à jour directe depuis le Ticket 002 malgré le maintien de `config-version: 1`.

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

Toutes les valeurs sont chargées dans `GameplaySettings` et appliquées au runtime BedWars.

| Clé | Type | Défaut / contrainte |
|---|---|---|
| `config-version` | entier | `1` |
| `combat.profile` | chaîne | `legacy_1_8` |
| `combat.attack-cooldown-enabled` | booléen | `false` |
| `combat.shields-enabled` | booléen | `false` |
| `combat.friendly-fire` | booléen | `false` |
| `combat.hit-invulnerability-ticks` | entier | `10`, de 0 à 40 |
| `combat.kill-credit-seconds` | entier | `10`, de 1 à 60 |
| `combat.knockback.horizontal` | nombre | `0.40`, de 0 à 2 |
| `combat.knockback.vertical` | nombre | `0.40`, de 0 à 2 |
| `combat.knockback.sprint-multiplier` | nombre | `1.15`, de 0 à 3 |
| `combat.knockback.projectile-multiplier` | nombre | `1.00`, de 0 à 3 |
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

## `game.yml`

Toutes les valeurs sont rechargeables dans `GameSettings`. Elles agissent uniquement sur les instances runtime `WAITING` et `STARTING`; elles ne modifient pas le lobby principal ni une carte modele administrative.

| Cle | Defaut | Effet |
|---|---:|---|
| `game.waiting.game-mode` | `ADVENTURE` | mode impose dans le lobby de partie |
| `game.waiting.protect-players` | `true` | bloque les actions de protection runtime |
| `game.waiting.disable-*` | `true` | faim, drop et pickup controles pendant l'attente |
| `game.waiting.void-rescue-y` | `0` | hauteur de retour au spawn d'attente |
| `game.waiting.destroy-empty-instance` | `true` | active le nettoyage d'une partie vide |
| `game.waiting.empty-destroy-delay-seconds` | `30` | delai avant destruction d'une partie vide |
| `game.countdown.normal-seconds` | `30` | duree au minimum de joueurs atteint |
| `game.countdown.full-game-seconds` | `10` | duree reduite si capacite maximale atteinte |
| `game.countdown.cancel-below-minimum` | `true` | annule `STARTING` si le minimum est perdu |
| `game.countdown.allow-join-during-countdown` | `true` | autorise l'entree en `STARTING` |
| `game.countdown.announcements.*` | listes | secondes annoncees dans chat et titres |
| `game.countdown.bossbar.*` | active, bleu, solide | bossbar facultative du compteur |
| `game.scoreboard.*` | actif, 20, `play.heneria.fr` | affichage et cadence de rafraichissement |
| `game.inventory.leave-slot/info-slot` | `8` / `4` | positions hotbar, distinctes et de 0 a 8 |
| `game.forced-start.enabled` | `true` | autorise le test `start --force` |

Le correctif 010.1 remplace les anciennes lignes fixes par `game.scoreboard.title`, `waiting.lines` et `starting.lines` (1 à 15 lignes, lignes vides autorisées), et ajoute `hide-red-numbers`, `update-interval-ticks`, `server-name`, `server-address` et `game.waiting.items.interaction-cooldown-millis`. Les placeholders couvrent ids partie/arène/carte, joueurs, minimum/maximum/manquants, état localisé, couleur, statut, countdown et serveur. L'évolution ajoute uniquement les clés absentes à un ancien `game.yml` après sauvegarde.

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

`menus.yml` configure le framework GUI; `items.yml` contient le registre Ticket 004 complet. Les deux sont rechargeables par `/bedwars reload`.

Les apparences `arena.teams.*` couvrent désormais le résumé, les laines des douze couleurs, les états spawn/lit et toutes les actions de la fiche d'équipe. Les nouvelles clés sont fusionnées dans un ancien `items.yml` sans remplacer les personnalisations existantes.

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
| `pagination.page-indicator-slot` | entier | `50`, dans l'inventaire |
| `navigation.history-enabled` | booléen | `true` |
| `navigation.max-history-size` | entier | `20`, de 0 à 100 |
| `interaction.default-click-cooldown-ms` | entier | `150`, de 0 à 60000 |
| `interaction.cancel-player-inventory-clicks` | booléen | `true` |
| `interaction.cancel-drag-events` | booléen | `true` |
| `refresh.enabled` | booléen | `true` |
| `refresh.minimum-interval-ticks` | entier | `10`, supérieur à 0 |
| `sounds.enabled` | booléen | `true` |
| `sounds.<id>.sound` | chaîne | son Bukkit valide; open/click/success/error/back/close |
| `sounds.<id>.volume` | nombre | positif ou nul |
| `sounds.<id>.pitch` | nombre | de 0 à 2 |
| `arena-editor.list.*` | tailles, slots et liste de slots | mise en page de la liste, valeurs bornées à l'inventaire |
| `arena-editor.editor.*` | taille et slots | mise en page de l'éditeur, valeurs bornées à l'inventaire |
| `text-input.timeout-seconds` | entier | durée positive avant annulation de la saisie |
| `text-input.cancel-keywords` | liste de chaînes | mots d'annulation normalisés, non vides |
| `items.yml: config-version` | entier | `1` |
| `fallback-item` | section item | fallback sûr obligatoire; `BARRIER` par défaut |
| `items.<clé>` | section item | clé normalisée `[a-z0-9][a-z0-9._-]*` |

### Propriétés de `items.yml`

| Propriété | Type / défaut | Contraintes et fallback |
|---|---|---|
| `material` | chaîne, parent ou fallback | nom Bukkit insensible à la casse; inconnu/air devient le matériau fallback avec warning |
| `amount` | entier, `1` | de 1 à la taille maximale du stack; sinon `1` avec warning |
| `name` | chaîne, vide | texte direct au format Ticket 002; exclusif avec `name-key` |
| `name-key` | chaîne, absent | clé des catalogues; absence dans une locale produit un warning et un diagnostic visible |
| `lore` | liste de chaînes, vide | remplace complètement le lore parent; exclusif avec `lore-keys` |
| `lore-keys` | liste de clés, vide | chaque ligne est traduite dans la locale du contexte |
| `glow` | booléen, `false` | ajoute `UNBREAKING` masqué uniquement sans enchantement réel |
| `unbreakable` | booléen, `false` | appliqué via `ItemMeta#setUnbreakable` |
| `custom-model-data` | entier supérieur ou égal à zéro, ou `null`, absent | ne fournit ni n'impose un resource pack |
| `item-flags` | liste, vide | flags Bukkit connus; inconnus ignorés avec warning; fusion sans doublon à l'héritage |
| `enchantments.<clé>` | entier, vide | enchantements connus et niveaux sûrs uniquement; fusion avec priorité enfant |
| `leather-color` | `#RRGGBB` ou section RGB, absent | canaux 0..255; ignorée avec warning hors matériau cuir |
| `head.type/value` | `context-player` ou `player`, absent | contexte par UUID; propriétaire statique appliqué seulement s'il est en ligne, sans réseau |
| `inherit` | clé, absent | parent unique, profondeur maximale 16, parent inconnu/cycle = reload refusé |
| `tags.<clé>` | chaîne, vide | métadonnées immuables; seuls `category` et `action` sont autorisés vers le PDC |
| `required-placeholders` | liste, vide | construction normale échoue si une valeur obligatoire manque; `buildOrFallback` affiche le fallback |

Les placeholders sont insérés après le rendu des couleurs : une valeur dynamique telle que `<red>Alex` reste littérale et ne peut pas injecter de style. Le contexte supporte joueur optionnel, locale, menu, page, nombres, booléens et valeurs futures sans dépendre d'une arène. Chaque construction relit le registre en mémoire, jamais le YAML, et crée un `ItemStack` neuf.

L'évolution depuis le Ticket 003 sauvegarde puis complète uniquement les clés absentes de `items.yml`; les personnalisations existantes ne sont pas écrasées. Au reload, langues, définitions, héritage et validation sont préparés ensemble. Une erreur `ERROR/CRITICAL` conserve l'ancien snapshot; les menus ouverts ne ferment pas et reflètent les nouvelles définitions valides à leur prochain rafraîchissement.

## Définitions `arenas/<id>.yml`

Chaque arène possède un fichier UTF-8 version 1. L'id doit correspondre au nom du fichier et à `[a-z0-9_-]{2,32}`. Les champs persistés couvrent `revision`, `display-name`, `status`, `enabled`, `world`, `template`, `environment`, `players.minimum/maximum`, `teams.count/players-per-team`, `locations.waiting/spectator`, `boundary.enabled` et ses points optionnels, ainsi que les métadonnées de création/modification. Une ancienne définition sans `revision` est lue en révision 1.

Une arène activable exige un monde chargé, une position d'attente, des capacités positives et cohérentes (`maximum = teams × players-per-team`) et des positions dans le monde configuré. La position spectateur absente produit un avertissement. `/bedwars reload` recharge les fichiers indépendamment : un YAML illisible conserve son ancienne définition active connue, tandis qu'une définition lisible mais invalide reste inspectable. La suppression crée d'abord `backups/arenas/yyyy-MM-dd/<id>.yml`.

## `worlds.yml`

Le Ticket 009 consomme `directories.instances`, `naming.instance-world-prefix`, `fallback-world` et les exclusions de copie. Les valeurs par défaut produisent un manifeste sous `instances/game-<UUID>/` et un monde Bukkit `hbw_game_<UUID compact>`. Modifier le préfixe ou les racines avec des instances vivantes est interdit opérationnellement et demande un redémarrage complet.

`WorldManagerSettings` est rechargé transactionnellement. Les dossiers doivent être relatifs et sûrs; un chemin absolu ou contenant `..` refuse le snapshot. Changer les racines ou préfixes pendant que des mondes sont chargés exige un redémarrage complet.

| Clé | Défaut / contrainte |
|---|---|
| `world-manager.enabled` | `true` |
| `directories.templates/metadata/instances/backups` | chemins relatifs confinés |
| `naming.template-world-prefix` | `hbw_template_` |
| `naming.instance-world-prefix` | `hbw_game_`, réservé |
| `fallback-world` | `world`, utilisé pour évacuer lors d'un déchargement forcé |
| `void-world.create-safety-platform` | `true` |
| `void-world.platform-material/radius/y` | `GLASS`, `3`, `64` |
| `defaults.environment/difficulty` | `NORMAL`, `PEACEFUL` |
| `defaults.fixed-time/clear-weather/pvp` | `6000`, `true`, `false` |
| `defaults.animals/monsters` | `false`, `false` |
| `auto-save.enabled/interval-minutes` | `false`, `10` |
| `unload.save-before-unload/refuse-if-players-present` | `true`, `true` |
| `copy.excluded-files` | `uid.dat`, `session.lock` |
| `copy.excluded-directories` | `playerdata`, `stats`, `advancements` |

Les métadonnées sont enregistrées dans `maps/metadata/<id>.yml`. Le dossier `maps/templates/<id>/` contient un marqueur de propriété; le monde Bukkit actif se trouve sous le conteneur de mondes du serveur avec son nom préfixé. Les sauvegardes de suppression vont dans `backups/maps/<horodatage>/<id>/`. Voir `WORLD_SYSTEM.md`.

Depuis le Ticket 008, chaque carte persiste aussi `daylight-cycle`, `weather-cycle`, `fire-tick` et `environmental-damage`. Les anciens fichiers sans ces clés reçoivent des valeurs sûres à la lecture. `menus.yml` contient les emplacements versionnés `map-editor` pour la liste, l'éditeur, les réglages, les associations et la validation. Les apparences correspondantes utilisent les clés `map.*-v4` de `items.yml`.

## Fichiers préparatoires

`upgrades.yml`, `shops.yml` et `generators.yml` sont actifs pour leurs systèmes runtime respectifs. `arenas/` est alimenté exclusivement par les services et assistants administratifs; seules les positions des deux PNJ y sont persistées, jamais les achats vivants.

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
