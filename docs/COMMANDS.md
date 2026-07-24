# Commandes et permissions

**Statut :** surface Ticket 001 opérationnelle.

## Objectif et périmètre

Documenter les commandes réellement disponibles et leurs contrôles d'accès.

## Commandes

- `/zombie` — version, état, maps enregistrées, instances actives et aide ;
- `/zombie help` — aide minimale ;
- `/zombie reload` — recharge uniquement la configuration globale sûre et les messages.

Le reload refuse un candidat invalide et conserve l'ancien. Au Ticket 001, les compteurs maps et
instances valent zéro.

## Permissions

`zombie.command.use` autorise la commande de base ; `zombie.command.reload` autorise le reload ;
`zombie.admin` regroupe les droits administratifs ; `zombie.editor` et `zombie.play` réservent les
futures fonctions. Les valeurs par défaut figurent dans `plugin.yml`.

## À compléter

Sous-commandes de maps, éditeur, parties et diagnostics avec permissions granulaires.
