# HeneriaZombie

HeneriaZombie est un futur mini-jeu Zombies pour un serveur Paper unique, inspiré de l'expérience
classique de Black Ops 2 et enrichi de mécaniques originales facultatives par map.

## État

Les Tickets 001 à 007 fournissent la fondation, le lobby, les instances isolées, le framework GUI,
l'éditeur universel, les manches, le moteur d'ennemis et le moteur d'armes configurable.

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

Le JAR déployable est produit dans `zombie-plugin/build/libs/HeneriaZombie-0.8.0-SNAPSHOT.jar`.
Pour un serveur local de développement : `.\gradlew.bat :zombie-plugin:runServer`.

## Documentation à lire

Toute reprise commence par `docs/AI_CONTEXT.md`, puis `docs/ARCHITECTURE.md` et
`docs/DECISIONS.md`. `docs/ROADMAP.md` décrit la suite sans présenter les fonctions futures comme
terminées.
