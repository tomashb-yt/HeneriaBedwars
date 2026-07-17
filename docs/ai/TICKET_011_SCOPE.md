# Ticket 011 - Equipes BedWars et navigation publique

Le Ticket 011 porte deux axes séparés dans le code, les tests et le rapport final : les équipes BedWars détaillées, puis le premier navigateur public de parties. Il ne doit exposer aucune action administrative.

## Entrée joueur

- `/bedwars` reste réservé au dashboard administratif;
- `/bw` et `/bw play` ouvrent l'accueil public;
- `/bedwars play` ouvre le même accueil;
- `/bw leave` et `/bedwars game leave` quittent la partie courante;
- permission publique `heneriabedwars.play`, accordée par défaut.

Le menu principal propose jouer maintenant, choisir une partie, informations et fermer. Un joueur déjà en partie voit les informations de sa partie et ne peut jamais rejoindre une seconde instance.

## Navigateur public

La liste est paginée, actualisable et filtrable par `TOUTES`, `DISPONIBLES`, `EN_ATTENTE`, `DEMARRAGE` et `PRESQUE_PLEINES`. Les tris sont meilleur choix, plus peuplées, moins peuplées et carte. Aucun enum technique (`ERROR`, `RESETTING`, `DESTROYED`) n'est affiché.

Chaque entrée indique carte, joueurs, minimum, capacité, état localisé, countdown et raison d'indisponibilité. Clic gauche rejoint; clic droit ouvre le détail pré-join, qui peut proposer rejoindre, retour et fermer. Ce détail est distinct du livre reçu après avoir rejoint, lequel reste uniquement informatif.

## Sélection et concurrence

`PublicGameSelector`, `GameSelectionPolicy` et `GameSelectionResult` classent les instances rejoignables sans dépendre du GUI. La priorité va aux parties `WAITING` déjà peuplées, non pleines, puis aux parties `STARTING` dont le countdown reste supérieur au minimum configuré. Si aucune partie n'existe, la stratégie minimale affiche un état vide; la création automatique reste un chantier matchmaking futur.

Le clic relit toujours l'instance et délègue à `GameLobbyService.join`. Le nombre affiché n'est jamais la source de vérité : deux joueurs sur la dernière place produisent un succès et un refus `GAME_FULL`, sans dépasser la capacité.

## Configuration et présentation

`game.yml` ajoutera `public-game-browser` avec activation, quick play, countdown minimum, préférence de population, taille, visibilité des parties pleines et auto-refresh central de 40 ticks. `items.yml` utilisera les familles `play.main.*`, `play.browser.*` et `play.info.*`. Les catalogues FR/EN fourniront toutes les clés `play.menu`, `play.browser`, `play.selection` et `play.info`.

## Validation attendue

Les tests couvriront `/bw`, `/bedwars play`, joueur non administrateur, joueur déjà en partie, liste vide, états disponible/plein/countdown, changement après ouverture, double clic, dernière place concurrente, quick play, filtres, tris, pagination, refresh, traductions, absence d'administration et délégation à `GameLobbyService`.
