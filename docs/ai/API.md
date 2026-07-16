# API publique

## Commandes internes disponibles

| Commande | Permission | Effet |
|---|---|---|
| `/bedwars`, `/hbw` | `heneriabedwars.admin` | aide traduite |
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

`heneriabedwars.admin` est accordée aux opérateurs et possède les permissions spécialisées comme enfants. La complétion ne propose que les sous-commandes autorisées.

`heneriabedwars.admin.arena.teleport` protège toutes les téléportations depuis l'éditeur. Les permissions existantes `arena.create`, `arena.edit`, `arena.enable`, `arena.disable`, `arena.delete`, `arena.list`, `arena.info` et `arena.menu` continuent de protéger chaque action au clic et en commande.

Le système de configuration reste interne. Ses points principaux sont `ConfigurationService`, `ConfigurationSnapshot`, `LanguageService`, `TranslationKey` et `PlaceholderContext`. L'API publique Ticket 001 (`HeneriaBedWarsApi`) est inchangée.

Le framework GUI et `ItemService` restent strictement internes. Ce sont des contrats du module plugin enregistrés dans `ServiceRegistry`, pas encore une API d'addons. Les modèles purs de `bedwars-core/item` permettent une future stabilisation sans exposer Bukkit prématurément.

`ArenaService` est également un service interne enregistré. `ArenaRepository` et les modèles de `bedwars-core/arena` ne sont pas encore exposés par `HeneriaBedWarsApi`; aucune API publique n'a donc été modifiée silencieusement.

`MapTemplateService`, `MapTemplateRepository`, `MapWorldService` et `MapFileService` restent aussi internes. Leurs modèles purs préparent une future API sans exposer Bukkit ni promettre une stabilité d'addon au Ticket 007.

L'API du Ticket 001 est volontairement minimale : `HeneriaBedWarsApi` expose seulement la version et l'état général, avec `PluginStatus`. Elle n'est pas encore publiée dans le `ServicesManager` Paper et ne constitue pas une API d'addons complète.

Les futurs contrats devront être stables, versionnés, documentés par JavaDoc et exposer des modèles immuables. Une API obsolète sera dépréciée avant suppression. Les événements publics seront documentés et aucune implémentation interne ne sera exposée inutilement.
