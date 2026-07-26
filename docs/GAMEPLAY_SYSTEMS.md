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

Compte à rebours, difficulté par formule, budgets d'apparition, santé croissante, points, mise à
terre, réanimation, spectateur après hémorragie, transitions, défaite et retour lobby sont actifs.
Les zombies actuels sont des zombies Paper standards marqués par PDC et rattachés à leur instance.
Ils ne brûlent pas au soleil. Le spawn joueur éditorial est traduit vers le monde cloné lors du
lancement et des reconnexions.

Économie, formules de difficulté, catalogue d'armes, intelligence des zombies, objectifs,
équilibrage, états de victoire/défaite et budgets de performance.
