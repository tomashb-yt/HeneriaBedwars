# Configuration

**Statut :** configuration globale v1 opérationnelle ; formats de map à venir.

## Objectif et périmètre

Décrire les fichiers lisibles, leurs invariants et le reload sûr du Ticket 001.

## Informations connues

`config.yml` contient `config-version: 1`, la langue, le debug, les mondes, la cible SQLite, la
capacité d'instances et les préférences GUI. `maximum-concurrent-games: -1` enlève le plafond
fonctionnel, pas les limites matérielles. Le chemin SQLite doit rester relatif au dossier du plugin.
Le seul type accepté à ce stade est `sqlite`.

`messages.yml` contient les modèles MiniMessage. Les valeurs par défaut sont copiées seulement si
le fichier manque. `/zombie reload` construit et valide un candidat, puis le remplace atomiquement ;
en cas d'erreur, l'ancien snapshot reste actif. Le reload ne touche ni monde, ni map, ni partie.

## Validation

Version, champs obligatoires, backend, chemin, capacité et thème sont contrôlés. Une erreur bloque
l'activation ; un avertissement est journalisé. Le monde lobby absent sera diagnostiqué par le
futur service de lobby, car le Ticket 001 ne charge aucun monde.

## À compléter

Migrations, configuration par map, schémas JSON, secrets MySQL éventuels et commandes de diagnostic
seront documentés lors de leur implémentation.
