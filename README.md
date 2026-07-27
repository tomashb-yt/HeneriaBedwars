# HeneriaZombie

HeneriaZombie est un futur mini-jeu Zombies pour un serveur Paper unique, inspiré de l'expérience
classique de Black Ops 2 et enrichi de mécaniques originales facultatives par map.

## État

Le projet fournit la fondation, le lobby, les instances isolées, le framework GUI, l'éditeur
universel, les manches, les ennemis, les armes, l'économie et la publication versionnée des maps.

## Prérequis

- Java 21 ;
- Paper 1.21.x ;
- Git pour contribuer. Le wrapper Gradle est inclus.

## Compiler et vérifier

```powershell
.\gradlew.bat clean
.\gradlew.bat build
.\gradlew.bat test
.\gradlew.bat spotlessCheck
.\gradlew.bat qualityGate
```

Le Ticket 006 remplace les zombies temporaires par quatre types YAML, une fabrique Paper, une
sélection pondérée, une IA de mêlée isolée, les dégâts centralisés, le secours anti-blocage et
deux capacités. Les commandes de diagnostic sont regroupées sous `/zzombie`.

Le Ticket 007 ajoute cinq armes YAML, munitions, rechargement, dégâts balistiques, achats muraux,
Mystery Box, Pack-a-Punch et `/zweapon`.

Le JAR déployable est produit dans `zombie-plugin/build/libs/HeneriaZombie-0.10.5-SNAPSHOT.jar`.
Pour un serveur local de développement : `.\gradlew.bat :zombie-plugin:runServer`.

## Documentation à lire

Toute reprise commence par `docs/AI_CONTEXT.md`, puis `docs/ARCHITECTURE.md` et
`docs/DECISIONS.md`. `docs/ROADMAP.md` décrit la suite sans présenter les fonctions futures comme
terminées.

## Ticket 008 — économie de partie

La version `0.9.0-SNAPSHOT` centralise les points dans des portefeuilles `long`, journalise chaque
mutation, rend les achats atomiques et implémente les drops Double Points, Max Ammo, Insta-Kill et
Nuke. Les armes murales, la Mystery Box et le Pack-a-Punch emploient le même `PurchaseService`.

Documentation : [économie](docs/ECONOMY_SYSTEM.md),
[transactions](docs/TRANSACTION_SYSTEM.md), [achats](docs/PURCHASE_SYSTEM.md) et
[bonus](docs/POWER_UP_SYSTEM.md).

## Stabilisation 0.9.1

Le clic droit des armes est désormais capturé même lorsque Paper pré-annule l'interaction de
l'item. L'éditeur affiche des repères lumineux pour les spawns, portes et barricades, et matérialise
la Mystery Box par un coffre et le Pack-a-Punch par un coffre de l'Ender. Ces deux stations sont
également présentes dans les mondes de partie. Les zombies sont forcés en IA agressive, leurs
contacts passent par les dégâts autoritaires du jeu et tout équipement vanilla aléatoire est retiré.

## Stabilisation 0.9.2

La mise à terre distingue désormais une équipe pouvant réanimer d'un joueur sans sauveteur. Les
machines possèdent une animation différée de cinq secondes, leur nom reste visible dans l'instance,
les impacts zombies donnent un retour immédiat et les bonus sont ancrés sur un sol sûr.

## Menus et publication 0.10.0

`/zombies` affiche les maps publiées et rejoint automatiquement une partie. `/zombies admin`
centralise la création, l'édition, la validation, le test et la publication. Chaque publication
est un snapshot immuable de la configuration et des blocs ; l'historique peut être restauré sans
modifier les parties en cours.
Voir [publication des maps](docs/MAP_PUBLICATION.md).

## Stabilisation 0.10.1

Les anciens `guis.yml` sont migrés en mémoire avant fusion afin de retirer les boutons obsolètes
qui entraient en collision avec le bouton de sortie. Le menu d'administration permet également de
supprimer définitivement une map inutilisée avec sa définition, ses spawns, ses objets, son
historique publié, ses snapshots et ses mondes possédés.

Le correctif `0.10.2` migre aussi l'ancien bouton « Gestion des maps » afin qu'il ouvre bien le
gestionnaire contenant les actions Tester, Publier et Supprimer.

La version `0.10.3` réorganise ce gestionnaire en trois étapes lisibles et ajoute « Visiter la
map », qui téléporte l'administrateur dans une copie privée sans démarrer de partie.
