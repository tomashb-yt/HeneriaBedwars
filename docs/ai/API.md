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

`heneriabedwars.admin` est accordée aux opérateurs et possède les permissions spécialisées comme enfants. La complétion ne propose que les sous-commandes autorisées.

Le système de configuration reste interne. Ses points principaux sont `ConfigurationService`, `ConfigurationSnapshot`, `LanguageService`, `TranslationKey` et `PlaceholderContext`. L'API publique Ticket 001 (`HeneriaBedWarsApi`) est inchangée.

Le framework GUI reste strictement interne pendant le Ticket 003. `GuiService` est un contrat du module plugin, pas une API d'addons. Cette surface sera réévaluée après plusieurs menus réels afin de ne pas figer prématurément navigation et modèles Bukkit.

L'API du Ticket 001 est volontairement minimale : `HeneriaBedWarsApi` expose seulement la version et l'état général, avec `PluginStatus`. Elle n'est pas encore publiée dans le `ServicesManager` Paper et ne constitue pas une API d'addons complète.

Les futurs contrats devront être stables, versionnés, documentés par JavaDoc et exposer des modèles immuables. Une API obsolète sera dépréciée avant suppression. Les événements publics seront documentés et aucune implémentation interne ne sera exposée inutilement.
