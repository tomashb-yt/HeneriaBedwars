# Contexte du projet

État Ticket 009 : le plugin sait transformer une arène valide et sa carte modèle en instance vivante isolée, avec clone temporaire, machine d'état, joueurs, équipes, événements et nettoyage. Il ne contient encore aucune mécanique BedWars : aucun lit actif, générateur, boutique, combat spécifique, victoire ou matchmaking.

HeneriaBedWars vise un plugin BedWars complet pour Paper 1.21.11 et Java 21, avec un combat inspiré du PvP 1.8. Il devra gérer plusieurs arènes et parties simultanées, un lobby principal et des lobbies d'attente, des équipes, lits, générateurs, boutiques et améliorations configurables, des statistiques, une API publique et de futurs addons.

Les stockages visés sont YAML pour l'amorçage, SQLite par défaut, MySQL/MariaDB en production et éventuellement Redis pour le réseau. PlaceholderAPI, Vault, PacketEvents, Citizens et Velocity sont des compatibilités futures, pas des dépendances actuelles.

Les opérations courantes devront être configurables en jeu afin que l'administrateur n'édite pas manuellement des fichiers. Les fichiers resteront utiles aux réglages avancés, sauvegardes, déploiements automatisés, diagnostics et récupérations. La modularité et la documentation doivent permettre une maintenance fiable par plusieurs développeurs et IA.

Depuis le Ticket 005, le projet gère des définitions administratives d'arènes persistantes et validables. Elles ne représentent ni une instance de partie, ni un monde cloné, ni un état de match. Cette séparation doit rester stricte dans les tickets gameplay.

Depuis le Ticket 006, toute la configuration générale de ces définitions est réalisable en jeu. L'éditeur réutilise exclusivement `ArenaService`, sauvegarde après chaque mutation et détecte les vues obsolètes par révision. La saisie chat est réservée aux sessions administratives et n'est pas diffusée.

Depuis le Ticket 007, les cartes modèles sont autonomes et persistantes. Le Ticket 009 conserve la séparation stricte `ArenaDefinition` / `MapTemplate` / `GameInstance` : le runtime référence des snapshots administratifs et travaille uniquement dans un clone jetable.
