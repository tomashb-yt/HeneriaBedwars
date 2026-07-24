# HeneriaZombie

HeneriaZombie est un futur mini-jeu Zombies pour un serveur Paper unique, inspiré de l'expérience
classique de Black Ops 2 et enrichi de mécaniques originales facultatives par map.

## État

Le Ticket 001 fournit uniquement une fondation exécutable : architecture en trois modules,
configuration versionnée, cycle de vie, API de diagnostic, commande `/zombie`, tests et
documentation centrale. Aucun gameplay, aucune map et aucune instance ne sont encore jouables.

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

Le JAR déployable est produit dans `zombie-plugin/build/libs/HeneriaZombie-0.1.0-SNAPSHOT.jar`.
Pour un serveur local de développement : `.\gradlew.bat :zombie-plugin:runServer`.

## Documentation à lire

Toute reprise commence par `docs/AI_CONTEXT.md`, puis `docs/ARCHITECTURE.md` et
`docs/DECISIONS.md`. `docs/ROADMAP.md` décrit la suite sans présenter les fonctions futures comme
terminées.
