# API publique

Le Ticket 012 ne modifie pas la surface publique : les snapshots existants reflètent maintenant kills, morts, final kills, lits détruits, état du lit et élimination. Les services de mutation des lits et morts restent internes afin de préserver leurs invariants atomiques.

Depuis le Ticket 009, `HeneriaBedWarsApi` est enregistré dans le `ServicesManager` Bukkit et expose des snapshots runtime immuables. Les sous-commandes historiques restent compatibles; aucune permission existante n'est supprimée.

## Commandes internes disponibles

| Commande | Permission | Effet |
|---|---|---|
| `/bedwars`, `/hbw` | `heneriabedwars.admin.dashboard` | ouvre le tableau de bord administratif; sans permission affiche l'aide joueur |
| `/bedwars version` | `heneriabedwars.admin` | versions, état et nombre de services |
| `/bedwars reload` | `heneriabedwars.admin.reload` | reload transactionnel des YAML |
| `/bedwars config` | `heneriabedwars.admin.config` | diagnostic sans secret |
| `/bedwars language` | `heneriabedwars.admin.language` | locale active et disponibles |
| `/bedwars language set <locale>` | `heneriabedwars.admin.language` | persistance et activation |
| `/bedwars gui`, `/hbw gui` | `heneriabedwars.admin.gui` | ouvre la démonstration GUI, joueur uniquement |
| `/bedwars item` | `heneriabedwars.admin.item` | aide des outils d'items |
| `/bedwars item list` | `heneriabedwars.admin.item` | nombre et vingt premières clés actives |
| `/bedwars item give <clé>` | `heneriabedwars.admin.item.give` | donne une copie au joueur si son inventaire a une place |
| `/bedwars item preview` | `heneriabedwars.admin.item.preview` | ouvre le registre paginé, joueur uniquement |
| `/bedwars arena create <id>` | `heneriabedwars.admin.arena.create` | crée et persiste un brouillon |
| `/bedwars arena list` | `heneriabedwars.admin.arena.list` | liste ids et statuts |
| `/bedwars arena info <id>` | `heneriabedwars.admin.arena.info` | affiche la définition |
| `/bedwars arena menu` | `heneriabedwars.admin.arena.menu` | ouvre la liste paginée |
| `/bedwars arena setworld|setwaiting|setspectator|setplayers|setteams|validate ...` | `heneriabedwars.admin.arena.edit` | modifie ou valide une définition |
| `/bedwars arena team list <arène>` | `heneriabedwars.admin.arena.edit` | affiche les équipes et l'état spawn/lit |
| `/bedwars arena team setspawn|clearspawn|teleport <arène> <équipe>` | `heneriabedwars.admin.arena.edit` / `.teleport` | configure ou visite le spawn ciblé |
| `/bedwars arena team setbed|clearbed|teleportbed <arène> <équipe>` | `heneriabedwars.admin.arena.edit` / `.teleport` | sélectionne le lit regardé, retire ou visite sa position |
| `/bedwars arena enable|disable <id>` | `heneriabedwars.admin.arena.enable` / `.disable` | change le statut administratif |
| `/bedwars arena delete <id>` | `heneriabedwars.admin.arena.delete` | ouvre la confirmation de sauvegarde/suppression |
| `/bedwars setup` | `heneriabedwars.admin.setup` | ouvre le menu principal d'administration, joueur uniquement |
| `/bedwars arena` | `heneriabedwars.admin.arena.menu` | ouvre directement la liste/éditeur, joueur uniquement |
| `/bedwars arena setmap <arène> <carte>` | `heneriabedwars.admin.arena.edit` | associe une carte modèle `BEDWARS` |
| `/bedwars map`, `/bedwars map menu` | `heneriabedwars.admin.map.menu` | ouvre la liste des cartes modèles |
| `/bedwars map create <id> [type]` | `heneriabedwars.admin.map.create` | crée une carte et un monde vide |
| `/bedwars map list`, `/bedwars map info <id>` | `heneriabedwars.admin.map` | liste ou inspecte les métadonnées |
| `/bedwars map load|save|unload <id>` | permissions `.map.load`, `.map.save`, `.map.unload` | gère le monde modèle |
| `/bedwars map unload <id> force` | `.map.unload` et `.map.force` | évacue vers le monde de secours puis décharge |
| `/bedwars map teleport <id>` | `heneriabedwars.admin.map.teleport` | charge si nécessaire puis téléporte au spawn |
| `/bedwars map setspawn <id>` | `heneriabedwars.admin.map.edit` | sauvegarde la position courante comme spawn |
| `/bedwars map duplicate <source> <destination>` | `heneriabedwars.admin.map.duplicate` | copie la carte en arrière-plan |
| `/bedwars map delete <id>` | `heneriabedwars.admin.map.delete` | ouvre la confirmation, sauvegarde puis supprime |
| `/bedwars game create <arène>` | `heneriabedwars.admin.game.create` | clone la carte, crée l'instance en `WAITING` et fait rejoindre le joueur |
| `/bedwars game list` | `heneriabedwars.admin.game.list` | liste les instances vivantes |
| `/bedwars game info <id-court ou UUID>` | `heneriabedwars.admin.game.info` | affiche arene, carte, etat, monde, joueurs, age et compteur |
| `/bedwars game join <arene ou id>` | `heneriabedwars.game.join` | rejoint une instance existante; sa création reste administrative |
| `/bedwars game leave` | `heneriabedwars.game.leave` | restaure le snapshot joueur puis quitte l'instance |
| `/bedwars game start <id> [--force]` | `heneriabedwars.admin.game.start` / `.force-start` | demarre le compteur ou force `PLAYING` pour un test |
| `/bedwars game stop <id>` | `heneriabedwars.admin.game.stop` | arrete, restaure les joueurs et nettoie le clone |
| `/bedwars game destroy <UUID>` | `heneriabedwars.admin.game.destroy` | alias historique de `stop` |

`heneriabedwars.admin` est accordée aux opérateurs et possède les permissions spécialisées comme enfants. La complétion ne propose que les sous-commandes autorisées.

`heneriabedwars.admin.arena.teleport` protège toutes les téléportations depuis l'éditeur. Les permissions existantes `arena.create`, `arena.edit`, `arena.enable`, `arena.disable`, `arena.delete`, `arena.list`, `arena.info` et `arena.menu` continuent de protéger chaque action au clic et en commande.

Le système de configuration reste interne. Ses points principaux sont `ConfigurationService`, `ConfigurationSnapshot`, `LanguageService`, `TranslationKey` et `PlaceholderContext`.

Le framework GUI et `ItemService` restent strictement internes. Ce sont des contrats du module plugin enregistrés dans `ServiceRegistry`, pas encore une API d'addons. Les modèles purs de `bedwars-core/item` permettent une future stabilisation sans exposer Bukkit prématurément.

`ArenaService` est également un service interne enregistré. `ArenaRepository` et les modèles de `bedwars-core/arena` ne sont pas encore exposés par `HeneriaBedWarsApi`; aucune API publique n'a donc été modifiée silencieusement.

`MapTemplateService`, `MapTemplateRepository`, `MapWorldService` et `MapFileService` restent aussi internes. Leurs modèles purs préparent une future API sans exposer Bukkit ni promettre une stabilité d'addon au Ticket 007.

`HeneriaBedWarsApi` expose `version`, `status`, `games`, `players` et `arenas`. `GameApi`, `PlayerGameApi` et `ArenaGameApi` sont en lecture seule et retournent `GameSnapshot`, `RuntimePlayerSnapshot` et `RuntimeTeamSnapshot`. Un addon récupère l'API avec `Bukkit.getServicesManager().load(HeneriaBedWarsApi.class)`. Les mutations restent volontairement internes afin de préserver les invariants de cycle de vie.

Les futurs contrats devront être stables, versionnés, documentés par JavaDoc et exposer des modèles immuables. Une API obsolète sera dépréciée avant suppression. Les événements publics seront documentés et aucune implémentation interne ne sera exposée inutilement.
