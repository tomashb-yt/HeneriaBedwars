# Décisions architecturales

**Statut :** ADR-001 à ADR-011 acceptées au Ticket 001.

Chaque ADR est immuable après acceptation ; une décision ultérieure la remplace explicitement.

## ADR-001 — Le plugin fonctionne sur un seul serveur Paper

**Contexte.** Le produit cible un lobby et plusieurs parties sur le même processus.
**Décision.** Aucune dépendance BungeeCord, Velocity ou second serveur.
**Raisons.** Déploiement simple et transitions locales déterministes.
**Conséquences.** L'isolation et la capacité doivent être gérées dans un seul serveur.
**Alternatives rejetées.** Un serveur par partie, disproportionné pour la cible.

## ADR-002 — Les parties sont séparées par des instances logiques et des mondes dédiés

**Contexte.** Plusieurs parties ne doivent partager ni blocs ni état.
**Décision.** Chaque partie future possédera une instance logique et un monde dédié.
**Raisons.** Nettoyage fiable, concurrence contrôlée et restauration possible.
**Conséquences.** Le clonage et la suppression devront être transactionnels.
**Alternatives rejetées.** Plusieurs parties dans un monde, trop fragile.

## ADR-003 — Le lobby et les parties ne partagent pas leur visibilité joueur

**Contexte.** Le lobby ne doit révéler ni perturber les joueurs en jeu.
**Décision.** Sessions, visibilité, inventaires et interfaces seront cloisonnés.
**Raisons.** Expérience cohérente et prévention des fuites d'état.
**Conséquences.** Toute transition devra capturer puis restaurer l'état joueur.
**Alternatives rejetées.** Un simple changement de scoreboard.

## ADR-004 — Aucun plafond fixe sur le nombre de spawns de zombies

**Contexte.** Les maps ont des géométries très différentes.
**Décision.** Les spawns seront une collection extensible à identifiants stables.
**Raisons.** Le modèle ne doit pas imposer une limite arbitraire.
**Conséquences.** La validation et les budgets runtime protégeront les performances.
**Alternatives rejetées.** Des champs numérotés en quantité fixe.

## ADR-005 — Les fonctions originales sont désactivables par map

**Contexte.** Certaines maps doivent rester classiques.
**Décision.** Chaque mécanique originale aura une activation indépendante dans la définition.
**Raisons.** Compatibilité des styles et déploiement progressif.
**Conséquences.** Une fonctionnalité ne pourra supposer qu'une autre est active.
**Alternatives rejetées.** Un unique mode « moderne » tout ou rien.

## ADR-006 — La documentation du dépôt est la source de vérité entre les IA

**Contexte.** Les conversations ne sont ni durables ni accessibles à tous.
**Décision.** Code, tests et `docs/` gouvernent toute reprise.
**Raisons.** Traçabilité et continuité.
**Conséquences.** Chaque ticket met à jour le contexte et le changelog.
**Alternatives rejetées.** Dépendre d'un historique de chat.

## ADR-007 — Les interfaces administratives sont accessibles principalement par GUI

**Contexte.** Un éditeur riche en commandes est difficile à apprendre.
**Décision.** Les parcours courants passeront par GUI ; les commandes resteront des secours précis.
**Raisons.** Découvrabilité et réduction des erreurs.
**Conséquences.** Les GUI devront être paginées, validées et accessibles.
**Alternatives rejetées.** Configuration YAML ou commandes uniquement.

## ADR-008 — Les configurations doivent être versionnées et validables

**Contexte.** Les formats évolueront et une erreur ne doit pas casser une partie.
**Décision.** Chaque racine persistante possède une version, des diagnostics et une activation
atomique.
**Raisons.** Migrations explicites et échec sûr.
**Conséquences.** Un format inconnu est refusé sans écrasement.
**Alternatives rejetées.** Désérialisation permissive silencieuse.

## ADR-009 — Trois modules imposent les frontières de dépendance

**Contexte.** La logique métier doit être testable sans serveur.
**Décision.** Séparer API, core et adaptateur Paper.
**Raisons.** Tests rapides et API publique maîtrisée.
**Conséquences.** Bukkit est interdit dans `zombie-api` et `zombie-core`.
**Alternatives rejetées.** Un module unique organisé seulement par packages.

## ADR-010 — L'injection est explicite et le registre n'est pas global

**Contexte.** Les singletons compliquent l'arrêt, les tests et les parties concurrentes.
**Décision.** `ZombieBootstrap` construit par constructeur et possède son registre.
**Raisons.** Propriété et durée de vie visibles.
**Conséquences.** Les nouveaux services doivent déclarer leurs dépendances.
**Alternatives rejetées.** Service locator statique et framework DI lourd.

## ADR-011 — SQLite est configuré mais différé jusqu'au premier besoin persistant

**Contexte.** Le Ticket 001 exige la cible SQLite mais aucune donnée métier à sauvegarder.
**Décision.** Valider son chemin sans ouvrir de connexion ni ajouter de pilote maintenant.
**Raisons.** Éviter une abstraction ou dépendance sans usage réel.
**Conséquences.** Le ticket de persistance devra définir migrations, exécuteur asynchrone et arrêt.
**Alternatives rejetées.** Une base vide ou un faux service annoncé comme fonctionnel.
