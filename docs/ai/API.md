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

`heneriabedwars.admin` est accordée aux opérateurs et possède les permissions spécialisées comme enfants. La complétion ne propose que les sous-commandes autorisées.

Le système de configuration reste interne. Ses points principaux sont `ConfigurationService`, `ConfigurationSnapshot`, `LanguageService`, `TranslationKey` et `PlaceholderContext`. L'API publique Ticket 001 (`HeneriaBedWarsApi`) est inchangée.

Le framework GUI et `ItemService` restent strictement internes. Ce sont des contrats du module plugin enregistrés dans `ServiceRegistry`, pas encore une API d'addons. Les modèles purs de `bedwars-core/item` permettent une future stabilisation sans exposer Bukkit prématurément.

`ArenaService` est également un service interne enregistré. `ArenaRepository` et les modèles de `bedwars-core/arena` ne sont pas encore exposés par `HeneriaBedWarsApi`; aucune API publique n'a donc été modifiée silencieusement.

L'API du Ticket 001 est volontairement minimale : `HeneriaBedWarsApi` expose seulement la version et l'état général, avec `PluginStatus`. Elle n'est pas encore publiée dans le `ServicesManager` Paper et ne constitue pas une API d'addons complète.

Les futurs contrats devront être stables, versionnés, documentés par JavaDoc et exposer des modèles immuables. Une API obsolète sera dépréciée avant suppression. Les événements publics seront documentés et aucune implémentation interne ne sera exposée inutilement.
