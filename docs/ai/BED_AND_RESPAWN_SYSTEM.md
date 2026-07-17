# Lits, morts et réapparitions

## Modèle administratif

`ArenaBedDefinition` contient un bloc pied, un bloc tête et une direction. `ArenaTeamDefinition` conserve le pied historique pour compatibilité et sérialise la tête/direction dans ses métadonnées stables. Une définition historique sans ces données doit être resélectionnée.

## Runtime

Au chargement du clone, Bukkit vérifie les deux `Bed.Part`, leur direction et remappe leurs coordonnées vers `hbw_game_*`. `GameInstance` indexe chaque bloc vers un unique `RuntimeBed`. L'état `alive` ne peut passer que de vrai à faux et garde destructeur/date.

## Destruction

Un `BlockBreakEvent` n'est traité que pour un membre `PLAYING` dans le monde de son instance. Le lit allié est refusé. Une destruction ennemie réussie met d'abord à jour le cœur, crédite le joueur, puis retire les deux blocs physiques. Les tentatives concurrentes suivantes reçoivent `ALREADY_DESTROYED`.

## Mort et respawn

`GameDeathService` choisit `RESPAWN`, `FINAL_DEATH` ou `IGNORE`. La politique `AT_DEATH` accorde le respawn si le lit était vivant à la mort. `GameRespawnService` balaie les échéances depuis le ticker central, téléporte au spawn d'équipe et active `protectedUntil`.

Sans lit, le joueur devient spectateur définitif. Quand aucun membre de l'équipe ne peut revenir, l'équipe est éliminée. Une seule équipe participante restante produit `GameVictoryEvent`, `ENDING`, annonce, délai, restauration des snapshots et suppression du clone.

## Limites

Le téléporteur spectateur est volontairement simple. Reconnexion, équipement de base final, récompenses, générateurs, boutiques et combat 1.8 complet sont hors Ticket 012. Toute validation finale nécessite deux joueurs sur Paper.
