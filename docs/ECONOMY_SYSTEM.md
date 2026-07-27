# Système économique

## Périmètre

Le Ticket 008 fournit une économie de session isolée par partie. `EconomyService` indexe en temps
constant un `GameEconomy` par UUID de partie et un `PlayerWallet` par joueur. Un portefeuille
contient un solde `long`, les totaux gagnés, dépensés et remboursés, le nombre de transactions et
la dernière transaction. Aucun setter public n'existe.

`TransactionService` est l'unique frontière de mutation. Les moteurs d'armes, de zombies, les
commandes et les achats ne modifient jamais le solde directement. Une économie est créée avant la
préparation de la partie, conservée pendant une reconnexion puis supprimée au nettoyage final.

## Récompenses

`RewardService` crédite les impacts, éliminations, assistances et réanimations. Les contributions
de dégâts sont indexées par partie et zombie, puis supprimées lors de sa mort. La récompense de
réanimation applique une fenêtre anti-farm par trio partie/réanimateur/cible.

Double Points est appliqué au calcul des récompenses de gameplay. Il ne touche pas les points de
départ, ajustements administratifs, dépenses ou remboursements.

## Affichage et résultats

Le scoreboard existant lit le solde central, le multiplicateur et le nombre de bonus actifs sans
être recréé. `PointDisplayService` groupe les transactions pendant la fenêtre configurée et les
affiche dans l'actionbar depuis le tick commun.

À la fin, `GameResult` reçoit un agrégat immuable par joueur : gains, dépenses, remboursements,
solde final, nombre de transactions et d'achats, plus grosse dépense et bonus collectés. Le dépôt
de résultats existant peut ainsi l'écrire de manière asynchrone.

## Limites explicites

Les points restent une monnaie de partie, pas une monnaie Vault ou globale. Le financement
`INDIVIDUAL` est actif ; les modes équipe, partage égal, contribution et personnalisé sont
représentés mais seront branchés lorsque leurs systèmes de gameplay existeront. Les détails du
journal restent en mémoire et sont bornés ; seuls les agrégats sont destinés à la persistance.
