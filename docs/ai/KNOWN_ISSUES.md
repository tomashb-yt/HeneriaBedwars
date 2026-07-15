# Limitations connues

## Ticket 002

- Le rendu MiniMessage est limité aux couleurs, décorations et hex; aucune balise interactive ou gradient.
- `YamlConfiguration` ne garantit pas la conservation des commentaires lorsque `config.yml` est réécrit par `language set`.
- Le reload manuel est synchrone; ce choix est adapté aux petits fichiers actuels.
- Les clés de commandes dans `config.yml` sont documentaires : commande et alias réels restent dans `plugin.yml` et demandent un redémarrage.
- Les réglages gameplay, lobby, menus, boutiques, upgrades, générateurs et stockage sont préparatoires.
- Aucun test en jeu n'a été exécuté dans l'environnement Codex actuel.

- Aucun gameplay, arène, équipe, lit, générateur, boutique ou menu.
- Aucun stockage fonctionnel malgré la valeur d'amorçage `sqlite`.
- API publique minimale non encore publiée auprès de Paper.
- Compatibilité compilée contre Spigot API 1.21 et prévue pour Paper 1.21.x ; le chargement et les commandes doivent encore être validés manuellement sur les deux plateformes.
- `/bedwars`, `/hbw`, les permissions, la console, les langues et la tab-complétion n'ont pas encore été testés en jeu après redémarrage complet.
- Le document historique `docs/ARCHITECTURE.md` décrit une cible plus large que les trois modules actuels.
- Le build désactive temporairement les caches/incréments et agrège les sources dans le module final pour contourner l'isolation du classpath Windows observée pendant le Ticket 001 ; cette mesure devra être réévaluée sur CI.
