# Modèles de données

**Statut :** modèles runtime, GUI, éditeur universel et boucle de jeu opérationnels.

## Instance

`GameInstance` est un agrégat mutable synchronisé. `GameInstanceSnapshot` est sa vue immuable :

- UUID technique ;
- identifiant stable de map ;
- nom de monde facultatif pendant la création ;
- état du cycle ;
- ensemble défensivement copié des joueurs ;
- capacité ;
- date de création ;
- propriétaire facultatif ;
- accès `PUBLIC` ou `PRIVATE` ;
- dernier diagnostic facultatif.

`GameInstanceRegistry` possède les agrégats actifs. Un monde est représenté dans le core par
`WorldInstanceHandle`, sans type Bukkit.

## Session joueur

`PlayerSession` contient l'UUID joueur, le contexte `LOBBY` ou `INSTANCE`, un UUID d'instance
facultatif, le statut en ligne et une échéance de reconnexion facultative. Ses invariants
interdisent un contexte instance sans identifiant et un contexte lobby avec identifiant.

`PlayerStateSnapshot`, limité au module Paper, copie défensivement inventaire, armure, main
secondaire, expérience, santé, alimentation, effets, mode, position, vitesses et vol.

## Définitions de map

`MapDefinition` est le schéma éditorial immuable en version 2. Il contient identité, auteur, dates,
monde, présentation, capacité, difficulté, mode, spawn joueur et quatre collections extensibles :
zones, portes, spawns zombies et objets typés. Les coordonnées utilisent `MapPoint`, sans Bukkit.

Une zone possède couleur, musique, ambiance, volume et ancre. Une porte possède prix, deux zones,
type, prérequis, animation et son. Un spawn possède poids, capacité, bornes de manche et distance,
visibilité, types autorisés et cooldown. Les objets utilisent un type stable et des propriétés
extensibles. `MapEditorSession` conserve outil, sélection, presse-papiers et historique borné.

## Modèle de monde technique

`MapTemplateDefinition` contient :

- `mapId`, limité à 64 caractères sûrs ;
- `maximumPlayers`, strictement positif ;
- le spawn `x`, `y`, `z`, `yaw`, `pitch`.

Sans fichier source, la capacité vient de la configuration globale et le spawn est lu dans le
`level.dat` vanilla. Un `zombie-map.yml` portant `schema-version: 1` peut encore les surcharger. Ce
modèle technique sert uniquement au clonage du Ticket 002. Le branchement d'une définition
éditoriale validée sur le catalogue de clones sera réalisé avec la boucle de jeu.

`MapPreviewService` conserve seulement l'UUID administrateur, le `mapId` et le handle de la copie
temporaire. Un aperçu n'est ni une instance ni une session de gameplay.

## Configuration

`ZombieSettings` et ses records imbriqués forment un snapshot immuable. La configuration globale
reste en version 1, les anciennes installations obtenant les nouvelles valeurs par défaut sans
écrasement. Les messages livrés complètent en mémoire les clés absentes du fichier utilisateur.

## GUI

`GuiSession` contient UUID joueur, jeton de vue, menu/contexte, accueil, historique borné, page,
recherche, filtres, données temporaires, confirmation, saisie et actualisation. Sa fermeture
libère ces références. Les snapshots de thèmes et menus sont immuables. `GuiPagination.Page`
expose tranche, index, nombre de pages et total sans plafond global.

## Persistance

## Partie et manche

`ZombieGame.Snapshot` contient état, manche, joueurs, raison de fin et durée.
`RoundState.Snapshot` expose total, attente, réservations, vivants, éliminés et phase.
`GamePlayer.Snapshot` conserve points, kills, réanimations, mises à terre, manche maximale et
échéance de reconnexion. `GameResult` est immuable et son port de persistance est asynchrone.

Les définitions éditoriales sont écrites en YAML versionné et atomique. Instances et sessions
restent runtime. SQLite reste réservé aux futures données de profil.
