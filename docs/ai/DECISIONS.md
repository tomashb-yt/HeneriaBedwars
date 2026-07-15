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
Utiliser `plugin.yml` avec `api-version: 1.21` et enregistrer la commande via l'API Bukkit classique.

### Raisons
Solution stable commune à Spigot et Paper, suffisante pour ce périmètre.

### Conséquences
Les commandes simples ne doivent pas dépendre d'une API Paper. Une migration de manifeste nécessitera un ADR et devra préserver le JAR Spigot si cette cible reste prise en charge.

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

## ADR-011 — YAML Bukkit sans dépendance supplémentaire

### Statut
Accepté.

### Décision
Utiliser `YamlConfiguration` uniquement dans `bedwars-plugin`; le cœur manipule des documents aplatis et immuables.

### Conséquences
Le JAR reste compatible Spigot/Paper sans bibliothèque embarquée. Les commentaires sont conservés tant que le fichier n'est pas réécrit; le changement de langue réécrit `config.yml` et ne garantit donc pas leur conservation parfaite.

## ADR-012 — Configurations Java immuables et snapshots transactionnels

### Statut
Accepté.

### Décision
Convertir les valeurs principales en records et construire un `ConfigurationSnapshot` complet avant activation atomique.

### Conséquences
Un reload invalide conserve intégralement l'état précédent; aucune valeur YAML mutable n'est exposée au reste du plugin.

## ADR-013 — MiniMessage limité rendu sans Adventure obligatoire

### Statut
Accepté.

### Décision
MiniMessage est le format recommandé, avec prise en charge explicite des couleurs nommées, décorations, hexadécimal et codes `&`. Le rendu produit les codes Minecraft legacy sans charger Adventure.

### Conséquences
Le plugin démarre sur Spigot 1.21. Les événements, gradients, hover/click et balises MiniMessage avancées ne sont pas pris en charge.

## ADR-014 — Version 1 obligatoire et migrations séquentielles futures

### Statut
Accepté.

### Décision
Chaque YAML contient `config-version: 1`. Une version absente ou inconnue refuse l'activation. `ConfigurationMigration` et `BackupService` préparent les migrations sans simuler de migration inutile.

### Conséquences
Toute migration future devra sauvegarder le fichier, écrire de façon sûre et avancer version par version.

## ADR-015 — Secrets exclus des diagnostics

### Statut
Accepté.

### Décision
`ConfigurationProblem` masque les clés contenant password, token, secret ou credential. `/bedwars config` n'affiche que le type de stockage.

### Conséquences
Les mots de passe ne sont jamais inclus dans les rapports ni dans les logs debug.

## ADR-016 — Migration étroite du format Ticket 001 non versionné

### Statut
Accepté.

### Contexte
Le Ticket 001 créait un `config.yml` officiel sans `config-version`, ce qui empêchait le Ticket 002 de démarrer lors d'une mise à jour normale.

### Décision
Considérer ce fichier comme version source 0 seulement s'il est lisible et contient la signature `plugin.language` chaîne non vide plus `plugin.debug` booléen. Sauvegarder, fusionner uniquement les défauts absents, puis écrire de façon sûre en version 1.

### Conséquences
Les installations Ticket 001 sont mises à niveau sans suppression de données. Un YAML vide, corrompu ou non reconnaissable n'est jamais « réparé » automatiquement. Les commentaires peuvent être reformattés par `YamlConfiguration`, limitation explicitement documentée.
