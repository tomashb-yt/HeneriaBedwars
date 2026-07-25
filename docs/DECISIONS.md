# Décisions architecturales

Chaque ADR acceptée est immuable ; une décision ultérieure la remplace explicitement.

## ADR-001 — Un seul serveur Paper

Le lobby et toutes les parties vivent dans un processus Paper, sans BungeeCord, Velocity ni
second serveur.

## ADR-002 — Un monde dédié par instance

Chaque partie possède un agrégat logique et une copie de monde propre. Plusieurs parties dans un
même monde sont rejetées à cause des risques de fuite et de nettoyage.

## ADR-003 — Isolation fondée sur la session

`PlayerSessionService`, et non le monde Bukkit courant, décide du contexte. Visibilité, tablist,
chat, audiences, inventaire et scoreboard suivent cette session.

## ADR-004 — Pas de plafond arbitraire

Les collections futures de spawns sont extensibles et `maximum-concurrent-games: -1` ne fixe aucun
plafond fonctionnel. Les ressources de la machine restent une limite opérationnelle.

## ADR-005 — Fonctions originales optionnelles par map

Chaque mécanique originale future doit pouvoir être désactivée indépendamment.

## ADR-006 — Documentation du dépôt comme source de vérité

Chaque ticket synchronise code, tests, contexte central, décisions et changelog.

## ADR-007 — Administration principalement par GUI

Les GUI seront le parcours principal ; les commandes du Ticket 002 sont des outils temporaires,
précis et permissionnés.

## ADR-008 — Configurations versionnées et atomiques

Un candidat inconnu ou invalide est refusé sans écraser le snapshot actif. Les valeurs de messages
livrées peuvent compléter les personnalisations en mémoire, jamais les écraser sur disque.

## ADR-009 — Trois modules imposent les frontières

API et core ne connaissent pas Paper. `zombie-plugin` est l'unique adaptateur de plateforme.

## ADR-010 — Injection explicite, aucun registre global

`ZombieBootstrap` possède les services et injecte leurs dépendances par constructeur.

## ADR-011 — SQLite différé jusqu'au premier besoin persistant

Le chemin est validé, mais aucune base vide ni faux service n'est créé.

## ADR-012 — Fichiers asynchrones, mondes synchrones

Les copies et suppressions utilisent un pool I/O borné. Chargement, déchargement, gamerules et
téléportations reviennent sur le thread Paper. Une instance reste `CREATING` jusque-là.

## ADR-013 — Suppression uniquement après déchargement confirmé

Un dossier runtime n'est supprimé qu'après confirmation de Paper. Un échec ou un arrêt serveur
préserve les fichiers et produit un diagnostic.

## ADR-014 — Profil lobby standardisé avec capture mémoire

Le plugin capture l'état pré-plugin, applique un lobby propre, capture ce lobby avant la partie et
le restaure à la sortie. Cette stratégie évite le partage d'inventaire sans introduire une
persistance prématurée ; une sauvegarde durable sera ajoutée avant les inventaires de valeur.

## ADR-015 — Catalogue de map minimal limité au Ticket 002

`zombie-map.yml` v1 fournit seulement capacité et spawn afin de tester les mondes isolés. Il ne
préjuge pas du futur schéma universel ni de l'éditeur.

## ADR-016 — Import par dossier et aperçu sur copie

Un dossier contenant `level.dat` est une map détectable sans manifeste manuel. Le spawn vanilla est
lu directement depuis le NBT hors thread serveur et la capacité utilise une valeur globale
configurable. Un aperçu ne charge jamais le modèle source : il utilise une copie temporaire hors du
registre des parties, supprimée après déchargement confirmé.
