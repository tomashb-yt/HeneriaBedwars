# État actuel

- Dernier ticket terminé : Ticket 001
- Version actuelle : `0.1.0-SNAPSHOT`
- État du build : réussi avec Gradle 8.14.5
- État des tests : réussi, 18 tests

## Fonctionnalités disponibles

- projet Gradle multi-modules ;
- chargement initial Paper ;
- bootstrap et cycle de vie avec rollback ;
- registre de services typé ;
- configuration générale minimale ;
- commande Bukkit `/bedwars` et alias `/hbw` avec aide, diagnostic `version`, permission et tab-complétion ;
- documentation initiale et formatage Spotless.

## Fonctionnalités non disponibles

Arènes, parties, équipes, lits, générateurs, boutiques, menus, statistiques et base de données fonctionnelle ne sont pas implémentés.

## Prochaine étape recommandée

Ticket 002 : système complet de configuration, messages et traductions.

## Validation serveur

La compilation contre Spigot API 1.21 et les tests automatisés sont réussis. Le fonctionnement réel de `/bedwars`, `/hbw`, de la permission et de la console n'a pas encore été confirmé sur un serveur Spigot/Paper après redémarrage complet.
