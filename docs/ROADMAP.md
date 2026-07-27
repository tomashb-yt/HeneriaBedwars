# Roadmap

**Statut :** socle, gameplay jusqu'au Ticket 008 et publication GUI opérationnels.

## Étapes

1. **Ticket 001 — Fondation et documentation centrale :** terminé.
2. **Ticket 002 — Lobby et instances isolées :** lobby, clones, sessions, isolation et commandes
   terminés, avec import simple et aperçu administratif des mondes ; aucune boucle de jeu.
3. **Ticket 003 — Framework GUI configurable :** moteur, menus joueur/admin, maps, instances,
   diagnostics, recherche et confirmations terminés.
4. **Ticket 004 — Schéma et éditeur universel :** registre, sessions, outil, GUI, sauvegarde,
   historique et validation terminés.
5. **Activation des maps :** terminé en 0.10.0 avec snapshots publiés et restauration.
6. **Lobby enrichi et matchmaking :** sélection publiée et création automatique terminées ;
   files configurables, groupes et statistiques restent à livrer.
7. **Boucle classique minimale :** terminée au Ticket 005 : manches, zombies temporaires, points,
   mort, réanimation et fin contrôlée.
8. **Moteur d'ennemis :** terminé au Ticket 006 : définitions, IA de mêlée, dégâts, capacités,
   isolation, diagnostic et nettoyage.
9. **Moteur d'armes :** terminé au Ticket 007 : munitions, dégâts balistiques, armes murales,
   Mystery Box, Pack-a-Punch, GUI, événements et définitions data-driven.
10. **Économie :** points avancés, portes, achats partagés et règles transactionnelles.
11. **Contenu avancé :** bonus, spéciaux, boss, pièges et quêtes.
12. **Fonctions originales :** modules indépendants après stabilisation du socle classique.
13. **Persistance et exploitation :** profils nécessaires, migrations, métriques et outils admin.

Chaque étape devient un ticket borné, compilé, testé, documenté et publié. SQLite ne sera activé
qu'avec un modèle persistant défini.

## Ticket 008 — terminé

- portefeuilles isolés, transactions, journal, limites et idempotence ;
- récompenses d'impact, mort, assistance et réanimation ;
- achats atomiques muraux, Mystery Box et Pack-a-Punch ;
- Double Points, Max Ammo, Insta-Kill, Nuke, drops et nettoyage ;
- commandes, affichage, agrégats de résultat et tests automatisés.

## Version 0.10.0 — menus et publication

- espaces joueur et administrateur séparés sous `/zombies` ;
- catalogue joueur limité aux versions publiées ;
- entrée automatique dans une partie publique disponible ou nouvellement créée ;
- création, validation, test, publication et dépublication depuis les menus ;
- historique durable, restauration non destructive et verrou exclusif d'édition.

## Version 0.10.1 — stabilisation administrative

- migration compatible des anciens menus et correction de la collision du slot 24 ;
- suppression complète d'une map inutilisée avec configuration, contenu éditorial, historique,
  snapshots et mondes possédés ;
- protection absolue des mondes serveur externes.

Le correctif `0.10.2` restaure l'accès au gestionnaire complet depuis les anciens menus
administratifs.

La version `0.10.3` livre le tableau de bord réorganisé et la visite isolée d'un template depuis
ce gestionnaire.

## Prochain ticket

Perks, machines et réglages de test avancés fondés sur leurs moteurs réels, puis groupes,
statistiques et files configurables.
