# Système de cartes modèles

## Interface guidée Ticket 008

Le menu v4 configure et applique heure, cycles, météo, difficulté, PVP, créatures, propagation du feu, dégâts environnementaux et autosauvegarde. Les changements sont d'abord persistés, puis appliqués au monde chargé; un échec restaure les anciennes métadonnées. Les archives complètes, duplications et suppressions sont préparées sur le thread serveur puis copiées hors thread avec verrou et suivi d'état.

## Périmètre

Le Ticket 007 gère des cartes modèles administratives. Une carte peut être créée, chargée, éditée manuellement, sauvegardée, déchargée, dupliquée et associée à une arène. Elle n'est ni une instance de partie ni une copie jetable. Le dossier `instances/` et le préfixe `hbw_game_` sont réservés à un futur ticket.

Types disponibles : `LOBBY`, `BEDWARS` et `GENERIC`. Une arène ne peut référencer qu'une carte `BEDWARS` existante et hors état `ERROR`. Les états `LOADING`, `SAVING` et `UNLOADING` sont transitoires; au redémarrage, l'état réel Bukkit est recalculé.

## Organisation des fichiers

```text
plugins/HeneriaBedWars/
├── worlds.yml
├── maps/
│   ├── metadata/<id>.yml
│   └── templates/<id>/managed-world.txt
├── instances/                         # réservé, non utilisé
└── backups/maps/<horodatage>/<id>/

<conteneur de mondes Bukkit>/
└── hbw_template_<id>/                 # chunks du monde de travail
```

Bukkit `WorldCreator` charge un monde par son nom dans le conteneur de mondes du serveur. Les chunks ne sont donc pas placés sous le dossier de données du plugin. Le marqueur `managed-world.txt` relie explicitement l'identifiant au monde préfixé et empêche le plugin de revendiquer silencieusement un dossier existant.

Les identifiants suivent `[a-z0-9_-]{2,32}`. Les chemins sont reconstruits depuis l'identifiant, normalisés et contrôlés sous leurs racines. Les liens symboliques ne sont jamais suivis. Une collision de marqueur, de métadonnée ou de monde provoque un refus sans publication partielle.

## Cycle d'une carte

La création produit un monde vide `NORMAL` avec générateur `VOID`, applique les réglages de difficulté, temps, météo, PVP, animaux, monstres et règles de jeu, puis crée éventuellement une plateforme de sécurité. Le spawn initial utilise le centre de cette plateforme.

Le chargement et le déchargement sont idempotents. Un déchargement normal est refusé si des joueurs sont présents. Avec la permission `heneriabedwars.admin.map.force`, les joueurs sont évacués vers `fallback-world`, puis le monde est sauvegardé selon la configuration et déchargé. Le plugin tente aussi de décharger proprement ses mondes à l'arrêt.

L'autosauvegarde utilise une seule tâche centrale et n'est active que si `auto-save.enabled` vaut `true`. `/bedwars reload` recharge les métadonnées et resynchronise les associations d'arènes, mais ne charge pas tous les mondes.

## Copies et suppressions

La duplication sauvegarde d'abord une source chargée, puis copie hors thread serveur dans un dossier temporaire voisin. `uid.dat`, `session.lock`, `playerdata`, `stats` et `advancements` sont exclus par défaut. Le dossier final et les métadonnées ne sont publiés qu'après réussite complète; une erreur nettoie au mieux les artefacts temporaires sans toucher à la source.

La suppression vérifie en direct les arènes liées et les cartes protégées comme lobby. Elle refuse aussi les joueurs présents. La préparation/décharge Bukkit s'effectue sur le thread serveur, puis la sauvegarde complète, son manifeste et l'effacement confiné s'exécutent en arrière-plan sous le même verrou. Un échec de sauvegarde interdit toute suppression.

## Commandes de contrôle

```text
/bedwars map
/bedwars map create <id> [LOBBY|BEDWARS|GENERIC]
/bedwars map list
/bedwars map info <id>
/bedwars map load <id>
/bedwars map teleport <id>
/bedwars map setspawn <id>
/bedwars map save <id>
/bedwars map unload <id> [force]
/bedwars map duplicate <source> <destination>
/bedwars map delete <id>
/bedwars arena setmap <arena> <map>
```

Pour une validation en jeu : créer une carte `BEDWARS`, vérifier le monde vide et la plateforme, construire quelques blocs, définir le spawn, sauvegarder/décharger/recharger, vérifier les blocs et le spawn, dupliquer, associer à une arène, confirmer que sa suppression est refusée, délier ou supprimer l'arène puis supprimer la copie et contrôler la sauvegarde sur disque.
