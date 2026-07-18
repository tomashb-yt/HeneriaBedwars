# Système de statistiques

## Flux d'une fin de partie

`GameDeathService` détermine la dernière équipe vivante, passe l'instance en `ENDING` et publie `GameVictoryEvent`. `StatisticsLifecycleComponent` retrouve immédiatement l'instance encore vivante et demande à `StatisticsService` d'en produire un résultat immuable. Les joueurs partis après le début restent dans un registre de participants distinct des membres actifs afin que leur défaite et leurs compteurs ne disparaissent pas. Le dépôt reçoit ensuite ce résultat sur son exécuteur dédié; le nettoyage du clone peut continuer sans attendre le disque.

## Données enregistrées

`player_statistics` contient un agrégat par UUID joueur : parties, victoires, kills, morts, final kills, lits détruits, secondes jouées, série actuelle, meilleure série et dernière partie. Les défaites sont dérivées de `games_played - wins`; taux de victoire et K/D sont calculés à la lecture.

`processed_matches` conserve l'UUID de partie, l'arène, la carte, l'équipe gagnante et la date. Cet UUID est la barrière contre les doubles écritures.

## Atomicité et concurrence

SQLite fonctionne en mode WAL avec un délai d'attente configurable. Chaque résultat ouvre une transaction : l'insertion `INSERT OR IGNORE` du match doit réussir avant les UPSERT des joueurs. Si l'UUID existe déjà, la transaction est annulée et aucun compteur ne change. En cas d'erreur, tous les participants restent inchangés.

Un exécuteur daemon mono-thread sérialise initialisation, écritures et lectures. Les callbacks d'affichage reviennent explicitement au thread Bukkit. Aucun listener, menu ou ticker n'exécute JDBC directement.

## Commandes et limites

`/bw stats` et `/bedwars stats` affichent seulement le profil personnel et requièrent `heneriabedwars.statistics.view`. Les parties sans victoire ne sont pas comptées. Les classements, profils tiers, saisons, migrations MySQL/MariaDB et exposition dans l'API publique restent futurs.
