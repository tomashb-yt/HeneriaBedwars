# Contexte du projet

État Ticket 017 du 2026-07-18 : chaque victoire produit un résultat immuable avant la destruction de l'instance. `StatisticsService` agrège parties, victoires, défaites, kills, morts, final kills, lits, temps de jeu et séries. SQLite est le stockage actif par défaut, toutes les opérations JDBC utilisent un exécuteur dédié et l'UUID de partie rend l'écriture idempotente. Les joueurs consultent leur profil avec `/bw stats` ou `/bedwars stats`.

État Ticket 016 du 2026-07-18 : le combat `legacy_1_8` est actif dans les clones en `PLAYING`. Les attaques n'attendent plus le cooldown 1.9+, les boucliers et dégâts de balayage sont neutralisés, les épées récupèrent le point de dégât historique et le knockback horizontal/vertical est configurable. `CombatPolicy` protège spectateurs, joueurs en respawn, protection de réapparition, équipes alliées et instances différentes; le vide conserve le dernier attaquant récent. Les réglages originaux du joueur sont restaurés à sa sortie.

État Ticket 015 du 2026-07-18 : la boutique d'objets propose désormais armures permanentes, pioches et haches à quatre niveaux ainsi que cisailles permanentes. Les outils exigent le niveau précédent et régressent d'un niveau à chaque mort. Un second PNJ, placé depuis la fiche d'équipe, vend Tranchant, Protection et Hâte; l'effet est partagé immédiatement avec les membres vivants et réappliqué au respawn.

Correctif gameplay 012–014 du 2026-07-18 : les clones forcent le PVP, les joueurs vivants peuvent poser puis casser leurs propres blocs sans modifier la carte d'origine, et la mort utilise le respawn immédiat avec un compteur visible chaque seconde. La boutique colore la laine selon l'équipe, colore les prix selon la monnaie et nomme les drops Fer/Or/Diamant/Émeraude Heneria. Après victoire, les joueurs reviennent au lobby avant la suppression du clone; `/bw join` recrée ensuite une instance propre depuis l'arène active.

Correctif Tickets 013–014 du 2026-07-18 : la fiche d'équipe adopte trois colonnes fixes spawn/lit/boutique et masque les actions impossibles. Les anciens `shops.yml` reçoivent désormais non destructivement le catalogue manquant; les PNJ sont recréés au démarrage de la partie même si le catalogue est vide. Les minerais apparaissent au centre exact du bloc, restent ancrés, suivent un rythme borné selon équipes/joueurs et les points diamant/émeraude affichent leur prochaine échéance par hologramme.

État Ticket 014 du 2026-07-18 : la fiche de chaque équipe configure la position et l'orientation d'un PNJ de boutique. À la création du clone, un villageois protégé et identifié par PDC apparaît à cet emplacement. Pendant `PLAYING`, les offres viennent de `shops.yml` et les échanges inventaire sont atomiques. Le Ticket 015 étend ce socle à l'équipement et au second PNJ.

État Ticket 013 phase 2 du 2026-07-18 : les générateurs fer, or, diamant et émeraude sont placés depuis l'assistant d'arène, persistés dans `arenas/<id>.yml`, remappés vers le clone puis activés uniquement en `PLAYING`. Le ticker central utilise `GameGeneratorService`; l'adaptateur Bukkit compte les items proches, fusionne les piles et crée les drops sans tâche par générateur. Les hologrammes, sons, particules et améliorations de niveau restent futurs.

État Ticket 012 du 2026-07-18 : le clone runtime indexe les deux blocs de chaque lit. Un ennemi peut le détruire une seule fois; le propriétaire est bloqué. `GameDeathService` décide respawn ou mort finale, `GameRespawnService` utilise le ticker central, puis la dernière équipe active déclenche `ENDING` et le nettoyage différé. Le code et les tests automatisés sont disponibles; la validation Paper multijoueur reste obligatoire avant de déclarer le ticket définitivement terminé.

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
