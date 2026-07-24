# Système d'instances

**Statut :** invariants planifiés, aucune instance implémentée.

## Objectif et périmètre

Isoler plusieurs parties simultanées dans un seul serveur Paper.

## Informations connues

Une map persistante sert de modèle à des instances logiques identifiées. Chaque instance possédera
un monde dédié, une machine d'état et ses joueurs. Un joueur et un monde ne pourront appartenir
qu'à une instance. L'entrée capturera l'état joueur ; la sortie le restaurera avant déchargement.
Clonage, sauvegarde et suppression seront hors thread ; création d'entités et mondes suivra les
contraintes Paper. `-1` n'impose aucun plafond fonctionnel.

## À compléter

États, files d'attente, stratégie de mondes, verrous, récupération après erreur, nettoyage et
observabilité.
