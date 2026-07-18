# Tests

## Ticket 017 — statistiques persistantes

Les tests purs vérifient agrégation, ratios, victoires/défaites, séries, valeurs impossibles et double enregistrement. Le test SQLite crée une vraie base, écrit deux participants, refuse le doublon, ferme le dépôt puis confirme les mêmes agrégats après réouverture. La configuration refuse aussi un chemin SQLite sortant du dossier du plugin, et la permission publique `stats` est couverte.

Validation complète : 210 tests réussis, aucun échec, aucune erreur et aucun test ignoré; build propre, Spotless et Shadow JAR validés. La matrice Paper doit vérifier création de `data.db`, profil vide, victoire à deux joueurs, compteurs des deux camps, `/bw stats`, redémarrage serveur et absence de double comptage.

## Ticket 016 — combat et dégâts

Les tests purs couvrent les états non jouables, spectateurs, protection de respawn, friendly-fire, instances différentes et ennemis autorisés. Un scénario supplémentaire confirme que `respawn.enabled: false` force une mort finale. La configuration vérifie les nouvelles valeurs et leur évolution non destructive avec sauvegarde.

Validation : 204 tests automatisés réussis, aucun échec ni erreur. La matrice Paper doit confirmer vitesse d'attaque, absence de sweep/bouclier, dégâts des épées, knockback normal/sprint/flèche, protection de trois secondes, allié invulnérable, ennemi tuable, chute avec kill-credit, message `FINAL KILL` et restauration après retour lobby.

## Ticket 015 — équipement et améliorations

Les tests purs couvrent l'ordre obligatoire des outils, leur régression à la mort, la permanence de l'armure et des cisailles, le paiement par niveau, l'absence de mutation sur échec, le niveau maximum et l'événement interne. Les tests d'arène vérifient l'ajout/retrait du second PNJ; la configuration vérifie l'évolution non destructive de `upgrades.yml`, des offres et des deux langues.

Validation complète : 200 tests automatisés réussis, aucun échec ni erreur; build, Spotless et Shadow JAR validés.

Matrice Paper requise : couleur d'armure pour deux équipes, achats des quatre paliers, mort et régression, respawn avec loadout, Tranchant partagé sur toutes les épées, Protection partagée sur toute l'armure, Hâte partagée, solde insuffisant, niveau maximum, destruction du clone et nouvelle partie sans ancienne progression.

## Correctif gameplay 012–014

Les nouveaux scénarios vérifient le registre match-scoped des blocs placés, l'impossibilité de l'utiliser avant `PLAYING`, l'attente du retour lobby avant suppression du monde, les couleurs de laine dont AQUA, l'évolution des ressources v2 et la parité FR/EN. La matrice Paper doit encore confirmer PVP, événements de placement/explosion, mort immédiate, titres 5–1, retour lobby et nouveau clone après victoire.

Validation complète : 196 tests automatisés réussis, aucun échec ni erreur; build propre, Spotless et Shadow JAR validés.

## Correctif Tickets 013–014

La suite couvre désormais les bornes du rythme adaptatif, sa désactivation, le recalage de la première échéance à `PLAYING` et l'évolution d'anciens `shops.yml`/`generators.yml` avec sauvegardes. La matrice Paper doit confirmer la fiche en trois colonnes, les PNJ réparés, les drops immobiles et les compteurs holographiques synchronisés.

La validation complète contient 193 tests automatisés réussis, sans échec ni erreur. `clean build test shadowJar spotlessCheck` et la production du JAR déployable passent.

## Ticket 014 — boutiques et achats

La suite complète contient 190 tests automatisés réussis, sans échec, erreur ni test ignoré. Les nouveaux scénarios vérifient la position de boutique via `ArenaService`, sa suppression et son round-trip YAML, un achat réussi avec événement interne, et les refus sans échange lorsque le solde ou la place manquent. Le test de configuration recharge aussi `shops.yml`, `items.yml` et la parité FR/EN.

La matrice Paper doit encore confirmer : apparition et orientation des PNJ dans le clone, invulnérabilité, ouverture uniquement en `PLAYING`, refus spectateur, affichage des quatre catégories, achat avec piles réparties, inventaire plein, solde insuffisant, fermeture de partie et disparition du monde/PNJ.

## Ticket 013 — phase 1

Neuf scénarios purs couvrent identifiants sûrs, règles bornées, upgrade immutable, activation limitée à `PLAYING`, absence de rattrapage en rafale, capacité locale lue seulement à échéance, budget global équitable et refus des enregistrements dupliqués ou tardifs. Aucun test ne prétend créer un item Bukkit : ce raccordement appartient à la phase suivante.

La suite complète contient 183 tests automatisés réussis, sans échec, erreur ni test ignoré.

## Ticket 012 — phase automatisée

La suite complète contient 174 tests automatisés réussis, sans échec, erreur ni test ignoré. Les nouveaux scénarios couvrent persistance pied/tête/direction, adjacency, index des deux blocs, protection du lit allié, double destruction, crédit unique, respawn centralisé, protection temporaire, mort finale, final kill, élimination, victoire et `ENDING`. La matrice Paper reste obligatoire pour les vrais événements de lit, mort immédiate, inventaires spectateur, titres, scoreboard et nettoyage multijoueur.

## Correctif Ticket 010.1

La suite contient 161 tests automatisés réussis. Les nouveaux tests purs couvrent visibilité de la surface publique, résolution d'actions runtime, clé inconnue, ancien UUID, état interdit, off-hand, cooldown/oubli, rendu de placeholders, lignes vides et identiques, placeholder absent et limite de 15 lignes. Aucun serveur Minecraft n'est disponible : clics Bukkit, PDC réel, menu, restauration, affichage Paper et masquage des nombres restent dans la matrice manuelle.

## Ticket 010

La suite ajoute des tests purs du lobby et du compteur : entree du premier et du second joueur, demarrage automatique, annulation quand le minimum redescend, reprise du compteur, passage en `PLAYING`, lancement force, destruction d'une instance vide par le ticker central, id court non ambigu et validation de `game.yml`. Elle contient 154 tests automatises reussis, sans echec ni test ignore lors de la derniere verification.

La matrice manuelle Paper doit verifier : creation de clone, entree de deux joueurs, inventaire et scoreboard d'attente, bloqueurs de lobby, rescue du vide, item et commande de sortie, restauration complete, compteur, annulation, bossbar, start force, nettoyage vide, stop et nettoyage apres redemarrage complet.

## Ticket 009

Les tests purs couvrent la séquence complète des états, le refus d'une transition illégale, la réservation unique d'arène, le rollback d'un clone échoué, les index joueur, l'affectation d'équipe, les événements et la destruction libérant monde/mémoire. La suite complète contient 149 tests, 0 échec, 0 erreur et 0 test ignoré. Les opérations Bukkit réelles restent dans la matrice manuelle Paper : création, arrivée dans le clone, présence simultanée de plusieurs joueurs, refus d'une seconde instance, sortie, destruction et contrôle des deux dossiers.

## Ticket 007

Les tests purs couvrent l'identifiant sécurisé, l'immutabilité/révision/état, l'ordre et les snapshots du registre, les verrous, la publication après persistance, les pannes de création et nettoyage, sauvegarde/déchargement, présence de joueurs, duplication indépendante, suppression avec sauvegarde, relations d'arènes en direct et conflits de révision. La validation d'arène couvre carte absente, inconnue, mauvais type, erreur et association valide.

Les tests plugin couvrent le round-trip YAML UTF-8, l'isolation des métadonnées cassées, les exclusions de copie, dossiers temporaires, sauvegardes et refus de chemins externes. La configuration vérifie `worlds.yml`, les chemins dangereux, la parité FR/EN, la fusion non destructive des clés d'interface v2 et la complétion permission-aware. La suite complète contient 137 tests, 0 échec et 0 ignoré.

Aucun serveur Minecraft n'est disponible dans l'environnement Codex. Il faut tester manuellement sur Paper : monde réellement vide, plateforme, règles de jeu, persistance des blocs et du spawn, présence de joueurs, évacuation forcée, téléportation, copie volumineuse, sauvegarde de suppression, redémarrage, associations et deux opérations concurrentes.

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
