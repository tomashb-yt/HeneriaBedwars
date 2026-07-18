# Roadmap

## Ticket 017 en validation

Les profils joueurs et les statistiques de match sont maintenant durables dans SQLite. L'enregistrement est asynchrone, transactionnel et protégé contre le double comptage; `/bw stats` expose une première vue personnelle. La prochaine étape logique est le Ticket 018 : classements, consultation d'autres profils et fondations de progression, après validation Paper de la persistance.

## Ticket 016 en validation

Le profil de combat inspiré de la 1.8, les règles de dégâts BedWars, le knockback configurable et le kill-credit sont raccordés au runtime sans réimplémenter les loadouts. Le prochain ticket devra être défini après validation Paper multijoueur de cette boucle jouable.

## Correctif gameplay en validation

Le socle des Tickets 012 à 015 est maintenant jouable : combat activé, construction limitée, respawn lisible, générateurs, boutiques, équipement et améliorations. Ces comportements doivent être confirmés à deux joueurs sur Paper avant de déclarer leurs validations closes; le prochain développement prévu est le Ticket 016.

## Correctif en validation — menus, PNJ et générateurs

La fiche d'équipe est désormais organisée en trois colonnes stables (spawn, lit et boutique), avec uniquement les actions réellement disponibles. Les PNJ de boutique sont réparés au démarrage du gameplay et les anciennes configurations reçoivent le catalogue par défaut sans écrasement. Les générateurs centrent et immobilisent leurs propres drops, adaptent modérément leur rythme à la population et affichent le prochain diamant ou la prochaine émeraude par hologramme. Une validation Paper multijoueur reste requise avant de clore les Tickets 013 et 014.

## Correctif livré — assistant compact et import de monde

La configuration essentielle d'une arène est regroupée dans une vue de cinq lignes avec accès direct aux équipes. Les cartes externes peuvent être déposées dans le dossier d'import propre à chaque modèle puis remplacées depuis le menu avec sauvegarde. Le prochain ticket gameplay reste inchangé; cette livraison améliore uniquement l'administration et le stockage des modèles.

- [TERMINÉ] Ticket 001 — Initialisation et fondation
- [TERMINÉ] Ticket 002 — Configuration, messages et traductions
- [TERMINÉ] Ticket 003 — Framework interne de menus
- [TERMINÉ] Ticket 004 — Système d'items configurables
- [TERMINÉ] Ticket 005 — Modèle administratif, validation et stockage des arènes
- [TERMINÉ] Ticket 006 — Éditeur complet des arènes via menus
- [TERMINÉ] Ticket 007 — Gestionnaire autonome de mondes et cartes modèles
- [TERMINÉ] Ticket 008 — Finalisation de l'éditeur graphique des cartes
- [TERMINÉ] Ticket 009 — Runtime des parties (Game Instance Engine)

- [TERMINE] Ticket 010 - Lobby de partie, file d'attente et compte a rebours
- [TERMINE] Correctif 010.1 - accès administrateur, items du lobby et scoreboard
- [TERMINÉ] Ticket 011 — Équipes BedWars et navigateur public
- [TERMINÉ] Correctif de configuration — fiches d'équipes, spawns, sélection des lits et commandes de secours
- [EN COURS] Ticket 012 — Destruction des lits, morts, réapparitions, éliminations et victoire
- [EN COURS — VALIDATION PAPER] Ticket 013 — Générateurs de ressources (assistant, persistance et drops runtime)
- [EN COURS — VALIDATION PAPER] Ticket 014 — Boutiques, PNJ et achats atomiques
- [EN COURS — VALIDATION PAPER] Ticket 015 — Équipement, outils et améliorations
- [EN COURS — VALIDATION PAPER] Ticket 016 — Combat 1.8 et dégâts BedWars
- [EN COURS — VALIDATION PAPER] Ticket 017 — Profils joueurs, statistiques et stockage SQLite

## Grandes phases futures

La logique automatisée des Tickets 012 à 017 est implémentée et doit encore être confirmée sur Paper multijoueur. La priorité suivante est la stabilisation en jeu, puis les classements et la progression avant le stockage réseau.

Fondation, configurations, interfaces, items, arènes, cartes, instances, lobby, équipes, lits, générateurs, boutiques, améliorations, combat et statistiques SQLite sont implémentés. Les classements, la progression, MySQL/MariaDB, le réseau et l'API de mutation restent futurs.
