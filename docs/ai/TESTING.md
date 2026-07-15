# Tests

## Ticket 003

La suite complète contient 57 tests : 45 dans le cœur et 12 dans le module plugin. Les 13 nouveaux tests purs couvrent construction/immutabilité, tailles/lignes/slots invalides, doublon et remplacement, routage des clics, permissions/conditions, navigation/retour/racine/limite, protections sessionId/viewId, pagination vide/multiple/réduite, rectangles, anti-spam, confirmation et interception d'exception avec session conservée. Un test d'évolution reproduit une installation Ticket 002 sans les nouvelles clés de menus/langues et vérifie fusion, trois sauvegardes et parité FR/EN; un test de commande vérifie aussi la permission propre à l'autocomplétion GUI.

MockBukkit n'a pas été ajouté sans preuve de compatibilité avec Paper 1.21 dans ce dépôt. L'ouverture réelle, les événements Bukkit, sons et manipulations anti-duplication doivent être validés manuellement sur le serveur Paper de test.

## Ticket 002

La suite contient 42 tests JUnit 5 : 31 dans le cœur et 11 dans le module plugin. Elle couvre installation non destructive, YAML valide/invalide, versions, défauts, ports, délais, tailles/slots de menus, matériaux, locale absente/inconnue, parité FR/EN, clé manquante, placeholders sûrs, couleurs classiques/hex, permissions/completion, snapshots transactionnels, rapports d'erreurs, absence de remplacement partiel et sauvegardes anti-collision.

Les tests de migration couvrent l'ancien `config.yml` sans version, la sauvegarde préalable, l'ajout de la version et des défauts, la conservation de la langue, du debug et des clés inconnues, un second démarrage sans nouvelle migration, le refus d'un fichier non reconnaissable, un YAML corrompu et une panne d'écriture simulée avec original préservé.

Les tests de logique n'exigent pas de serveur. Les scénarios `/plugins`, commandes, permissions, changement persistant de langue, YAML cassé puis redémarrage doivent encore être exécutés manuellement sur Spigot/Paper 1.21.x. Ne pas utiliser `/reload` de Bukkit comme validation principale.

JUnit 5 est configuré dans les modules et les tests se trouvent sous `src/test/java`. La commande principale est `./gradlew test`; `./gradlew clean build` exécute aussi les tests et les contrôles de formatage.

Le Ticket 001 couvre le registre de services (5 tests) et le cycle de vie (8 tests). Le correctif commandes ajoute 5 tests pour l'aide principale, le diagnostic de version, le refus de permission, un expéditeur console et la tab-complétion permission-aware, soit 18 tests au total. Ils n'utilisent ni Bukkit ni serveur Minecraft.

Les tests automatisés ne prouvent pas l'enregistrement réel par le serveur. Après copie du nouveau JAR et redémarrage complet, vérifier `/plugins`, `/bedwars`, `/hbw`, les variantes `version`, la tab-complétion, un opérateur, un joueur sans permission et la console.

Les futurs tickets doivent tester la logique métier indépendamment de Paper, ajouter des tests d'intégration uniquement aux frontières utiles et documenter tout besoin de MockBukkit ou d'un vrai serveur.
