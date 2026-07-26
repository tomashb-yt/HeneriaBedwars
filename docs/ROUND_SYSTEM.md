# Système de manches

`RoundDifficultyCalculator` calcule sans Paper le nombre d'ennemis, leur santé, le plafond vivant
et le délai. `RoundState` possède les compteurs et empêche doubles réservations, doubles morts et
fins prématurées. Une manche suit `PREPARING → SPAWNING → ACTIVE → COMPLETED`.

Le runtime réserve un budget, demande l'apparition au port `ZombieSpawner`, puis confirme ou
libère chaque réservation. La manche finit uniquement lorsque tous les ennemis ont été créés,
qu'aucun n'est vivant et qu'aucune réservation n'est en vol.

La population part de `base + per-round × (round - 1)`, applique le multiplicateur de joueurs et
les bornes. La santé est exponentielle et plafonnable. Les valeurs sont figées à la création.

Ticket 005 crée seulement des zombies vanilla. Types avancés, navigation et conditions de zone
dynamiques appartiennent au prochain moteur.
