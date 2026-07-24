# Architecture

**Statut :** fondation adoptée au Ticket 001.

## Objectif et périmètre

Définir les frontières durables du plugin, les dépendances autorisées et les stratégies prévues.
Ce document ne spécifie pas encore les algorithmes de gameplay.

## Modules

```text
zombie-api  <-  zombie-core  <-  zombie-plugin  -> Paper
     ^                                  |
     +----------------------------------+
```

- `zombie-api` expose des contrats publics en lecture seule, sans Paper ni implémentation interne.
- `zombie-core` porte modèles, politiques et services applicatifs testables.
- `zombie-plugin` assemble les services et adapte commandes, événements, mondes, inventaires et
  ordonnanceur Paper.

Une dépendance remonte seulement vers la gauche. Les échanges inverses utilisent une interface
définie au niveau consommateur. Aucun module ne peut former de cycle.

## Services et cycle de vie

`ZombieBootstrap` est la racine de composition. Il construit les objets par injection de
constructeur et les conserve dans un `ServiceRegistry` possédé par l'instance du plugin. Les futurs
services avec ressources utiliseront `LifecycleComponent` : démarrage ordonné, rollback complet en
cas d'échec, arrêt inverse et tentative de fermeture de tous les composants.

## Événements internes

Les événements métier futurs seront des objets Java indépendants de Bukkit. Un port synchrone
explicite publiera les transitions déterministes ; l'adaptateur Paper ne traduira que les événements
destinés aux extensions. Les listeners ne contiendront pas de logique métier substantielle.

## Configuration

Les ressources par défaut ne remplacent jamais les fichiers existants. Le YAML est lu dans un
snapshot immuable, validé puis échangé atomiquement. Un reload invalide conserve le dernier
snapshot valide. Les définitions futures auront chacune une version et des identifiants stables.

## Isolation future des parties

Une définition de map persistante sera distincte d'une instance vivante. Chaque partie possédera
un identifiant, un monde dédié et ses états runtime. Un index empêchera un joueur ou un monde
d'appartenir à deux instances. Les opérations fichiers seront asynchrones ; les appels Bukkit
resteront sur le thread serveur.

## GUI future

Les GUI suivront le modèle vue/session/action. Les inventaires Paper afficheront un modèle
immuable ; une session courte reliera joueur et contexte ; les actions appelleront un service
applicatif. Les items seront identifiés par PDC, les textes viendront de la configuration et aucun
accès SQL ne sera effectué dans un clic.

## Éléments à compléter

Ports de stockage, bus interne, modèles de map, machine d'état d'instance, stratégie de clonage et
contrat GUI seront ajoutés avec leurs tickets et tests.
