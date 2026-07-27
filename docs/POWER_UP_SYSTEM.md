# Bonus et drops

## Architecture

`PowerUpRegistry` contient les définitions immuables. `PowerUpService` isole les effets temporaires
par partie et applique leur politique de cumul. `PowerUpDropService` est indépendant de Paper :
son générateur aléatoire est injecté, et il gère chance, poids, plafond par manche, cooldown,
durée de vie, collecte idempotente et expiration.

`PaperPowerUpService` matérialise un drop lumineux, interdit son ramassage vanilla et vérifie sa
proximité depuis le tick groupé existant. Il n'existe aucune tâche par drop ou par bonus.

Avant l'apparition, l'adaptateur cherche dans un rayon de deux blocs une surface solide disposant
de deux blocs d'air. Le drop est centré juste au-dessus, sa vélocité reste nulle et le tick groupé
le ramène à son ancre s'il a été déplacé. Le secours utilise la surface la plus haute de la colonne
d'origine.

## Bonus implémentés

- `DOUBLE_POINTS` double les récompenses compatibles et étend sa durée jusqu'au plafond ;
- `INSTA_KILL` fait passer les tirs par le service de dégâts avec une valeur létale ;
- `MAX_AMMO` recharge les réserves via `PaperWeaponService` ;
- `NUKE` applique des dégâts explosifs à chaque zombie via `PaperZombieEngine`.

Les activations diffusent actionbar et son aux joueurs de la partie. Les drops et effets sont
supprimés lors du nettoyage de l'instance. Une seconde instance possède des registres entièrement
distincts.

## Commandes de diagnostic

`/zeconomy givepowerup <type> [game]`, `/zeconomy clearpowerups <game>` et
`/zeconomy debug <game>` permettent de forcer puis inspecter les effets sans contourner leur
service.
