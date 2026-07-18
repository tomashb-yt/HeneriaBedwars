# Équipement et améliorations — Ticket 015

## Parcours administrateur

Dans `/bedwars`, ouvrir une arène puis une équipe. Les quatre colonnes représentent spawn, lit, boutique d'objets et boutique d'améliorations. Pour chaque PNJ : se placer, définir, visiter, retirer. Position et orientation sont sauvegardées dans l'équipe.

## Parcours joueur

Au début, le joueur reçoit une épée en bois et une armure de cuir colorée. Les armures mailles, fer et diamant sont permanentes pendant cette partie. Les cisailles sont permanentes. Pioches et haches ont quatre niveaux; chaque achat exige le niveau précédent et une mort retire un niveau.

Le PNJ d'améliorations vend Tranchant I, Protection I à IV et Hâte I à II. Chaque achat bénéficie immédiatement à toute l'équipe.

Le loadout est reconstruit au démarrage, après achat et après respawn. Une nouvelle `GameInstance` repart toujours à zéro.

## Invariants

- aucun niveau n'est écrit dans `arenas/`;
- aucun paiement n'est effectué par un listener ou un menu;
- un outil ne saute jamais un palier;
- le niveau maximum ne consomme aucune monnaie;
- le respawn lit uniquement l'état de `RuntimePlayer` et `RuntimeTeam`.
