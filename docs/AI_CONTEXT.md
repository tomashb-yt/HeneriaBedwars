# Instructions obligatoires pour toute IA

Avant de modifier le projet :

1. Lire ce fichier intégralement.
2. Lire `ARCHITECTURE.md`.
3. Lire `DECISIONS.md`.
4. Lire les documents liés au système modifié.
5. Vérifier le code existant avant de proposer une nouvelle architecture.
6. Mettre à jour la documentation après toute modification.
7. Ne jamais considérer une conversation externe comme source de vérité.
8. Considérer le dépôt et sa documentation comme la source de vérité.

## Vision

HeneriaZombie vise un mini-jeu Zombies haut de gamme sur un seul serveur Paper. Un lobby central
accueillera les joueurs, plusieurs maps pourront être enregistrées et plusieurs parties isolées
pourront vivre simultanément. L'inspiration classique est Black Ops 2, sans reproduire ses contenus
protégés. L'identité propre vient de systèmes optionnels configurables map par map.

## Objectifs fonctionnels

- séparer totalement les joueurs du lobby de ceux des parties ;
- cloner ou charger des mondes dédiés pour chaque instance logique ;
- proposer un configurateur universel en jeu, principalement par GUI ;
- accepter un nombre non plafonné arbitrairement de maps, instances ou spawns de zombies ;
- conserver des interfaces cohérentes, accessibles et configurables ;
- rendre chaque système de gameplay modulaire, testable et désactivable lorsque pertinent ;
- persister en SQLite les données structurées sans bloquer le thread serveur.

`-1` dans la capacité d'instances signifie « aucune limite fonctionnelle fixe ». Les ressources,
la sécurité et les protections opérationnelles du serveur restent les limites réelles.

## Lobby et instances prévus

Le lobby est un contexte sans gameplay Zombies. Rejoindre une partie créera une session joueur et
une instance indépendante liée à une définition de map validée. Visibilité, inventaire, monde,
scoreboard et état de jeu seront cloisonnés. Un arrêt devra restaurer les joueurs avant de libérer
le monde. Rien de ce paragraphe n'est encore implémenté.

## Configurateur universel prévu

L'éditeur devra pouvoir décrire n'importe quelle map sans hypothèse sur sa forme : zones, portes,
achats, machines, chemins, objectifs et une collection extensible de spawns identifiés. Il
enregistrera des identifiants stables et une version de schéma, puis affichera les erreurs avant
activation. Il n'existe pas encore dans le Ticket 001.

## Gameplay à préserver

Le socle futur couvrira manches, points, portes, barricades, armes murales, boîte mystère, courant,
atouts, Pack-a-Punch, bonus, réanimations, zombies spéciaux, boss, Easter Eggs, pièges et partie
infinie.

Les extensions originales envisagées sont : directeur adaptatif, reliques, mutations de manche,
règles de zone, événements dynamiques, contrats, quêtes modulaires, arbres d'amélioration d'armes,
extraction, fins multiples et évolution dynamique de zones. Elles devront être indépendamment
désactivables par map.

## Conventions techniques

Java 21, Gradle Kotlin DSL et Paper 1.21.x sont imposés. Adventure représente les textes. YAML
sert aux configurations lisibles ; JSON sera réservé aux structures exportables complexes ;
SQLite servira aux données persistantes. Les appels Paper restent dans `zombie-plugin`, la logique
testable dans `zombie-core` et les contrats publics stables dans `zombie-api`. Les services sont
assemblés explicitement, sans singleton global.

## Structure

- `zombie-api` : contrats publics indépendants ;
- `zombie-core` : application et domaine sans Paper ;
- `zombie-plugin` : adaptateurs Paper et JAR déployable ;
- `docs` : source de vérité inter-IA ;
- `zombie-plugin/src/main/resources` : manifeste et valeurs par défaut.

## État réel — Ticket 001 validé

Terminé dans le code : build multi-module, configuration immuable validée, installation des YAML,
rechargement transactionnel des options sûres, états de cycle de vie, registre local de services,
API publique de diagnostic, `/zombie`, `/zombie help`, `/zombie reload`, permissions et tests
unitaires. Les compteurs de maps et d'instances valent honnêtement zéro.

La chaîne `clean qualityGate` passe et un démarrage manuel Paper 1.21.11 a validé l'activation,
les trois commandes et l'arrêt propre.

Non implémenté : lobby, maps, éditeur, instances, mondes clonés, stockage SQLite actif, GUI et tout
gameplay. Aucun listener gameplay n'est enregistré.

## Fichiers importants

- `AGENTS.md` : procédure de travail ;
- `build.gradle.kts` et `settings.gradle.kts` : build ;
- `HeneriaZombiePlugin.java` : entrée Paper ;
- `ZombieBootstrap.java` : composition ;
- `ConfigurationManager.java` : chargement transactionnel ;
- `config.yml`, `messages.yml`, `plugin.yml` : ressources livrées ;
- `ROADMAP.md` et `DECISIONS.md` : ordre et contraintes.

## Prochaines étapes

Le prochain ticket doit préciser les modèles de map et leur registre avant d'ajouter l'éditeur ou
le gameplay. L'isolation des mondes et sessions viendra ensuite, puis le lobby, les GUI et les
systèmes de manches par incréments terminés et testés.

## Risques connus

La présence des mondes configurés n'est pas validée au Ticket 001. SQLite est configuré mais aucune
connexion n'est ouverte. Un nombre illimité fonctionnel d'instances n'est pas une garantie de
capacité matérielle. Les API Paper peuvent évoluer entre sous-versions 1.21. Chaque futur système
doit protéger le thread serveur, les restaurations joueur et la suppression de mondes.

## Règle de reprise

Comparer toujours documentation, tests et code. Ne jamais annoncer une fonction planifiée comme
livrée. Après chaque ticket, actualiser ce fichier, les documents concernés, le changelog et les
décisions nouvelles, puis exécuter la définition de terminé d'`AGENTS.md`.
