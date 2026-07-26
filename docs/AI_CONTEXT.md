# Instructions obligatoires pour toute IA

Avant de modifier le projet :

1. lire ce fichier intégralement ;
2. lire `ARCHITECTURE.md`, `DECISIONS.md` et `ROADMAP.md` ;
3. lire les documents du système modifié et les dernières entrées du changelog ;
4. inspecter Git et le code réel ;
5. synchroniser documentation, tests et code avant de terminer.

Le dépôt est la source de vérité. Une conversation externe ne l'est jamais.

## Vision

HeneriaZombie est un mini-jeu Zombies haut de gamme exécuté sur un seul serveur Paper. Un lobby
central et plusieurs parties totalement isolées coexistent dans le même processus. Le projet
reprend les principes familiers du genre sans reproduire de contenu protégé et garde ses
mécaniques originales optionnelles par map.

## Conventions techniques

- Java 21, Gradle Kotlin DSL et Paper 1.21.x ;
- Adventure pour les textes et audiences ;
- YAML versionné pour la configuration lisible ;
- `zombie-api` sans plateforme, `zombie-core` sans Paper et `zombie-plugin` comme frontière Paper ;
- injection explicite par constructeur, sans singleton global mutable ;
- fichiers hors thread serveur, appels Bukkit sur le thread serveur ;
- snapshots de configuration validés puis activés atomiquement.

## État réel — Ticket 004

Le socle Ticket 001 reste opérationnel. Le Ticket 002 ajoute :

- un lobby central chargé au démarrage et un état joueur de lobby standardisé ;
- des modèles de monde minimaux identifiés par `zombie-map.yml` ;
- des instances simultanées à identifiant UUID, capacité par map et cycle contrôlé ;
- copie asynchrone, chargement Paper, déchargement puis suppression sûre des mondes ;
- sessions joueur exclusives, capture/restauration d'état et reconnexion avec délai ;
- isolation de visibilité, tablist, chat, messages de mort et audiences Adventure ;
- scoreboards indépendants par contexte ;
- protections configurables des mondes d'instance ;
- commandes temporaires de création, inspection, entrée, sortie et arrêt ;
- détection automatique des dossiers de monde et aperçus administratifs isolés sans partie ;
- compteurs API réels pour les modèles et instances.

Le Ticket 004 ajoute un schéma éditorial v2, un registre asynchrone, des sessions d'administration,
les commandes `/zmap`, l'outil protégé, les GUI de placement et un validateur structurel. Les
définitions sont auto-sauvegardées sous `plugins/HeneriaZombie/maps`.

Le correctif 0.5.3 transforme une définition valide en instance privée de test lorsque le
catalogue contient un monde source homonyme. Il ne constitue pas encore une boucle de gameplay.

Les états sont `CREATING`, `WAITING`, `STARTING`, `RUNNING`, `ENDING`, `CLEANING`, `CLOSED` et
`ERROR`. Aucune manche, aucun zombie et aucun matchmaking ne sont implémentés.

## Modèle de map minimal du Ticket 002

Le catalogue détecte tout dossier
`<world-container>/zombie_templates/<mapId>/` contenant `level.dat`. Le spawn vanilla est lu hors
thread serveur. `zombie-map.yml` devient une surcharge facultative de capacité et de spawn. Une
visite administrative charge toujours une copie temporaire : le modèle source n'est jamais chargé
ni modifié directement.

## Stratégie joueur

À la première prise en charge, le plugin capture l'état pré-plugin en mémoire. Le lobby reçoit un
profil vide et standardisé. L'entrée en instance capture ce profil lobby, applique un profil de
partie propre et téléporte au spawn du modèle. La sortie restaure le profil lobby.

Lors d'un arrêt normal, les joueurs sont renvoyés au lobby avant le déchargement des mondes. Les
snapshots ne sont pas persistés après un crash brutal du processus ; le serveur conserve toutefois
son propre `playerdata`. Une persistance transactionnelle dédiée sera nécessaire avant
l'introduction d'inventaires de valeur.

## Limites connues

- le catalogue de clonage utilise encore son manifeste technique minimal distinct de l'éditeur ;
- l'ajout simple accepte un dossier de monde, mais pas encore une archive ZIP ;
- les instances restent en attente jusqu'à leur arrêt administratif : aucune boucle de jeu ;
- SQLite est configuré mais aucune donnée métier ne justifie encore l'ouverture d'une connexion ;
- le test automatisé simule plusieurs sessions, mais une validation visuelle complète exige trois
  clients Minecraft ;
- un arrêt serveur conserve les dossiers d'instance, car leur suppression serait incertaine ;
- `-1` retire le plafond fonctionnel, sans supprimer les limites matérielles.

## Interfaces livrées aux Tickets 003–004

Le moteur GUI configurable fournit des sessions isolées, navigation, pagination sans plafond,
recherche privée par chat, permissions, confirmations, rafraîchissement partagé et activation
atomique de `guis.yml`. Les menus disponibles sont joueur, administration, maps, instances,
diagnostics et confirmation. Ils réutilisent les services du Ticket 002.

L'éditeur configure les informations, zones, portes, spawns et objets sans YAML manuel. Groupe,
profil, statistiques et export restent désactivés. Le rendu doit encore être validé manuellement.

## Reprise

Le prochain incrément doit suivre `ROADMAP.md`, utiliser les services existants au lieu de créer un
second registre de sessions ou d'instances, et préserver la frontière Paper. Ne jamais annoncer
les fonctionnalités de gameplay planifiées comme déjà livrées.
