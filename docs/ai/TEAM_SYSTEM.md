# Système d'équipes

Chaque `ArenaTeamDefinition` contient identité, couleur, capacité, spawn et lit administratif. Le runtime crée un `RuntimeTeam`, conserve ses membres, son spawn, l'état irréversible du lit et son élimination.

Une équipe est participante lorsqu'au moins un joueur lui a été assigné. Elle est éliminée seulement après destruction du lit et lorsque tous ses membres sont en mort finale; un joueur ayant un respawn déjà accordé la maintient active. Les équipes configurées mais vides ne bloquent pas la victoire.

La sélection reste possible uniquement en `WAITING`. Après le lancement, l'affectation est verrouillée et toutes les décisions passent par l'identifiant runtime.
