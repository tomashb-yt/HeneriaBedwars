# Limitations connues

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
