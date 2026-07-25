# Commandes et permissions

**Statut :** commandes temporaires du Ticket 002 opérationnelles.

## Commandes joueur

- `/zombie` — état, version et compteurs réels ;
- `/zombie help` — aide ;
- `/zombie lobby` — retour sûr au lobby ;
- `/zombie instance join <id>` — rejoint l'unique instance correspondant au préfixe UUID ;
- `/zombie instance leave` — quitte l'instance et restaure le lobby.

Elles exigent `zombie.command.use` et, pour les transitions joueur, `zombie.play`.

## Commandes administratives

- `/zombie map list` — rescane les dossiers contenant `level.dat` ;
- `/zombie map preview <mapId>` — ouvre une copie temporaire sans créer de partie ;
- `/zombie map leave` — retourne au lobby, décharge et supprime la copie ;
- `/zombie instance create <mapId>` — copie le modèle et crée une instance publique ;
- `/zombie instance list` — liste UUID abrégé, map, état et capacité ;
- `/zombie instance info <id>` — détaille une instance ;
- `/zombie instance stop <id>` — expulse, décharge et nettoie ;
- `/zombie reload` — recharge un candidat valide lorsqu'aucune instance n'est active.

Les commandes de map et les quatre commandes d'instance exigent `zombie.instance.admin`. Le reload
exige en plus `zombie.command.reload`. Un préfixe UUID ambigu est refusé. Le reload est refusé
pendant une partie ou un aperçu.

## Permissions

- `zombie.admin` regroupe tous les droits administratifs ;
- `zombie.chat.global` permet le canal global explicite avec le préfixe `!` ;
- `zombie.world.bypass` contourne les protections des mondes d'instance ;
- `zombie.editor` réserve le futur éditeur ;
- `zombie.play` autorise les commandes joueur.

Les valeurs par défaut et la hiérarchie exacte figurent dans `plugin.yml`.
