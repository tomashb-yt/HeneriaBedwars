# Tests et vérifications

**Statut :** socle automatisé et démarrage Paper du Ticket 001 validés.

## Objectif et périmètre

Définir une validation reproductible avant chaque ticket.

## Automatisation

`.\gradlew.bat test` exécute JUnit 5. Les tests actuels couvrent registre, ordre/rollback/arrêt du
cycle de vie, parser de commande, validation, valeurs par défaut, installation des fichiers,
reload transactionnel et API de statut. `spotlessCheck` contrôle le formatage et `qualityGate`
agrège formatage, checks et JAR ombré.

## Vérification manuelle

Lancer `.\gradlew.bat :zombie-plugin:runServer`, accepter l'EULA du serveur de test, vérifier
l'activation et l'arrêt, puis essayer `/zombie`, `help`, `reload` et un YAML invalide. Aucun gameplay
ne doit apparaître au Ticket 001.

Validation du Ticket 001 effectuée sur Paper 1.21.11 build 132 et Java 21 : activation, commande
d'information, aide, reload sans avertissement et arrêt propre réussis.

## À compléter

Tests d'intégration Paper, budgets de performance, simulations multi-instances et restauration
après panne.
