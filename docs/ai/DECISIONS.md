# Décisions d'architecture

## ADR-001 — Utilisation de Java 21

### Statut
Accepté.

### Contexte
Paper 1.21.x requiert un runtime Java moderne.

### Décision
Compiler et exécuter avec Java 21.

### Raisons
Version LTS, records et langage moderne, compatibilité Paper.

### Conséquences
Les serveurs et outils de build doivent fournir un JDK 21.

## ADR-002 — Utilisation de Gradle Kotlin DSL

### Statut
Accepté.

### Contexte
Le build doit être reproductible et multi-modules.

### Décision
Utiliser Gradle 8.14.5 via Wrapper et des scripts `.gradle.kts`.

### Raisons
Configuration typée, Wrapper portable et bonne gestion des modules.

### Conséquences
Le Wrapper est la seule commande de build prise en charge.

## ADR-003 — Architecture multi-modules

### Statut
Accepté.

### Contexte
L'API, le métier et Paper évoluent à des rythmes différents.

### Décision
Créer `bedwars-api`, `bedwars-core` et `bedwars-plugin`.

### Raisons
Frontières lisibles et tests indépendants du serveur.

### Conséquences
Les dépendances inverses ou cycliques sont interdites.

## ADR-004 — Séparation de Paper et de la logique métier

### Statut
Accepté.

### Contexte
Le métier doit être testable sans serveur Minecraft.

### Décision
Limiter Paper au module plugin et utiliser des abstractions dans le cœur.

### Raisons
Tests rapides, faible couplage et évolutivité.

### Conséquences
Les futurs accès serveur passeront par des adaptateurs explicites.

## ADR-005 — Documentation obligatoire entre les tickets

### Statut
Accepté.

### Contexte
Plusieurs humains et IA doivent reprendre le projet sans perte de contexte.

### Décision
Un ticket ne peut être terminé sans mise à jour de `AGENTS.md` et `docs/ai`.

### Raisons
Le code seul ne capture ni limites ni intentions.

### Conséquences
La documentation fait partie des critères de build fonctionnels du projet.

## ADR-006 — Pas de framework d'injection lourd

### Statut
Accepté.

### Contexte
Le socle possède peu de composants.

### Décision
Utiliser l'injection par constructeur et un registre léger ; ni Spring ni Guice.

### Raisons
Initialisation explicite et temps de démarrage réduit.

### Conséquences
Le bootstrap reste le composition root manuel.

## ADR-007 — Aucun accès nullable silencieux aux services obligatoires

### Statut
Accepté.

### Contexte
Un service obligatoire absent est une erreur de programmation.

### Décision
`require` lève `MissingServiceException`; `find` renvoie `Optional` pour les services réellement facultatifs.

### Raisons
Les échecs sont immédiats et explicites.

### Conséquences
Le registre ne renvoie jamais `null`.

## ADR-008 — Manifeste plugin.yml stable

### Statut
Accepté.

### Contexte
Le Ticket 001 n'a besoin que d'une commande Bukkit simple.

### Décision
Utiliser `plugin.yml` avec `api-version: 1.21.11`.

### Raisons
Solution Paper stable, documentée et suffisante pour ce périmètre.

### Conséquences
Une migration vers le manifeste Paper nécessitera un ADR si une API moderne la justifie.

## ADR-009 — Licence propriétaire temporaire

### Statut
Accepté.

### Contexte
Aucune licence de redistribution n'a été choisie par le propriétaire.

### Décision
Interdire temporairement la redistribution sans autorisation.

### Raisons
Éviter d'imposer automatiquement MIT, GPL ou Apache.

### Conséquences
Une future licence permanente devra remplacer `LICENSE` et cet ADR.

## ADR-010 — Compilation agrégée du JAR final

### Statut
Accepté temporairement.

### Contexte
L'environnement Windows isolé de validation refuse à `javac` la relecture des dossiers de classes locaux entre tâches, bien que ces classes soient valides. Il verrouille aussi les JAR lors de l'analyse incrémentale Gradle.

### Décision
Conserver les dépendances de projets normales, désactiver les caches et la compilation incrémentale, compiler les sources principales avec les tests du cœur et agréger les sources API/cœur lors de la compilation du module plugin final.

### Raisons
Le build complet et ses tests restent reproductibles dans l'environnement pris en charge, sans modifier les contrats ni embarquer Paper.

### Conséquences
Le module final recompile API et cœur avant de produire le Shadow JAR. Cette mesure pourra être retirée dans un ticket ultérieur après validation sur une CI Windows/Linux non isolée.
