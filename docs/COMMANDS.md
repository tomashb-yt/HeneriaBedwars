# Commandes et permissions

**Statut :** commandes runtime, GUI et éditeur opérationnelles.

## Commandes joueur

## Administration de partie

- `/zgame list` liste les parties ;
- `/zgame info <instance>` et `/zgame debug <instance>` affichent leur snapshot ;
- `/zgame start <instance>` lance la partie ;
- `/zgame stop <instance>` demande une fin contrôlée ;
- `/zgame nextround <instance>` accélère une transition ;
- `/zgame setround <instance> <numéro>` choisit la prochaine manche pendant une transition.

Les permissions sont `zombies.admin.game.start`, `.stop`, `.round`, `.list` et `.debug`.
`/zmap test` démarre automatiquement la boucle sur l'instance créée.

- `/zombies` — ouvre le menu principal joueur ;
- `/zombie` — état, version et compteurs réels ;
- `/zombie help` — aide ;
- `/zombie lobby` — retour sûr au lobby ;
- `/zombie instance join <id>` — rejoint l'unique instance correspondant au préfixe UUID ;
- `/zombie instance leave` — quitte l'instance et restaure le lobby.

Elles exigent `zombie.command.use` et, pour les transitions joueur, `zombie.play`.

## Commandes administratives

- `/zombie admin` — ouvre le menu principal administrateur ;
- `/zombie map list` — rescane les dossiers contenant `level.dat` ;
- `/zombie map preview <mapId>` — ouvre une copie temporaire sans créer de partie ;
- `/zombie map leave` — retourne au lobby, décharge et supprime la copie ;
- `/zombie instance create <mapId>` — copie le modèle et crée une instance publique ;
- `/zombie instance list` — liste UUID abrégé, map, état et capacité ;
- `/zombie instance info <id>` — détaille une instance ;
- `/zombie instance stop <id>` — expulse, décharge et nettoie ;
- `/zombie reload` — recharge un candidat valide lorsqu'aucune instance n'est active.
- `/zmap create <id>` — crée une définition dans le monde courant et ouvre l'éditeur ;
- `/zmap edit <id>` — ouvre une session existante ;
- `/zmap leave` — sauvegarde et ferme proprement la session ;
- `/zmap validate` — affiche erreurs, avertissements et conseils ;
- `/zmap test` — valide, sauvegarde puis crée et rejoint une instance privée du modèle homonyme ;
- `/zmap save`, `/zmap undo`, `/zmap redo` — sauvegarde et historique.

Les commandes de map et les quatre commandes d'instance exigent `zombie.instance.admin`. Le reload
exige en plus `zombie.command.reload`. Un préfixe UUID ambigu est refusé. Le reload est refusé
pendant une partie ou un aperçu.

## Permissions

- `zombie.admin` regroupe tous les droits administratifs ;
- `zombie.chat.global` permet le canal global explicite avec le préfixe `!` ;
- `zombie.world.bypass` contourne les protections des mondes d'instance ;
- `zombie.editor` autorise l'éditeur universel et ses GUI ;
- `zombie.play` autorise les commandes joueur.

Les valeurs par défaut et la hiérarchie exacte figurent dans `plugin.yml`.

Les GUI ajoutent `zombie.gui.player`, `zombie.gui.admin`, `zombie.gui.maps`,
`zombie.gui.instances`, `zombie.gui.settings`, `zombie.gui.diagnostics` et
`zombie.gui.dangerous-actions`. La dernière autorise les confirmations destructives.
