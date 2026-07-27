# Systèmes de gameplay

**Statut :** boucle, ennemis et moteur d'armes classique opérationnels.

## Objectif et périmètre

Conserver la cible fonctionnelle sans introduire d'implémentation temporaire.

## Mode classique prévu

Manches, points, portes, barricades, armes murales, boîte mystère, courant, machines d'atouts,
Pack-a-Punch, bonus, réanimations, zombies spéciaux, boss, Easter Eggs, pièges et partie infinie.
Chaque système aura son service métier, sa configuration de map et ses tests d'invariants.

## Fonctions originales facultatives

Directeur adaptatif, reliques, mutations de manche, règles par zone, événements dynamiques,
contrats, quêtes modulaires, arbres d'amélioration des armes, extraction, plusieurs fins et
évolution dynamique de zones. Chacune sera désactivable indépendamment par map et ne sera jamais
requise pour exécuter le mode classique.

## À compléter

Compte à rebours, difficulté, budgets d'apparition, points, mise à terre, réanimation, spectateur,
transitions, défaite et retour lobby sont actifs. Le Ticket 006 ajoute les types pondérés par
manche, le ciblage isolé, l'attaque de mêlée, résistances, tirs à la tête explicites, mort
idempotente et secours anti-blocage.

Les armes de départ, armes murales, munitions, Mystery Box et Pack-a-Punch sont actifs depuis le
Ticket 007. Les coûts débitent les points de la partie et les dégâts passent par le moteur
d'ennemis. Portes, barricades, courant et atouts restent réservés aux tickets suivants.

## Économie commune

Les impacts, éliminations, assistances et réanimations alimentent désormais le portefeuille de la
partie. Les achats muraux, Mystery Box et Pack-a-Punch utilisent un pipeline commun avec
remboursement. Les futurs perks, portes, pièges et téléporteurs disposent déjà de types d'achat et
de raisons de transaction dédiés, sans gameplay factice.

Les bonus Double Points, Max Ammo, Insta-Kill et Nuke sont collectables sur les morts de zombies.
Leur logique est détaillée dans `POWER_UP_SYSTEM.md`.

La stabilisation 0.9.2 rend la mise à terre dépendante d'un sauveteur vivant, ajoute les animations
visuelles des deux machines, leurs hologrammes isolés, un retour court sur les dégâts reçus et un
ancrage sûr au sol pour chaque bonus.
