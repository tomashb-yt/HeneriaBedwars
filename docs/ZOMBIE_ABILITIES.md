# Capacités des zombies

`ZombieAbilityService` est un registre d'implémentations identifiées par chaîne stable. Les
capacités sont invoquées par la boucle groupée et ne créent aucune tâche.

Capacités livrées :

- `poison_hit` : applique Poison I pendant trois secondes après une attaque réussie, avec cinq
  secondes de cooldown propre au zombie ;
- `explode_on_death` : à une mort récompensée, produit un effet visuel et inflige quatre points de
  dégâts aux joueurs vivants de la même instance dans un rayon de quatre blocs. Aucun bloc n'est
  détruit.

Une suppression administrative, une fin de partie ou une disparition invalide n'active pas
l'explosion.
