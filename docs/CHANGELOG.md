# Changelog

Toutes les évolutions notables de HeneriaZombie sont consignées ici.

## 0.6.2-SNAPSHOT — Spawn éditorial et zombies diurnes

### Corrigé

- le lancement place les joueurs au spawn configuré par l'éditeur dans le monde cloné ;
- les arrivées en cours et reconnexions réutilisent ce spawn éditorial ;
- les zombies temporaires du système de manches ne brûlent plus au soleil.

### Précisé

- portes, barricades, Pack-a-Punch et objets similaires restent des données éditoriales jusqu'au
  ticket d'économie de map.

## 0.6.1-SNAPSHOT — Compatibilité des anciennes GUI

### Corrigé

- fusion récursive des nouveaux menus dans un ancien `guis.yml` ;
- les menus d'éditeur ne perdent plus thème, slots et matériaux pendant cette migration ;
- les personnalisations déjà présentes restent prioritaires et le fichier utilisateur n'est pas
  réécrit.

## 0.6.0-SNAPSHOT — Boucle de jeu et manches

### Ajouté

- agrégats métier isolés `ZombieGame`, `RoundState` et `GamePlayer` ;
- difficulté configurable, budgets d'apparition et zombies vanilla temporaires ;
- compte à rebours, points, mise à terre, réanimation, reconnexion et défaite ;
- scoreboard, résultats asynchrones, nettoyage et commande `/zgame` ;
- démarrage automatique avec `/zmap test` et tests du cycle complet.

### Modifié

- version portée à `0.6.0-SNAPSHOT` ;
- configuration enrichie des sections `game`, `players` et `rounds`.

## 0.5.3-SNAPSHOT — Instance de test et sécurité d'inventaire

### Ajouté

- `/zmap test` valide la map puis crée et rejoint une instance privée du modèle homonyme.

### Corrigé

- les items Paper sans métadonnées ne provoquent plus de `NullPointerException` pendant un clic
  d'inventaire ;
- une entrée refusée dans l'instance de test déclenche son nettoyage.

## 0.5.2-SNAPSHOT — Placement et ouverture sans conflit

### Corrigé

- un clic droit dans l'air ou accroupi + clic droit rouvre désormais le menu éditeur ;
- un clic gauche ou droit sur un bloc place l'élément lorsqu'une action est sélectionnée ;
- sans action sélectionnée, le clic droit sur un bloc ouvre également le menu ;
- le lore de l'outil explique les interactions sans ambiguïté.

## 0.5.1-SNAPSHOT — Réouverture du menu éditeur

### Corrigé

- l'outil permet de rouvrir le menu sans relancer `/zmap edit` ;
- le lore présente les commandes de l'outil.

## 0.5.0-SNAPSHOT — Éditeur universel de maps

### Ajouté

- schéma éditorial v2 immuable, registre et collections extensibles ;
- commandes `/zmap`, sessions exclusives, outil protégé, sélection et presse-papiers ;
- GUI de toutes les catégories, placement, déplacement, duplication et suppression confirmée ;
- informations générales configurables par saisie privée ;
- auto-save YAML asynchrone sérialisé, remplacement atomique et backup ;
- historique undo/redo et validation structurelle avec graphe de zones ;
- tests du service, du validateur et du cycle complet de persistance.

### Sécurité et limites

- aucun accès disque sur le thread Paper et aucun type Bukkit dans le core ;
- nettoyage de session et d'outil à la sortie, déconnexion et arrêt ;
- l'activation d'une définition dans une partie attend le ticket de boucle de jeu ;
- les interactions visuelles doivent encore être validées manuellement dans Minecraft.

## 0.4.0-SNAPSHOT — Framework GUI configurable

### Ajouté

- moteur central et registres extensibles d'écrans et d'actions ;
- sessions isolées, retour/accueil, recherche privée et pagination sans plafond ;
- thèmes, menus, matériaux, textes, slots, permissions, sons et actions dans `guis.yml` ;
- validation agrégée et activation atomique asynchrone ;
- menus joueur, administration, maps, instances, diagnostics et confirmation ;
- aperçu/création de map, entrée d'instance et arrêt confirmé ;
- expiration et rafraîchissement périodique partagé ;
- tests de pagination, permissions, historique, confirmation, saisie et YAML invalide.

### Sécurité et limites

- les interactions d'inventaire ne permettent pas de récupérer les items ;
- joueur et jeton de vue sont vérifiés avant chaque callback ;
- éditeurs métier, profils, groupes, statistiques, export et suppression restent indisponibles ;
- la validation visuelle multi-clients reste manuelle.

## 0.3.0-SNAPSHOT — Import simple et aperçu des maps

### Ajouté

- détection automatique d'un monde grâce à son seul `level.dat`, sans YAML obligatoire ;
- lecture bornée et asynchrone du spawn vanilla NBT ;
- `/zombie map list`, `/zombie map preview <mapId>` et `/zombie map leave` ;
- copies d'aperçu isolées du registre des parties et nettoyage forcé après déchargement ;
- capacité par défaut configurable pour les maps sans manifeste.

### Sécurité

- le monde modèle n'est jamais chargé ni modifié directement ;
- un seul aperçu par administrateur et transition concurrente refusée ;
- déconnexion, retour lobby et changement de contexte libèrent la copie.

## 0.2.3-SNAPSHOT — Repli sûr du lobby

### Corrigé

- un refus de création de `zombie_lobby` par Paper ne désactive plus le plugin lorsque le monde de
  repli configuré est disponible ;
- le monde de lobby réellement utilisé est diagnostiqué clairement au démarrage.

## 0.2.2-SNAPSHOT — Compatibilité Paper 1.21

### Corrigé

- résolution compatible de l'attribut de vie maximale renommé entre les premières versions Paper
  1.21 et les versions de maintenance récentes ;
- retour lobby et initialisation joueur fonctionnels sur Paper 1.21 build 130 ;
- compilation de la frontière Paper contre l'API minimale `1.21-R0.1-SNAPSHOT`.

## 0.2.1-SNAPSHOT — Correctif Ticket 002

### Corrigé

- arguments de commandes comme `<id>` affichés littéralement sans être interprétés par
  MiniMessage ;
- message d'usage invalide ne provoquant plus d'exception de commande ;
- création garantie de `zombie_templates` lors de la première recherche ;
- diagnostic d'une map absente indiquant maintenant le chemin absolu attendu ;
- version du JAR distincte du Ticket 001.

## 0.1.0-SNAPSHOT — Ticket 002

### Ajouté

- lobby central et profils joueur séparés ;
- modèle de map minimal versionné et catalogue asynchrone ;
- cycle complet des instances, registre concurrent et capacité configurable ;
- clonage, chargement, déchargement et nettoyage sûr des mondes ;
- sessions exclusives et reconnexion avec expiration ;
- isolation de visibilité, tablist, chat, morts, audiences et scoreboards ;
- protections configurables des mondes et permission de bypass ;
- commandes temporaires lobby/instance et compteurs API réels ;
- tests du domaine, des erreurs de création, de la reconnexion et de l'isolation.
- validation Paper 1.21.11 du clonage, chargement, arrêt, déchargement, suppression et arrêt
  interrompu.

### Sécurité et exploitation

- aucune copie ou suppression volumineuse sur le thread Paper ;
- aucune suppression après un déchargement non confirmé ;
- reload refusé pendant une instance active ;
- dossiers d'instance préservés pendant l'arrêt serveur.

### Limites assumées

- aucune manche, aucun zombie, aucun matchmaking et aucune GUI ;
- snapshots joueur non persistés après un crash brutal ;
- manifeste de map limité à la capacité et au spawn.

## 0.1.0-SNAPSHOT — Ticket 001

### Ajouté

- projet Gradle Java 21 en modules `zombie-api`, `zombie-core` et `zombie-plugin` ;
- manifeste Paper 1.21, permissions et commande `/zombie` ;
- configuration v1, messages MiniMessage et reload transactionnel ;
- cycle de vie avec rollback et registre de services explicitement possédé ;
- API publique de diagnostic, tests, formatage et JAR déployable ;
- validation manuelle du démarrage et de l'arrêt sur Paper 1.21.11.
