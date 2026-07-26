# Systèmes de gameplay

**Statut :** boucle classique minimale livrée au Ticket 005 ; contenu avancé planifié.

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

L'économie des portes, barricades, armes, boîte, courant, atouts et Pack-a-Punch n'est pas encore
active. Le moteur expose les contrats nécessaires sans simuler ces systèmes.
