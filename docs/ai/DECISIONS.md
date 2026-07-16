# Décisions d'architecture

## ADR-040 — Instance runtime distincte des définitions persistantes

Accepté. `ArenaDefinition` et `MapTemplate` restent administratifs; `GameInstance` ne les modifie jamais et possède un UUID ainsi qu'un clone jetable.

## ADR-041 — Machine d'état fermée et index atomiques

Accepté. Les transitions autorisées sont déclarées dans `GameInstance`. `GameInstanceManager` réserve l'arène avant le clonage, indexe un joueur une seule fois et libère tous les index après destruction ou rollback.

## ADR-042 — Ports asynchrones pour monde et joueur

Accepté. `RuntimeWorldService` et `RuntimePlayerGateway` utilisent `CompletionStage`. L'adaptateur Bukkit copie/supprime hors thread et replannifie chargement, téléportation et déchargement sur le thread serveur. Cette frontière permet de futurs adaptateurs proxy sans ajouter de dépendance maintenant.

## ADR-043 — API publique par snapshots

Accepté. `HeneriaBedWarsApi` expose trois façades en lecture seule et des records immuables. L'implémentation est publiée par le `ServicesManager` Bukkit; aucune classe de `bedwars-core`, collection mutable ou objet Bukkit n'est exposé.

## ADR-039 — Éditeur graphique des cartes

Accepté. Le menu v4 est une façade : toutes les mutations passent par `MapTemplateService` ou `ArenaService`. `MapEditorStateStore` conserve uniquement des données bornées par UUID et les nettoie à la déconnexion. Progression guidée et validation technique restent séparées. `MapOperationTracker` expose sauvegarde, duplication et suppression sans remplacer `MapOperationLock`. Les copies de fichiers sont asynchrones, les arènes restent la source de vérité des associations et l'application des réglages Bukkit est compensée en cas d'échec.

## ADR-029 — Définition d'arène distincte d'une partie

Accepté. `ArenaDefinition` décrit uniquement une configuration administrative persistante. `ArenaStatus` ne réutilise aucun futur état `WAITING/PLAYING/RESETTING` et aucune arène activée ne lance de gameplay.

## ADR-030 — Un YAML UTF-8 sûr par arène

Accepté. `ArenaId` limite les noms à `[a-z0-9_-]{2,32}` et le dépôt écrit `arenas/<id>.yml` par remplacement atomique. Le cœur dépend seulement d'`ArenaRepository`.

## ADR-031 — Publication mémoire après persistance

Accepté. Toute création ou modification est sauvegardée avant le remplacement copy-on-write d'`ArenaRegistry`. Une panne d'écriture laisse l'ancienne définition intacte.

## ADR-032 — Reload partiel et diagnostic visible

Accepté. Une définition structurée mais non activable reste visible comme `INVALID`. Un YAML illisible conserve l'ancienne définition du même id au reload ; au premier chargement il est journalisé et ignoré sans désactiver le plugin.

## ADR-033 — Sauvegarde obligatoire avant suppression

Accepté. Le seul contrat de suppression est `deleteWithBackup`. Le fichier est copié sous `backups/arenas/<date>/` avant sa suppression, avec suffixe anti-collision.

## ADR-022 — Clés logiques et définitions d'items immuables

Accepté. Les items sont adressés par des clés minuscules pointées, normalisées par `ItemKey`. `ItemDefinition` ne contient aucune section YAML ni donnée Bukkit mutable.

## ADR-023 — Reconstruction de chaque ItemStack

Accepté. Seules les définitions validées sont conservées; chaque appel à `ItemService.build` crée un stack et une meta indépendants. Aucun cache d'`ItemStack` traduit ou dynamique n'est partagé.

## ADR-024 — Héritage simple borné et fusion explicite

Accepté. Profondeur maximale 16, parent unique, parent inconnu/cycle refusé. Le lore enfant remplace le lore parent; flags et placeholders requis sont réunis sans doublon; enchantements et tags fusionnent avec priorité enfant.

## ADR-025 — Actions GUI séparées des items

Accepté. `items.yml` décrit uniquement l'apparence et des métadonnées non exécutables. Slots, permissions et actions restent dans les définitions GUI Java.

## ADR-026 — PDC contrôlé par liste blanche

Accepté. `heneriabedwars:item_key` est interne; seuls les tags configurables `category` et `action` peuvent être copiés dans des clés PDC dédiées. Le slot/session reste l'autorité pour les clics GUI.

## ADR-027 — Registre d'items dans le snapshot transactionnel

Accepté. Traductions, héritages et propriétés sont validés avant l'échange de `ConfigurationSnapshot`. Une erreur d'héritage ou critique conserve intégralement l'ancien registre et les menus ouverts ne sont pas fermés.

## ADR-028 — Têtes sans résolution réseau synchrone

Accepté. Le joueur du contexte est appliqué par UUID; un propriétaire statique n'est appliqué que s'il est déjà en ligne. Les textures Base64 et téléchargements de skin sont exclus de ce ticket.

## ADR-017 — Identification GUI par holder et identifiants de vue

Accepté. Chaque inventaire utilise `GuiInventoryHolder(sessionId, viewId, menuId)`, jamais le titre. `viewId` est renouvelé à chaque navigation, empêchant une fermeture retardée de supprimer la vue suivante.

## ADR-018 — Une session GUI active par joueur

Accepté. `GuiSession` est indexée par UUID joueur sans référence forte au joueur dans le cœur. Remplacement et nettoyage restent déterministes.

## ADR-019 — Framework GUI interne avant API publique

Accepté. Les modèles restent internes jusqu'à plusieurs usages métier réels; `bedwars-api` reste inchangé et aucune surface instable n'est figée.

## ADR-020 — Inventaires exclusivement sur le thread serveur

Accepté. Toute ouverture hors thread est replanifiée et les auto-refresh utilisent une tâche Bukkit centrale, jamais une tâche par bouton ou vue.

## ADR-021 — Navigation par historique limité

Accepté. Une deque bornée conserve des définitions `Gui` immuables; le remplacement sans historique est explicite. Retour et racine sont prédictibles, la mémoire bornée par `menus.yml`.

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

## ADR-032 à ADR-037 — Édition administrative des arènes

### Statut
Accepté.

### Décisions

- **ADR-032** — La saisie administrative utilise une session de chat unique par joueur. Le message est annulé de façon asynchrone puis traité sur le thread serveur; timeout, annulation, déconnexion et arrêt terminent la session.
- **ADR-033** — Chaque modification d'arène est sauvegardée automatiquement. L'état mémoire n'est publié qu'après réussite de la persistance.
- **ADR-034** — Une révision persistée, initialisée à 1 et incrémentée par mutation réussie, fournit un verrouillage optimiste. Une révision attendue obsolète produit `CONFLICT` sans écriture.
- **ADR-035** — Commandes et menus utilisent la même instance injectée d'`ArenaService` afin de conserver validation, sauvegarde et conflits identiques.
- **ADR-036** — Les menus ne lisent et n'écrivent jamais directement `arenas/*.yml`; `ArenaRepository` reste derrière le service.
- **ADR-037** — Les téléportations administratives exigent `heneriabedwars.admin.arena.teleport`; aucune téléportation n'est une conséquence implicite d'une sauvegarde.

### Conséquences
L'éditeur reste testable sans Bukkit pour ses règles principales et deux administrateurs ne peuvent pas écraser silencieusement leurs modifications. Les opérations création, suppression, activation, monde, positions, capacité et équipes sont journalisées simplement; un audit persistant reste futur.

## ADR-038 à ADR-044 — Cartes modèles autonomes

### Statut
Accepté.

### Décisions

- **ADR-038** — Une carte modèle possède un identifiant normalisé `[a-z0-9_-]{2,32}`; tous les chemins sont reconstruits depuis cet identifiant et confinés aux racines configurées.
- **ADR-039** — Bukkit charge les mondes modèles dans son conteneur de mondes avec le préfixe contrôlé `hbw_template_`. `maps/templates/<id>/managed-world.txt` matérialise la propriété du plugin sans prétendre que Bukkit peut charger un monde depuis le dossier de données du plugin.
- **ADR-040** — Métadonnées, dossier de monde et état chargé sont distincts. Les états transitoires ne sont pas restaurés après redémarrage; la présence réelle dans Bukkit détermine `LOADED` ou `UNLOADED`.
- **ADR-041** — Chargement, sauvegarde, déchargement et téléportation utilisent le thread serveur. Les copies et suppressions lourdes utilisent le scheduler asynchrone, avec un verrou par carte.
- **ADR-042** — Une suppression produit obligatoirement une sauvegarde complète avec manifeste avant toute suppression. Une carte liée à une arène active ou protégée comme lobby est refusée.
- **ADR-043** — Les arènes sont la source de vérité des associations; les liens inverses de carte sont des données dérivées et réparables.
- **ADR-044** — Le Ticket 007 réserve `instances/` mais ne crée aucune instance de partie. Les cartes modèles ne sont jamais modifiées automatiquement par un match.

### Conséquences
Les opérations restent sûres face aux traversées de chemin, liens symboliques, copies partielles et vues concurrentes. Une installation qui change le préfixe ou les dossiers doit redémarrer complètement et ne doit pas déplacer manuellement un monde chargé.

## ADR-045 — Navigation administrative déterministe

### Statut
Accepté.

### Décision
Les retours des menus métier ciblent explicitement leur parent logique au lieu de dépendre uniquement de l'historique de session. Après suppression réussie d'une arène, le GUI ouvre une nouvelle session dont le tableau de bord est la racine et supprime la révision observée de cette arène.

### Conséquences
Une vue ouverte directement par commande, devenue obsolète après reload ou issue d'une confirmation ne peut plus renvoyer vers un éditeur supprimé. Le bouton retour conserve le même sens visuel dans tout l'assistant : sous-menu vers éditeur, éditeur vers liste, liste vers configuration.
