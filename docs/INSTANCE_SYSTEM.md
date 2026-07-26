# Système d'instances

**Statut :** opérationnel et relié à la boucle de jeu au Ticket 005.

## Agrégat et cycle de vie

Une instance contient un UUID, un `mapId`, un monde dédié, une capacité, une date de création, des
joueurs, un propriétaire facultatif, un accès public ou privé et un diagnostic d'erreur.

Transitions autorisées :

```text
CREATING -> WAITING | ERROR
WAITING  -> STARTING | ENDING | ERROR
STARTING -> RUNNING | ENDING | ERROR
RUNNING  -> ENDING | ERROR
ENDING   -> CLEANING | ERROR
CLEANING -> CLOSED | ERROR
ERROR    -> CLEANING | CLOSED
CLOSED   -> aucune transition
```

Une transition invalide lève `InvalidInstanceTransitionException` et le service envoie un
diagnostic au logger Paper. Une instance n'entre dans `WAITING` qu'après copie et chargement
réussis de son monde.

## Création et monde

1. le catalogue valide `mapId`, `level.dat` et l'éventuel `zombie-map.yml` hors thread serveur ;
2. le service réserve atomiquement une place dans la capacité globale ;
3. un UUID et un dossier `zombie_instances/hz_<uuid>` sont alloués ;
4. le modèle est copié sur le pool I/O, sans `uid.dat`, `session.lock` ni lien symbolique ;
5. Paper charge le monde sur son thread ;
6. les gamerules configurées sont appliquées et l'instance devient joignable.

Deux créations utilisant le même modèle produisent deux UUID et deux dossiers distincts.

## Aperçu administratif

Une map détectée peut être visitée sans `GameInstance`. Le service d'aperçu crée une copie isolée,
la charge avec le même adaptateur monde, autorise son propriétaire, puis le téléporte au spawn
vanilla. `/zombie map leave` et la déconnexion renvoient ou libèrent le joueur, déchargent la copie
et la suppriment même lorsque la suppression des mondes de partie est désactivée.

## Instance de test éditeur

`/zmap test` réutilise le pipeline d'instance : validation éditoriale, sauvegarde, fermeture de
session, sortie d'un éventuel aperçu, clone privé puis entrée du créateur. Le `mapId` éditorial
doit correspondre à un dossier `zombie_templates/<mapId>` valide.

`/zombie instance leave` quitte le test. L'instance reste inspectable puis peut être supprimée avec
`/zombie instance list` et `/zombie instance stop <id>`. Le test ne contourne ni capacité,
isolation, protection, chargement Paper ni nettoyage.

## Entrée et sortie joueur

`InstanceCoordinator` réalise la transaction entre session, registre, état joueur, téléportation,
scoreboard et visibilité. Une session déjà liée à une autre instance est refusée. En cas de monde
absent ou téléportation échouée, l'appartenance est annulée et le joueur retourne au lobby.

La sortie retire d'abord l'appartenance, restaure le profil lobby, applique le scoreboard lobby et
recalcule toutes les paires de visibilité.

## Isolation

- lobby : seuls les joueurs lobby sont visibles ;
- instance : seuls les membres en ligne du même UUID sont visibles ;
- tablist : suit les mêmes appels Paper de masquage ;
- chat : viewers filtrés par la même politique ;
- morts et connexions : message global supprimé puis audience ciblée ;
- interfaces : un scoreboard lobby et un scoreboard propre à chaque instance.

Tout futur son, titre, boss bar, action bar ou annonce de gameplay doit utiliser
`PaperAudienceService`, jamais `Bukkit.broadcast`.

## Reconnexion

Une déconnexion peut conserver l'UUID d'instance jusqu'à l'échéance configurée. Si
`reserve-player-slot` vaut `false`, la session et la place sont libérées immédiatement. Dans le
délai, la reconnexion valide encore l'instance, recharge le modèle et téléporte réellement au
spawn. Si l'instance ou le modèle n'est plus valide, le lobby devient la destination sûre.

Une tâche unique, exécutée chaque seconde, expire les réservations et retire leurs membres du
registre.

## Fermeture et nettoyage

L'arrêt administratif expulse les joueurs, passe par `ENDING`, `CLEANING`, décharge le monde après
le délai configuré et supprime le dossier si cette option est active. Si le déchargement échoue,
le dossier n'est jamais supprimé et l'instance passe en `ERROR`.

Une copie ou un chargement échoué retire l'instance du registre. Les fichiers partiels sont
supprimés seulement si `preserve-failed-worlds` est désactivé et si le monde a pu être déchargé.

À l'arrêt du serveur, toutes les instances sont marquées interrompues et journalisées. Les mondes
sont déchargés sur le thread serveur, mais leurs dossiers sont conservés par prudence.

## Protections

L'UUID technique de l'instance est aussi celui du `ZombieGame`. Les deux machines restent
distinctes : l'instance possède le monde et les joueurs, la partie possède manches et résultats.
À la fin, le runtime retire ses zombies, renvoie les joueurs, puis demande l'arrêt sûr du monde.

L'entrée technique utilise d'abord le spawn du modèle pour garantir une destination sûre. Dès
qu'une partie démarre, `PaperGameRuntime` traduit les coordonnées du spawn joueur éditorial vers
le monde cloné. La même destination est réappliquée à une arrivée en cours ou une reconnexion.

Les gamerules et listeners contrôlent apparition naturelle, météo, temps, blocs, objets, PVP,
inventaire et sauvetage du vide. `zombie.world.bypass` fournit l'exception administrative
explicite. Une téléportation vers un monde d'instance est refusée si la session ne correspond pas
à son propriétaire logique.
