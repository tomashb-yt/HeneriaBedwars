# Publication et catalogue de maps

**Statut :** opérationnel en `0.10.3-SNAPSHOT`.

## Deux snapshots distincts

`plugins/HeneriaZombie/maps/<mapId>/map.yml` est la copie de travail auto-sauvegardée par
l'éditeur. Une publication ne rend jamais ce fichier directement visible des joueurs : elle crée
un `PublishedMapVersion` immuable sous
`plugins/HeneriaZombie/maps/<mapId>/versions/v<version>.yml`, puis remplace atomiquement la version
active dans `publication.yml`.

Le monde déclaré par `MapDefinition.world` est le monde d'édition chargé séparément des
instances. Avant un test ou une publication, Paper le sauvegarde, puis le pool I/O remplace le
modèle `zombie_templates/<mapId>` par une copie sûre. Chaque publication conserve aussi les blocs
dans `world-versions/v<version>/`. Les parties publiques sont clonées depuis ce snapshot physique,
jamais depuis le monde de travail.

Le menu peut dupliquer une map : un nouveau monde sous `zombie_editing/` et une définition dont
tous les points ciblent ce monde sont créés. L'archivage retire une map du catalogue sans
supprimer ses données. Une archive reste restaurable ou republiable.

La suppression définitive est une action différente et irréversible. Elle efface la définition
complète, donc aussi tous les spawns, portes, zones et objets, ainsi que `publication.yml`,
`versions/`, `world-versions/` et `zombie_templates/<mapId>`. Un monde d'édition est supprimé
seulement s'il appartient au plugin sous `zombie_editing/hz_edit_<mapId>` ; le monde externe
éventuellement utilisé lors de la création reste intact.

Une modification ultérieure remplace seulement le snapshot du `MapRegistry`. Le catalogue joueur
et les nouvelles parties publiques continuent à utiliser la dernière version publiée. Une partie
déjà démarrée conserve en plus son propre `MapDefinition` dans son `RuntimeState`.

## Cycle de publication

Le menu `/zombies admin` propose le parcours suivant :

```text
Gestion des maps
→ Créer ou ouvrir
→ Visiter le template dans une copie temporaire
→ Configurer et sauvegarder
→ Valider
→ Tester
→ Publier
```

La publication est refusée si le validateur produit une erreur bloquante ou si le dossier
`zombie_templates/<mapId>` n'est pas un modèle de monde valide. Après l'écriture asynchrone des
fichiers, le statut passe à `PUBLISHED` et le catalogue joueur est immédiatement actualisé.

Dépublier passe le statut à `READY`. Les parties existantes ne sont pas arrêtées. Restaurer une
ancienne version crée une nouvelle version publiée : aucun historique n'est réécrit.

## Catalogue et entrée joueur

`/zombies` n'affiche que les publications au statut `PUBLISHED`. Chaque entrée indique nom, icône,
description, difficulté, mode, capacité, état, nombre de parties, joueurs présents et version.

Un clic cherche d'abord une instance publique joignable. À défaut, il prépare automatiquement un
clone public, y place le joueur et démarre la partie. Le joueur n'a jamais à connaître l'UUID de
l'instance. Si le nombre minimum n'est pas atteint, l'instance reste en attente ; le joueur
suivant déclenche automatiquement le démarrage au seuil configuré. Le bouton de sortie arrête sa
participation et le renvoie au lobby.

## Concurrence et sécurité

- une seule écriture de publication s'exécute à la fois par map ;
- le candidat est persisté avant d'être exposé en mémoire ;
- les versions sont monotones et les snapshots existants ne sont jamais écrasés ;
- les écritures YAML et copies de monde utilisent le pool I/O ;
- une map ne peut avoir qu'un éditeur actif ;
- une suppression est refusée pendant une instance ou une édition et attend les écritures en cours ;
- les permissions sont vérifiées au clic ;
- les restaurations passent par la confirmation sensible partagée.

## Validation manuelle

1. ouvrir `/zombies admin`, créer ou choisir une map ;
2. valider puis publier une map possédant un dossier `zombie_templates/<id>` valide ;
3. vérifier son apparition immédiate dans `/zombies` ;
4. rejoindre et constater la création automatique d'une partie ;
5. modifier le nom de la copie de travail et vérifier que le catalogue conserve l'ancien nom ;
6. publier de nouveau et vérifier la nouvelle version ;
7. restaurer la première version depuis l'historique ;
8. dépublier et vérifier la disparition du catalogue sans arrêt de la partie déjà lancée ;
9. répéter avec deux administrateurs et confirmer le verrou exclusif d'édition.
10. archiver une map et vérifier que ses fichiers restent présents ;
11. supprimer une map sans instance ni éditeur, puis vérifier la disparition de sa configuration,
    de ses versions et de son modèle.

Les réglages avancés de simulation (joueurs artificiels, portes forcées, courant, perks et
invincibilité) nécessitent leurs moteurs de gameplay respectifs et ne sont pas simulés.
