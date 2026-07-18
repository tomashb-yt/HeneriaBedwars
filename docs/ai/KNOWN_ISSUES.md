# Limitations connues

## Correctif générateurs et PNJ

- Les `TextDisplay`, l'ancrage des items et la réparation des villageois doivent encore être confirmés visuellement sur Paper 1.21.x.
- Le rythme est capturé au passage `PLAYING`; une déconnexion ultérieure ne recalcule pas les intervalles en cours afin de garder un calendrier stable.
- Les hologrammes couvrent diamant et émeraude. Fer et or restent volontairement sans texte pour éviter de surcharger les bases.

## Ticket 014 — boutiques

- Les PNJ, clics, menus et échanges d'inventaire doivent encore être validés sur un serveur Paper avec plusieurs joueurs.
- Le catalogue livre des objets simples. Les armures équipées, outils évolutifs, raccourcis favoris, pièges et améliorations d'équipe appartiennent au Ticket 015.
- La laine du catalogue est actuellement blanche; sa coloration automatique selon l'équipe sera traitée avec l'équipement contextuel.
- Une équipe sans position de boutique reste jouable mais aucun PNJ n'est créé pour elle; la fiche indique clairement l'étape manquante.

## Ticket 013 — phase 2

- Les positions, la persistance, l'assistant et les drops runtime sont disponibles, mais doivent encore être confirmés sur un serveur Paper avec une vraie carte clonée.
- L'écran affiche au maximum quatorze générateurs par arène; une pagination sera nécessaire pour les cartes qui dépassent cette limite.
- Particules, sons et améliorations automatiques de niveau restent dans les phases suivantes.

## Ticket 012 en validation

- Les anciennes sélections de lit à un seul bloc doivent être refaites depuis la fiche d'équipe.
- Le téléporteur spectateur suit actuellement le premier joueur actif; une liste graphique complète reste une amélioration ultérieure.
- La reconnexion en `PLAYING` n'est pas prise en charge : une déconnexion est une élimination définitive.
- La validation automatisée ne remplace pas un test Paper à deux comptes des événements de mort, respawn, destruction et fin.
- Les générateurs sont désormais actifs lorsqu'ils sont configurés; boutiques, économie, équipement final, récompenses et PvP 1.8 complet restent absents.

- La téléportation d'équipe au passage `PLAYING` est couverte par la résolution métier et la compilation Bukkit, mais doit encore être confirmée avec plusieurs joueurs sur un serveur Paper réel.

## Import de cartes externes

- Le monde importé doit correspondre à la version Minecraft du serveur; le plugin ne convertit pas les formats de chunks.
- L'opération est couverte par tests de fichiers, mais l'évacuation, le déchargement et le rechargement doivent encore être validés sur un serveur Paper réel.
- Après remplacement, les coordonnées administratives conservées peuvent ne plus correspondre au nouveau build : revérifier attente, spectateur, spawns et lits depuis l'assistant avant activation.

## Préparation Ticket 012 — configuration des équipes

- La sélection GUI doit encore être validée sur Paper : l'administrateur doit être dans le monde modèle, regarder un lit complet à moins de huit blocs avant de cliquer sur « Sélectionner le lit regardé ».
- La définition persistante conserve le pied historique ainsi que la tête et la direction dans ses métadonnées compatibles; les anciennes sélections à un bloc doivent être refaites.
- Huit équipes au maximum sont affichées dans la vue compacte actuelle; les formats supérieurs restent persistables mais nécessiteront une pagination du menu.

## Correctif Ticket 010.1

- Le matchmaking public, les PNJ et la commande courte `/bw` restent futurs; `game join|leave` est la surface publique temporaire.
- Les équipes BedWars détaillées et le scoreboard `PLAYING` restent futurs.
- Paper 1.21 peut masquer les nombres via `NumberFormat.blank`; le JAR conserve sa compatibilité Spigot et laisse les scores visibles lorsque cette capacité n'existe pas.
- Les clics, menus, restaurations et affichages doivent encore être validés sur un serveur Paper réel.

## Ticket 010

- Le passage en `PLAYING` est structurel : il ne demarre ni lit, generateur, boutique, mort BedWars, condition de victoire ni spectateur gameplay.
- Les snapshots de joueur sont seulement en memoire. Une deconnexion abandonne le snapshot et un crash ne fournit ni restauration offline ni reconnexion.
- Les listeners, teleports, inventaires, bossbars, scoreboards, titres, sons, protections et suppression de clone doivent encore etre testes avec plusieurs joueurs sur un serveur Paper reel.
- La liste administrative affiche les 28 premieres instances; un filtre ou une pagination runtime sera ajoute si un usage depasse cette limite.
- `start --force` est volontairement reserve a l'administration de test et ne doit pas etre expose a des joueurs ordinaires.

## Ticket 009

- Il s'agit d'un moteur de cycle de vie, pas d'une partie BedWars jouable : les équipes runtime sont des emplacements génériques et aucun lit, générateur, achat, mort finale ou vainqueur n'est calculé.
- Sous Bukkit/Spigot, les chunks actifs doivent vivre dans le conteneur de mondes. `instances/game-<UUID>/world/active-world.txt` conserve le manifeste tandis que les chunks sont dans `hbw_game_<UUID>`; les deux emplacements sont supprimés ensemble.
- Après un arrêt brutal, les dossiers orphelins sont supprimés de façon asynchrone avant la première nouvelle création. Aucune reconnexion ou reprise de partie après crash n'est fournie.
- Le stockage runtime est uniquement mémoire. Redis, SQL, proxy, matchmaking, replay et reconnexion sont des fonctions futures, pas des fonctionnalités simulées.
- Le comportement réel de copie, chargement, téléportation et nettoyage doit encore être validé sur Paper avec plusieurs joueurs.

## Ticket 008

- L'éditeur est validé automatiquement mais doit encore être essayé sur Paper pour les inventaires, saisies chat, téléportations et règles de monde.
- Le suivi affiche état, durée et détail, pas un pourcentage d'octets copiés.
- Les associations montrent les 21 premières arènes disponibles; une pagination sera nécessaire au-delà.
- La détection de modifications couvre blocs, seaux, explosions et structures, pas toutes les mutations possibles d'autres plugins.
- La sélection globale du lobby principal reste hors de ce menu; une carte déjà configurée comme lobby reste protégée.

## Ticket 007

- Les cartes sont des modèles persistants administratifs; aucune instance temporaire, copie par partie, remise à zéro ou nettoyage de match n'est actif.
- Seul le générateur `VOID` est livré. L'import d'un monde externe, les générateurs tiers et les dimensions personnalisées ne sont pas pris en charge.
- Les mondes Bukkit vivent dans le conteneur de mondes du serveur avec un préfixe contrôlé; `maps/templates/` contient les marqueurs de propriété, pas les chunks actifs.
- Un changement des chemins ou préfixes de `worlds.yml` demande un redémarrage complet et ne déplace pas les anciens mondes.
- La duplication et la suppression peuvent prendre du temps selon la taille du monde; elles sont asynchrones mais aucune barre de progression persistante n'est fournie.
- La création/édition réelle de mondes, les téléportations, l'évacuation forcée, les règles de jeu et le comportement après crash doivent être testés manuellement sur Paper.
- Les anciennes arènes utilisant `map.template` sont lues pour compatibilité, puis écrites sous `map.template-id` lors d'une prochaine sauvegarde.

## Ticket 006

- L'éditeur configure une définition administrative mais ne crée aucune partie jouable et ne copie, charge ou réinitialise aucun monde.
- Les équipes restent limitées au nombre et aux joueurs par équipe : pas de couleurs, membres runtime, lits, générateurs, boutiques ni PNJ.
- La sélection de monde liste les mondes déjà chargés par Bukkit; aucun template ou import n'est disponible avant le Ticket 007.
- La saisie textuelle est limitée au chat et doit être validée manuellement sur Paper, notamment annulation, timeout, déconnexion et non-diffusion.
- Les menus, téléportations, permissions et conflits avec deux comptes n'ont pas été testés en jeu dans l'environnement Codex.
- L'audit administratif est un log simple; aucun historique persistant n'est encore stocké.

## Ticket 005

- Les arènes sont des définitions administratives : aucune partie, équipe runtime, lit, générateur, clonage ou reset de monde n'est actif.
- Le premier menu du Ticket 005 a été remplacé par l'éditeur Ticket 006; les templates et instances temporaires relèvent du Ticket 007.
- Les mondes sont seulement résolus parmi les mondes actuellement chargés par Bukkit ; aucun chargement ou import automatique n'est effectué.
- Les YAML d'arènes n'ont qu'une version 1 et aucune migration historique n'est nécessaire à ce stade.
- Aucun test en jeu des commandes, permissions, téléportations futures ou inventaires d'arènes n'a été exécuté dans l'environnement Codex.

## Ticket 004

- Aucun éditeur complet d'items en jeu, aucune boutique et aucun objet de gameplay actif.
- Les têtes utilisent le joueur contextuel par UUID; un nom statique hors ligne n'est pas résolu afin d'éviter tout appel réseau synchrone. Les textures Base64 ne sont pas prises en charge.
- Aucun resource pack n'est fourni; `custom-model-data` ne garantit donc aucun modèle visuel.
- Le rendu texte conserve le sous-ensemble MiniMessage/legacy du Ticket 002, pas l'ensemble Adventure avancé.
- Les enchantements configurables restent sûrs et bornés; aucun niveau unsafe arbitraire.
- La compatibilité est compilée avec Spigot API 1.21 et cible Paper 1.21.x, mais les métadonnées/PDC et commandes n'ont pas été exécutés sur un serveur réel dans cet environnement.

## Ticket 002

- Le rendu MiniMessage est limité aux couleurs, décorations et hex; aucune balise interactive ou gradient.
- `YamlConfiguration` ne garantit pas la conservation des commentaires lorsque `config.yml` est réécrit par `language set`.
- Le reload manuel est synchrone; ce choix est adapté aux petits fichiers actuels.
- Les clés de commandes dans `config.yml` sont documentaires : commande et alias réels restent dans `plugin.yml` et demandent un redémarrage.
- Les réglages gameplay, lobby, menus, boutiques, upgrades, générateurs et stockage sont préparatoires.
- Aucun test en jeu n'a été exécuté dans l'environnement Codex actuel.
- La migration automatique concerne uniquement le `config.yml` reconnaissable du Ticket 001; les autres fichiers non versionnés restent volontairement refusés.

- Aucun gameplay, arène, équipe, lit, générateur, boutique ou menu métier; seule la démonstration GUI est disponible.
- Aucun stockage fonctionnel malgré la valeur d'amorçage `sqlite`.
- API publique minimale non encore publiée auprès de Paper.
- Compatibilité compilée contre Spigot API 1.21 et prévue pour Paper 1.21.x ; le chargement et les commandes doivent encore être validés manuellement sur les deux plateformes.
- `/bedwars`, `/hbw`, les permissions, la console, les langues et la tab-complétion n'ont pas encore été testés en jeu après redémarrage complet.
- Le document historique `docs/ARCHITECTURE.md` décrit une cible plus large que les trois modules actuels.
- Le build désactive temporairement les caches/incréments et agrège les sources dans le module final pour contourner l'isolation du classpath Windows observée pendant le Ticket 001 ; cette mesure devra être réévaluée sur CI.
- Aucun test en jeu du framework n'a été réalisé ici; la compatibilité compilée Spigot/Paper ne vaut pas validation runtime Paper.
