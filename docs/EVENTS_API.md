# API d'événements

**Statut :** événements métier internes livrés ; API Bukkit publique différée.

## Objectif et périmètre

Préparer des transitions observables sans coupler le domaine à Bukkit.

## Informations connues

Les événements internes futurs seront immuables et indépendants de Paper. Leur publication ne
remplacera pas les appels de service ni les invariants transactionnels. Seuls les événements utiles
aux extensions seront traduits en événements Bukkit publics, documentés et déclenchés sur le
thread approprié.

`GameEvent` publie création, préparation, démarrage, début/fin de manche, enregistrement et
élimination de zombie, mise à terre, réanimation, élimination de joueur et fin. Son
`GameEventDispatcher` est synchrone, non annulable et indépendant de Paper. Les invariants sont
appliqués avant publication.

`ZombieEvent` ajoute le catalogue `PRE_SPAWN`, `SPAWNED`, `TARGET_SELECTED`, `TARGET_LOST`,
`PRE_ATTACK`, `ATTACKED`, `PRE_DAMAGE`, `DAMAGED`, `ABILITY_PRE_ACTIVATE`, `ABILITY_ACTIVATED`,
`PRE_DEATH`, `DEATH`, `REMOVED` et `STUCK`. Les événements `PRE_*` sont annulables ; les autres
refusent explicitement `cancel()`. `ZombieEventDispatcher` est synchrone, isole les erreurs
d'abonnés et retourne le résultat d'annulation. Il doit être appelé sur le thread propriétaire du
moteur. Aucune traduction Bukkit publique n'est livrée dans ce ticket afin d'éviter un volume
d'événements public prématuré.
