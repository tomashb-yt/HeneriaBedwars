# Cycle de vie d'une partie

```text
WAITING_FOR_PLAYERS → PREPARING → COUNTDOWN → STARTING
                                           → ROUND_ACTIVE
                                           ↔ ROUND_TRANSITION
                                           → ENDING → FINISHED → CLEANING
```

Le compte à rebours revient en attente si les joueurs sont insuffisants et redémarre lorsque le
minimum revient. Une déconnexion conserve l'état précédent jusqu'à l'échéance ; une reconnexion
le restaure, sinon le joueur est éliminé. L'hémorragie mène au spectateur, tandis qu'une
réanimation accroupie et maintenue rend le joueur vivant.

Toutes les fins passent par `ZombieGame.end`, produisent au plus un `GameResult`, retirent les
entités indexées, affichent le résultat, renvoient les joueurs et confient le monde à
`InstanceCoordinator`. `PaperZombieEngine.removeAll` annule les capacités, supprime les entités et
vide les trois index de suivi avant le déchargement du monde. L'arrêt est idempotent et ne laisse
aucune tâche individuelle.

## Cycle de l'économie

L'économie est créée avant `prepare`, les portefeuilles sont ouverts avec une transaction de
départ et survivent à la reconnexion. La fin enrichit le résultat avec les agrégats financiers
avant sa sauvegarde asynchrone. Le nettoyage final supprime achats idempotents, contributions,
transactions, bonus, drops et affichages de la seule partie concernée.
