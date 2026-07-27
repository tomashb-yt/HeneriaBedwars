# Refonte de la gestion des maps

**Statut :** tranche 1 opérationnelle en `0.11.0-SNAPSHOT`.

## Audit de l'ancien parcours

L'ancien gestionnaire mélangeait trois identités différentes :

- le template technique sous `zombie_templates/<id>` ;
- la définition Zombies persistée dans `maps/<id>/map.yml` ;
- un monde Bukkit choisi implicitement lors de la création.

La commande et le menu de création utilisaient le monde courant de l'administrateur. Une création
depuis le lobby pouvait donc référencer le lobby comme monde d'édition. La visite utilisait une
copie temporaire non modifiable, mais aucune opération ne transformait proprement un template en
monde de travail. Enfin, le chargement Paper était dupliqué dans plusieurs adaptateurs et la
sauvegarde manuelle ne persistait que le YAML.

## Invariants du nouveau modèle

Une map gérée possède désormais trois niveaux explicites :

```text
template importé et immuable
→ zombie_editing/hz_edit_<mapId> (monde de travail)
→ snapshot publié immuable
→ zombie_instances/hz_<uuid> (copie jetable par partie)
```

- importer ne charge ni ne modifie jamais le template source ;
- créer une map produit toujours un monde de travail dédié ;
- `MapDefinition.world` référence ce monde de travail, jamais le lobby courant ;
- charger, sauvegarder et décharger passent par `ManagedMapWorldService` ;
- les vérifications de dossiers et copies restent sur le pool I/O ;
- les opérations Bukkit restent sur le thread Paper ;
- sauvegarder depuis l'éditeur persiste les blocs puis la définition ;
- tester et publier sauvegardent le monde de travail avant de remplacer le template technique ;
- les instances continuent d'utiliser une copie indépendante.

## Parcours livré

Depuis `/zombies admin` :

1. ouvrir **Gestion des maps** ;
2. sélectionner un `template importé` ;
3. utiliser **Visiter** pour une inspection sans sauvegarde, ou **Importer et modifier** ;
4. l'import copie le template dans `zombie_editing/hz_edit_<id>`, charge ce monde et ouvre
   l'éditeur dessus ;
5. **Sauvegarder** écrit les blocs et la configuration ;
6. **Décharger le monde** libère explicitement un monde de travail sans joueur ;
7. **Modifier** recharge automatiquement un monde déchargé et téléporte au spawn configuré.

`/zmap create <id>` crée également un monde de travail dédié. `/zmap edit <id>` charge ce monde et
téléporte réellement l'administrateur, au lieu d'ouvrir l'outil dans son monde courant.

## Tranches suivantes

La refonte reste découpée en livraisons vérifiables. Les prochaines tranches doivent ajouter le
catalogue séparé des mondes serveur externes, leur import guidé, les opérations de renommage
physique, puis les réglages avancés manquants de la définition Zombies. Elles ne doivent pas
remettre en cause les invariants ci-dessus.
