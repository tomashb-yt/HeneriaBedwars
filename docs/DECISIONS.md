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

## ADR-017 — Un moteur GUI, des registres extensibles

Un unique `GuiService` gère cycle de vie et protections. Écrans et callbacks sont enregistrés par
injection dans deux registres locaux. Aucun module ne doit ajouter sa propre politique de clic ni
identifier une action grâce au nom d'un item.

## ADR-018 — Configuration GUI cachée et atomique

`guis.yml` est lu hors thread serveur, validé complètement et activé par échange atomique. Une
erreur conserve le snapshot précédent. Les rendus ne lisent jamais le disque.

## ADR-019 — Jeton de vue et rafraîchissement partagé

Chaque ouverture tourne un jeton empêchant un clic obsolète. Une seule tâche entretient toutes les
sessions et rafraîchit les inventaires en place selon leur intervalle.

## ADR-020 — Définition éditoriale immuable distincte du monde source

`MapDefinition` est un snapshot core versionné, sans Bukkit, distinct du manifeste technique de
clonage. L'éditeur remplace atomiquement le snapshot du registre ; une partie ne modifiera jamais
le modèle source.

## ADR-021 — Auto-save sérialisé avec backup

Chaque mutation déclenche une sauvegarde. Les écritures d'une même map sont ordonnées sur le pool
I/O, utilisent un fichier temporaire et conservent `map.yml.bak`.

## ADR-022 — Machines d'état instance et partie séparées

L'instance possède le cycle technique du monde ; `ZombieGame` possède le cycle métier. Elles
partagent un UUID, jamais un enum ni un état mutable.

## ADR-023 — Un ordonnanceur groupé

`PaperGameRuntime` exécute toutes les parties dans une tâche Paper unique. Aucun zombie, compte à
rebours, délai d'hémorragie ou réanimation ne crée sa propre tâche.

## ADR-024 — Apparition derrière un port

Le domaine dépend de `ZombieSpawner`, pas de Bukkit. Le Ticket 006 remplace définitivement
l'adaptateur temporaire par `PaperZombieEngine`.

## ADR-025 — Snapshot de définition par ennemi

Le registre YAML est remplacé atomiquement. Chaque apparition capture sa définition : un reload
n'altère jamais santé, IA, récompenses ou capacités d'un zombie déjà actif.

## ADR-026 — IA native sans NMS

La navigation terrestre délègue le chemin physique à Paper/Minecraft, tandis que ciblage,
cooldowns, dégâts, capacités, isolation et secours restent sous contrôle du moteur. Aucun NMS
n'est utilisé.

## ADR-027 — Mort idempotente et suivi indexé

`ZombieInstance.claimDeath` garantit une attribution unique. `ZombieTracker` maintient les index
interne, Bukkit et partie ; aucun scan global de mondes ne participe au tick.

## ADR-028 — Snapshot et tick groupé par arme

Chaque `WeaponInstance` capture sa définition au moment de la distribution. Un reload affecte
uniquement les futures armes. Cadence, rafales, charges et rechargements utilisent le tick groupé
de la partie, sans tâche par joueur ou par item.

## ADR-029 — Mort exclusivement détenue par le moteur d'ennemis

Le moteur d'armes calcule un dégât et le transmet à `PaperZombieEngine.damageFromWeapon`. Seuls
`ZombieDamageService` et la procédure idempotente de retrait attribuent mort et récompense. Une
arme ne supprime jamais directement une entité.

## ADR-008 — économie transactionnelle par partie

Décision : utiliser un agrégat en mémoire par partie, des montants `long` et une mutation unique
via `TransactionService`. Les achats appliquent une compensation plutôt qu'une transaction
distribuée : débit, attribution, puis remboursement lié au débit si nécessaire.

Raisons : accès constant, isolation stricte, aucun disque dans la boucle serveur, diagnostic
complet et nettoyage déterministe. Les clés idempotentes restent en mémoire jusqu'à la fin de la
partie, même si les détails anciens du journal sont purgés.

Conséquence : le financement individuel est sûr maintenant ; les économies d'équipe et
contributions nécessiteront leurs propres agrégats, sans réutiliser un portefeuille joueur.

## ADR-030 — Copie de travail et publication immuable

Une définition éditoriale reste modifiable dans `MapRegistry`. Une publication validée crée une
révision immuable distincte et durable. Seule la révision active au statut `PUBLISHED` alimente le
catalogue et les nouvelles parties publiques ; une partie capture sa définition au démarrage.

Une restauration ajoute une révision au lieu de réécrire l'historique. Cette séparation évite
qu'un autosave administratif modifie la production et permet de dépublier sans interrompre les
instances existantes.

## ADR-031 — Matchmaking simple sans file artificielle

Un clic joueur rejoint en priorité une instance publique disposant d'une place. En l'absence
d'instance, le système en crée une avec le pipeline isolé existant. Aucune file persistante ou
simulation de disponibilité n'est créée tant qu'une politique de file configurable n'est pas
définie.

## ADR-032 — Suppression possédée et archivage distinct

L'archivage est réversible et conserve tous les fichiers. La suppression permanente exige une
confirmation, l'absence d'instance et de verrou d'édition, puis retire définition, publication,
snapshots et modèle hors thread serveur.

Un monde n'est supprimable que si son chemin est exactement
`zombie_editing/hz_edit_<mapId>`. Une définition peut référencer un monde serveur existant lors de
sa création ; ce monde n'appartient pas au plugin et doit rester intact. Le registre en mémoire
n'oublie la map qu'après le succès de la suppression persistante.

## ADR-033 — Monde de travail possédé pour chaque map gérée

Toute nouvelle map éditoriale référence un monde possédé sous
`zombie_editing/hz_edit_<mapId>`. Un template importé est copié vers ce monde avant l'ouverture de
la session ; une création génère directement ce monde. Le monde courant de l'administrateur ne
peut plus devenir implicitement la source d'édition.

`ManagedMapWorldService` est l'unique propriétaire du chargement, de la sauvegarde, du
déchargement, de la duplication et de la synchronisation vers `zombie_templates`. Les accès
fichiers sont asynchrones et les appels Bukkit synchrones. Les anciennes définitions référençant
un monde externe restent lisibles pour compatibilité et ne deviennent jamais supprimables par le
plugin.
