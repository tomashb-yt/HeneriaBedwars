# API d'extension

**Statut :** diagnostic public minimal disponible.

## Objectif et périmètre

Permettre de futures extensions sans exposer les implémentations internes.

## Informations connues

`ZombieApi` expose actuellement l'état du plugin et les compteurs de maps/instances. Il ne dépend
pas de Paper et est enregistré dans le `ServicesManager` Bukkit à l'exécution. Les collections
mutables, objets runtime et classes de `zombie-core` ne feront jamais partie du contrat public.
Toute évolution incompatible exigera une décision et une stratégie de version.

## À compléter

Registres contrôlés de mécaniques, événements publics, compatibilité binaire et exemple d'addon.
