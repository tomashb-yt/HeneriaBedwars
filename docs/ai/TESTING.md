# Tests

JUnit 5 est configuré dans les modules et les tests se trouvent sous `src/test/java`. La commande principale est `./gradlew test`; `./gradlew clean build` exécute aussi les tests et les contrôles de formatage.

Le Ticket 001 couvre le registre de services (5 tests) et le cycle de vie (8 tests). Le correctif commandes ajoute 5 tests pour l'aide principale, le diagnostic de version, le refus de permission, un expéditeur console et la tab-complétion permission-aware, soit 18 tests au total. Ils n'utilisent ni Bukkit ni serveur Minecraft.

Les tests automatisés ne prouvent pas l'enregistrement réel par le serveur. Après copie du nouveau JAR et redémarrage complet, vérifier `/plugins`, `/bedwars`, `/hbw`, les variantes `version`, la tab-complétion, un opérateur, un joueur sans permission et la console.

Les futurs tickets doivent tester la logique métier indépendamment de Paper, ajouter des tests d'intégration uniquement aux frontières utiles et documenter tout besoin de MockBukkit ou d'un vrai serveur.
