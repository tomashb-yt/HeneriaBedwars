# API d'événements

**Statut :** événements métier internes livrés ; API Bukkit publique différée.

## Objectif et périmètre

Préparer des transitions observables sans coupler le domaine à Bukkit.

## Informations connues

Les événements internes futurs seront immuables et indépendants de Paper. Leur publication ne
remplacera pas les appels de service ni les invariants transactionnels. Seuls les événements utiles
aux extensions seront traduits en événements Bukkit publics, documentés et déclenchés sur le
thread approprié.

## À compléter

`GameEvent` publie création, préparation, démarrage, début/fin de manche, enregistrement et
élimination de zombie, mise à terre, réanimation, élimination de joueur et fin. Son
`GameEventDispatcher` est synchrone, non annulable et indépendant de Paper. Les invariants sont
appliqués avant publication.

Contrat du bus, ordre, erreurs d'abonnés, événements annulables et catalogue versionné.
