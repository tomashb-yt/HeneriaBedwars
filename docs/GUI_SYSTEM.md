# Système GUI

**Statut :** architecture prévue, aucune GUI implémentée.

## Objectif et périmètre

Fournir des interfaces joueur et administration cohérentes, configurables et sans logique métier
dans les listeners d'inventaire.

## Informations connues

Le modèle prévu sépare vue immuable, session courte et action applicative. Les items d'action
porteront un identifiant PDC ; titres et textes seront des composants Adventure ; thèmes, sons et
animations seront configurables. Pagination, confirmation des actions destructives et nettoyage à
la déconnexion sont obligatoires. Aucun clic GUI ne fera d'accès SQL.

## À compléter

Contrats de vue, moteur de pagination, thèmes, accessibilité, éditeur et tests d'interaction.
