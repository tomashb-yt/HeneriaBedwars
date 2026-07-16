# Tests

## Ticket 006

La suite contient 113 tests : 90 dans le cœur et 23 dans le module plugin, tous réussis. Les 25 nouveaux scénarios couvrent sessions de saisie (démarrage, validation, refus, annulation, timeout, déconnexion, concurrence), révision initiale et incrément, conflit optimiste, sauvegarde échouée sans publication, nom, monde, positions, joueurs, équipes, limites et suppression, état de liste (filtre, tri, page, refresh), routage des problèmes et apparences INFO/WARNING/ERROR/CRITICAL, compatibilité YAML des révisions et limites partielles, ainsi que refus transactionnel des slots d'éditeur en collision.

La non-diffusion du chat est garantie par l'annulation immédiate dans le listener avant la planification sur le thread serveur; l'exécution réelle de l'événement Bukkit reste dans la matrice manuelle. Aucun test en jeu n'a été effectué dans l'environnement Codex. Il faut notamment vérifier `/bedwars setup`, liste vide/pagination/filtres/tri, toutes les saisies, couleurs, téléportations, conflit avec deux comptes, sauvegarde de suppression et persistance après un arrêt `stop` puis redémarrage.

## Ticket 005

La suite contient 88 tests : 68 dans le cœur et 20 dans le module plugin. Les 18 nouveaux scénarios couvrent ids sûrs, immutabilité, statuts, validation requise/avertissements/capacités/mondes, remplacement atomique du registre, persistance avant publication, échec d'écriture ou suppression sans mutation mémoire, recalcul des équipes, reload préservant une ancienne définition illisible, complétion permission-aware, round-trip YAML UTF-8, isolation d'un fichier cassé, nom de fichier dangereux et sauvegarde datée avant suppression.

Les commandes Bukkit, positions réelles, existence des mondes, rendu des menus et permissions au clic compilent contre Spigot 1.21 mais nécessitent encore la matrice manuelle sur Paper. Aucun serveur ni MockBukkit compatible n'a été utilisé dans cet environnement.

## Ticket 004

La suite contient 70 tests : 54 dans le cœur et 16 dans le module plugin. Les nouveaux scénarios couvrent format/casse des clés, immutabilité, valeurs par défaut, contexte typé, registre, source GUI exclusive, héritage profond, surcharge, fusion, parent inconnu, cycles directs/indirects, profondeur déterministe même avec cache, matériaux/fallback, quantité, flags, enchantements sûrs, couleur incompatible, fallback critique, complétion des commandes, évolution sauvegardée d'un `items.yml` Ticket 003 et conservation du registre lors d'un reload cyclique.

MockBukkit n'a pas été ajouté : aucune version compatible avec la frontière Spigot/Paper 1.21 de ce dépôt n'a été établie. La construction réelle de `ItemStack`, les `Component`/métadonnées, glow, PDC, têtes, inventaire plein, preview et changements visuels après reload nécessitent la matrice manuelle du Ticket 004 sur Paper.

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
