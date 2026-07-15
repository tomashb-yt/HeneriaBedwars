# Tests

JUnit 5 est configuré dans les modules et les tests se trouvent sous `src/test/java`. La commande principale est `./gradlew test`; `./gradlew clean build` exécute aussi les tests et les contrôles de formatage.

Le Ticket 001 couvre le registre de services (enregistrement, lecture, absence, doublon, optionnel) et le cycle de vie (démarrage, arrêt, ordres, rollback, état après échec et transitions interdites), soit 13 tests. Ils n'utilisent ni Paper ni serveur Minecraft.

Les futurs tickets doivent tester la logique métier indépendamment de Paper, ajouter des tests d'intégration uniquement aux frontières utiles et documenter tout besoin de MockBukkit ou d'un vrai serveur.
