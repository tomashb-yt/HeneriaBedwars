# API d'événements

**Statut :** principes seulement ; aucun événement public ou métier défini.

## Objectif et périmètre

Préparer des transitions observables sans coupler le domaine à Bukkit.

## Informations connues

Les événements internes futurs seront immuables et indépendants de Paper. Leur publication ne
remplacera pas les appels de service ni les invariants transactionnels. Seuls les événements utiles
aux extensions seront traduits en événements Bukkit publics, documentés et déclenchés sur le
thread approprié.

## À compléter

Contrat du bus, ordre, erreurs d'abonnés, événements annulables et catalogue versionné.
