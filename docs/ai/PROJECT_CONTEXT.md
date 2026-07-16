# Contexte du projet

État Ticket 008 : l'administration des cartes modèles est entièrement accessible depuis une interface guidée v4. Les commandes demeurent une surface avancée compatible. Ce ticket ne crée toujours aucune instance de partie ni gameplay BedWars.

HeneriaBedWars vise un plugin BedWars complet pour Paper 1.21.11 et Java 21, avec un combat inspiré du PvP 1.8. Il devra gérer plusieurs arènes et parties simultanées, un lobby principal et des lobbies d'attente, des équipes, lits, générateurs, boutiques et améliorations configurables, des statistiques, une API publique et de futurs addons.

Les stockages visés sont YAML pour l'amorçage, SQLite par défaut, MySQL/MariaDB en production et éventuellement Redis pour le réseau. PlaceholderAPI, Vault, PacketEvents, Citizens et Velocity sont des compatibilités futures, pas des dépendances actuelles.

Les opérations courantes devront être configurables en jeu afin que l'administrateur n'édite pas manuellement des fichiers. Les fichiers resteront utiles aux réglages avancés, sauvegardes, déploiements automatisés, diagnostics et récupérations. La modularité et la documentation doivent permettre une maintenance fiable par plusieurs développeurs et IA.

Depuis le Ticket 005, le projet gère des définitions administratives d'arènes persistantes et validables. Elles ne représentent ni une instance de partie, ni un monde cloné, ni un état de match. Cette séparation doit rester stricte dans les tickets gameplay.

Depuis le Ticket 006, toute la configuration générale de ces définitions est réalisable en jeu. L'éditeur réutilise exclusivement `ArenaService`, sauvegarde après chaque mutation et détecte les vues obsolètes par révision. La saisie chat est réservée aux sessions administratives et n'est pas diffusée.

Depuis le Ticket 007, les cartes modèles sont autonomes, persistantes et chargeables comme mondes Bukkit vides. Une arène peut référencer une carte `BEDWARS`, mais la carte, la définition d'arène et la future instance de partie restent trois concepts distincts. Le dossier `instances/` est réservé : la copie temporaire, le reset et le cycle de match restent futurs.
