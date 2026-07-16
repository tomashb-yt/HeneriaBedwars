# Cycle des parties

Le cycle cible envisagé est :

```text
DISABLED -> WAITING -> STARTING -> PLAYING -> ENDING -> RESETTING
                                                    \-> ERROR
```

Ces états ne sont pas implémentés. Leur définition exacte, les transitions autorisées, l'idempotence et la récupération d'erreur devront être couvertes par des tests avant toute utilisation.

Les statuts administratifs d'une définition (`DRAFT`, `READY`, `ENABLED`, `DISABLED`, `INVALID`, `ERROR`) sont implémentés depuis le Ticket 005 mais ne font pas partie de ce cycle. `ENABLED` signifie seulement « autorisée pour un futur gestionnaire de parties ».
