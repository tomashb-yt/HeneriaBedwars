# Contexte du projet

État correctif runtime/équipes du 2026-07-17 : les cartes et titres d'équipe respectent leur couleur, la fiche est organisée en deux colonnes fixes spawn/lit et tous les retours sont localisés. Quand le countdown atteint `PLAYING`, chaque joueur est déplacé au spawn de son équipe dans le clone runtime. Ce déplacement lance la phase de partie mais n'active encore aucune mécanique de lit, mort, victoire, générateur ou boutique.

État correctif UX du 2026-07-17 : l'assistant d'arène principal tient sur cinq lignes et expose directement les équipes colorées. Les opérations essentielles sont carte, attente, spectateur, format, spawn/lit de chaque équipe, validation et activation. Les réglages avancés ne saturent plus cette vue.

Chaque `MapTemplate` possède un dépôt administrateur confiné `maps/templates/<id>/import/`. La présence de `level.dat` autorise un remplacement guidé : déchargement Bukkit sur le thread serveur, sauvegarde et copie hors thread, échange avec restauration de secours, puis rechargement sur le thread serveur. Aucun chemin fourni par l'utilisateur n'est accepté.

Etat correctif 010.1 : la surface `/bedwars` est administrative, tandis que `game join|leave` forme la petite surface publique temporaire. Les items du lobby sont authentifiés par PDC et délèguent au service métier; l'information joueur est séparée des menus administratifs. Le scoreboard d'attente est localisé, configurable et incrémental.

Le Ticket 011 ajoutera deux axes indépendants : équipes BedWars détaillées et navigateur public `/bw`. Le livre d'un joueur déjà présent reste informatif; seul le navigateur avant join pourra proposer l'action rejoindre.

Etat Ticket 010 : le plugin transforme une arene valide et sa carte modele en instance isolee, accueille plusieurs joueurs dans un lobby temporaire, protege cet espace, gere un compte a rebours et atteint `PLAYING` sans lancer de gameplay BedWars. Aucun lit actif, generateur, boutique, combat specifique, victoire ou matchmaking n'est fourni.

État Ticket 009 : le plugin sait transformer une arène valide et sa carte modèle en instance vivante isolée, avec clone temporaire, machine d'état, joueurs, équipes, événements et nettoyage. Il ne contient encore aucune mécanique BedWars : aucun lit actif, générateur, boutique, combat spécifique, victoire ou matchmaking.

HeneriaBedWars vise un plugin BedWars complet pour Paper 1.21.11 et Java 21, avec un combat inspiré du PvP 1.8. Il devra gérer plusieurs arènes et parties simultanées, un lobby principal et des lobbies d'attente, des équipes, lits, générateurs, boutiques et améliorations configurables, des statistiques, une API publique et de futurs addons.

Les stockages visés sont YAML pour l'amorçage, SQLite par défaut, MySQL/MariaDB en production et éventuellement Redis pour le réseau. PlaceholderAPI, Vault, PacketEvents, Citizens et Velocity sont des compatibilités futures, pas des dépendances actuelles.

Les opérations courantes devront être configurables en jeu afin que l'administrateur n'édite pas manuellement des fichiers. Les fichiers resteront utiles aux réglages avancés, sauvegardes, déploiements automatisés, diagnostics et récupérations. La modularité et la documentation doivent permettre une maintenance fiable par plusieurs développeurs et IA.

Depuis le Ticket 005, le projet gère des définitions administratives d'arènes persistantes et validables. Elles ne représentent ni une instance de partie, ni un monde cloné, ni un état de match. Cette séparation doit rester stricte dans les tickets gameplay.

Depuis le Ticket 006, toute la configuration générale de ces définitions est réalisable en jeu. L'éditeur réutilise exclusivement `ArenaService`, sauvegarde après chaque mutation et détecte les vues obsolètes par révision. La saisie chat est réservée aux sessions administratives et n'est pas diffusée.

Depuis le Ticket 007, les cartes modèles sont autonomes et persistantes. Le Ticket 009 conserve la séparation stricte `ArenaDefinition` / `MapTemplate` / `GameInstance` : le runtime référence des snapshots administratifs et travaille uniquement dans un clone jetable.
