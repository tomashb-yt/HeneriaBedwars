# Cycle des parties

Le cycle cible envisagé est :

```text
DISABLED -> WAITING -> STARTING -> PLAYING -> ENDING -> RESETTING
                                                    \-> ERROR
```

Ces états ne sont pas implémentés. Leur définition exacte, les transitions autorisées, l'idempotence et la récupération d'erreur devront être couvertes par des tests avant toute utilisation.
