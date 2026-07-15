# Guide de reprise — HeneriaBedWars

## Présentation

HeneriaBedWars est un futur plugin BedWars modulaire pour Paper 1.21.11. Il utilise Java 21, Gradle Kotlin DSL et le package racine `fr.heneria.bedwars`. La version actuelle est `0.1.0-SNAPSHOT`. La priorité est de garder la logique métier indépendante de Paper, testable et compatible avec plusieurs parties simultanées. Aucun gameplay n'existe à l'issue du Ticket 001.

## Lecture obligatoire

Avant chaque ticket, lire dans cet ordre :

1. `AGENTS.md` ;
2. `docs/ai/PROJECT_CONTEXT.md` ;
3. `docs/ai/ARCHITECTURE.md` ;
4. `docs/ai/CURRENT_STATE.md` ;
5. `docs/ai/ROADMAP.md` ;
6. `docs/ai/DECISIONS.md` ;
7. `docs/ai/KNOWN_ISSUES.md` ;
8. les dernières entrées de `docs/ai/TICKET_HISTORY.md`.

Inspecter ensuite Git, tous les Markdown pertinents et les fichiers touchés. Ne supprimer ni écraser une ressource existante sans analyse. Vérifier qu'aucun secret n'est ajouté.

## Commandes utiles

```bash
./gradlew clean
./gradlew build
./gradlew test
./gradlew shadowJar
./gradlew spotlessCheck
./gradlew spotlessApply
```

Sous Windows, remplacer `./gradlew` par `.\gradlew.bat`. Le JAR déployable est produit par `:bedwars-plugin:shadowJar`.

## Règles architecturales

- `bedwars-api` ne dépend ni de Paper, ni de `bedwars-core`, ni de `bedwars-plugin`.
- `bedwars-core` peut dépendre de l'API mais doit rester indépendant de Paper.
- `bedwars-plugin` est la seule frontière Paper et peut dépendre des deux autres modules.
- Aucune logique métier importante dans les listeners, commandes ou classe principale.
- Aucune requête SQL dans un menu et aucune opération lourde sur le thread serveur.
- Aucun accès d'une API publique aux implémentations internes.
- Aucune nouvelle dépendance sans justification dans `DECISIONS.md`.
- Aucune modification silencieuse d'une API publique.
- Aucun `TODO` sans référence de ticket, aucune fonctionnalité dupliquée et aucune exception importante ignorée.
- Préférer l'injection par constructeur ; ne pas introduire de singleton global mutable.

## Définition de terminé

Un ticket est terminé uniquement si le code compile, tous les tests et le formatage passent, le JAR attendu est produit, la documentation et la roadmap reflètent le code réel, l'historique du ticket et les décisions sont à jour, aucun secret ou fichier temporaire n'est ajouté et `git diff --check` ne remonte aucune erreur.
