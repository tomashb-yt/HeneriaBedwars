# HeneriaZombie

HeneriaZombie est un futur mini-jeu Zombies pour un serveur Paper unique, inspiré de l'expérience
classique de Black Ops 2 et enrichi de mécaniques originales facultatives par map.

## État

Les Tickets 001 à 005 fournissent la fondation, le lobby, les instances isolées, le framework GUI
et l'éditeur universel. La boucle de manches et les zombies ne sont pas encore implémentés.

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

Le Ticket 005 ajoute compte à rebours, manches configurables, zombies standards temporaires,
points, mise à terre, réanimation, défaite et retour au lobby.

Le JAR déployable est produit dans `zombie-plugin/build/libs/HeneriaZombie-0.6.0-SNAPSHOT.jar`.
Pour un serveur local de développement : `.\gradlew.bat :zombie-plugin:runServer`.

## Documentation à lire

Toute reprise commence par `docs/AI_CONTEXT.md`, puis `docs/ARCHITECTURE.md` et
`docs/DECISIONS.md`. `docs/ROADMAP.md` décrit la suite sans présenter les fonctions futures comme
terminées.
