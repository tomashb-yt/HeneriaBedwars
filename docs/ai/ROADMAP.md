# Roadmap

## Ticket 015 en validation

L'équipement durable, les outils évolutifs et les trois premières améliorations d'équipe sont raccordés aux boutiques et aux respawns. Le prochain ticket est le Ticket 016, consacré au combat 1.8 et aux dégâts BedWars; il ne doit pas réimplémenter les loadouts.

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
- [À FAIRE] Ticket 016 — Combat 1.8 et dégâts BedWars

## Grandes phases futures

La logique automatisée des Tickets 012 à 015 est implémentée et doit encore être confirmée sur Paper multijoueur. Le Ticket 016 ajoutera le ressenti de combat 1.8 et les règles de dégâts sans modifier la progression d'équipement.

Fondation, configurations, interfaces, items, définitions d'arènes, éditeur administratif, cartes modèles, instances temporaires et lobby de partie sont terminés. Les équipes détaillées, lits, générateurs, boutiques, améliorations, combat, statistiques, stockage SQL, réseau et API de mutation restent futurs.
