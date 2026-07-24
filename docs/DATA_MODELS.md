# Modèles de données

**Statut :** règles de modélisation adoptées ; modèles gameplay à venir.

## Objectif et périmètre

Séparer les données administratives persistantes, les agrégats joueur et l'état runtime.

## Informations connues

Les identifiants techniques sont stables et distincts des noms affichés. Les records immuables
représentent les snapshots de configuration. Une future `MapDefinition` versionnée ne contiendra
jamais une instance vivante. Les états de manche, zombies, joueurs et monde seront possédés par une
instance et ne seront pas sérialisés dans les YAML administratifs.

SQLite accueillera les données relationnelles réellement persistantes avec migrations. Les accès
seront asynchrones et les résultats reviendront au thread serveur avant tout appel Paper.

## À compléter

Identifiants, schéma de map, session joueur, instance, zones, entités, progression et tables SQL.
