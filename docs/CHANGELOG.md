# Changelog

Toutes les évolutions notables de HeneriaZombie sont consignées ici.

## 0.10.5-SNAPSHOT — Visite avant configuration

### Ajouté

- les dossiers valides de `zombie_templates/` apparaissent dans le gestionnaire administratif,
  même lorsqu'aucune définition éditoriale n'existe encore ;
- une fiche dédiée indique clairement l'état `template importé` et permet de visiter immédiatement
  une copie privée sans créer de map, de partie ou de fichier `map.yml`.

### Modifié

- le catalogue des templates est actualisé à chaque ouverture du gestionnaire ;
- les templates configurés et non configurés partagent désormais la même recherche et pagination ;
- version portée à `0.10.5-SNAPSHOT`.

## 0.10.4-SNAPSHOT — Transition visite, édition et test

### Corrigé

- le bouton `Modifier la map` ferme et détruit désormais la copie de visite avant de charger le
  monde de travail ;
- la téléportation finale vers l'éditeur ne peut plus être écrasée par le retour au lobby lié au
  nettoyage de l'aperçu ;
- le lancement d'un test ferme également tout aperçu actif avant de créer l'instance privée ;
- les anciennes sessions de partie sont quittées explicitement avant l'entrée dans un test.

### Modifié

- version portée à `0.10.4-SNAPSHOT`.

## 0.10.3-SNAPSHOT — Tableau de bord et visite des maps

### Ajouté

- bouton `Visiter la map` ouvrant une copie administrative isolée du template ;
- téléportation au spawn du monde sans création de partie ni modification du template ;
- permission dédiée `zombies.admin.maps.visit` ;
- informations synthétiques : état, monde de travail, disponibilité du template, validation,
  version, zones, spawns et objets.

### Modifié

- tableau de bord agrandi à 54 slots et organisé en trois étapes visuelles ;
- ordre explicite : visiter, modifier, dupliquer, vérifier, tester puis publier ;
- descriptions complètes distinguant visite, édition, test, publication, archivage et suppression ;
- schéma GUI porté à `4` avec migration ciblée du menu de détail ;
- version portée à `0.10.3-SNAPSHOT`.

## 0.10.2-SNAPSHOT — Accès au gestionnaire de maps

### Corrigé

- migration de l'ancien bouton administratif `Gestion des maps` de `nav.maps` vers `maps.admin` ;
- accès restauré au détail contenant modification, validation, test, publication, archivage et
  suppression ;
- schéma GUI porté à `3`.

## 0.10.1-SNAPSHOT — Migration GUI et suppression complète

### Corrigé

- migration automatique du premier schéma `guis.yml` avant fusion des nouvelles valeurs ;
- suppression des anciens boutons `join`, `group` et `profile` lorsqu'ils utilisent encore leurs
  actions embarquées ;
- remplacement de l'ancienne action du bouton `play` par le catalogue des maps publiées ;
- disparition de la collision entre `group` et `leave` au slot 24.

### Ajouté

- bouton de suppression définitive dans le détail administratif d'une map ;
- confirmation explicite et refus lorsqu'une instance ou une session d'édition utilise la map ;
- suppression asynchrone de `map.yml`, backups, spawns, objets, publication, versions et snapshots ;
- suppression du modèle `zombie_templates/<mapId>` et du monde d'édition uniquement lorsque ce
  dernier appartient explicitement au plugin ;
- conservation garantie des mondes serveur externes associés à une map.

### Modifié

- version portée à `0.10.1-SNAPSHOT`.

## 0.10.0-SNAPSHOT — Menus et publication des maps

### Ajouté

- `/zombies` ouvre le menu joueur et `/zombies admin` l'espace administratif ;
- catalogue des seules maps publiées avec informations, état et occupation en temps réel ;
- entrée automatique dans une instance publique disponible ou création d'une nouvelle partie ;
- gestion GUI des maps : création, édition, validation, test, publication et dépublication ;
- duplication complète de la configuration et du monde d'édition, archivage sans perte ;
- joueurs minimum/maximum configurables et attente jusqu'au seuil de démarrage ;
- validation paginée avec détail, solution et téléportation lorsqu'une position existe ;
- statuts explicites, versions immuables, historique durable et restauration confirmée ;
- permissions séparées `zombies.menu.*`, `zombies.admin.maps.*`, parties et diagnostics.

### Sécurité

- copie de travail distincte de la version publiée ;
- snapshot physique des blocs par version et clonage public depuis ce snapshot ;
- publication activée seulement après validation, contrôle du modèle et écriture réussie ;
- sérialisation des publications concurrentes par map ;
- verrou exclusif d'édition ;
- parties actives conservant le snapshot capturé à leur démarrage.
- démarrage du JAR validé sur Paper 1.21.11 build 132 avec Java 21.

### Modifié

- les commandes techniques restent disponibles pour debug, console et secours ;
- version portée à `0.10.0-SNAPSHOT`.

## 0.9.2-SNAPSHOT — Stabilisation des interactions

### Corrigé

- un joueur sans allié vivant est éliminé immédiatement au lieu d'attendre une réanimation
  impossible ;
- les joueurs à terre utilisent une pose basse fixe et ne peuvent plus se déplacer, tirer,
  interagir, déplacer leur inventaire ou ramasser un objet ;
- les coups de zombies produisent animation de blessure, son, particules, actionbar et recul bref ;
- les bonus recherchent un sol sûr à proximité et restent ancrés sans gravité ni vélocité.

### Ajouté

- animation Mystery Box de cinq secondes avec défilement d'armes avant attribution et paiement ;
- animation Pack-a-Punch de cinq secondes avec arme visible et restitution après traitement ;
- hologrammes `Mystery Box` et `Pack-a-Punch` dans le monde isolé de chaque instance.

### Modifié

- durée des animations de station configurable par propriété `animation-ticks`, valeur par défaut
  `100` ;
- version portée à `0.9.2-SNAPSHOT`.

## 0.9.1-SNAPSHOT — Stabilisation gameplay et édition

### Corrigé

- les armes traitent les clics droits pré-annulés par Paper et tirent avec les items sans action
  vanilla ;
- un contact natif de zombie déclenche désormais exactement une frappe du moteur de jeu ;
- les zombies sont explicitement conscients et agressifs, et leur équipement vanilla aléatoire
  est supprimé.

### Ajouté

- repères lumineux et libellés pour le spawn joueur, les spawns zombies, portes et barricades ;
- coffre visible pour la Mystery Box et coffre de l'Ender visible pour le Pack-a-Punch ;
- matérialisation automatique de ces stations dans chaque monde de partie jetable.

### Modifié

- version portée à `0.9.1-SNAPSHOT`.

## 0.9.0-SNAPSHOT — Ticket 008

### Ajouté

- portefeuilles `long` isolés, transactions immuables, journal borné et idempotence ;
- achats atomiques, résolution de prix et remboursements complets ou partiels ;
- récompenses d'impact, mort, assistance et réanimation avec anti-farm ;
- Double Points, Max Ammo, Insta-Kill, Nuke et drops configurables ;
- commande `/zeconomy`, feedback groupé et scoreboard économique ;
- agrégats financiers de fin de partie et tests du domaine.
- démarrage du JAR validé sur Paper 1.21.11 build 132 avec Java 21.

### Modifié

- armes murales, Mystery Box et Pack-a-Punch utilisent `PurchaseService` ;
- le moteur de jeu ne stocke et ne modifie plus directement les points ;
- version portée à `0.9.0-SNAPSHOT`.

## 0.8.0-SNAPSHOT — Ticket 007

### Ajouté

- moteur d'armes data-driven, munitions, cadence, rechargement, dispersion, recul et pénétration ;
- dégâts balistiques reliés au moteur de zombies sans suppression directe d'entité ;
- armes murales, Mystery Box pondérée et Pack-a-Punch multi-niveaux ;
- cinq définitions YAML installées et recharge atomique asynchrone ;
- items PDC, statistiques par arme, sons, modèles, commande `/zweapon` et GUI de catalogue ;
- événements internes annulables, placement d'armes murales et tests du domaine.

### Modifié

- les joueurs reçoivent `starter_pistol` au lancement et après une arrivée en partie ;
- version portée à `0.8.0-SNAPSHOT`.

## 0.7.0-SNAPSHOT — Ticket 006

### Ajouté

- quatre définitions YAML versionnées et recharge atomique asynchrone ;
- registre immuable, attributs par manche, sélection pondérée et suivi O(1) ;
- IA de mêlée isolée, navigation terrestre Paper et secours anti-blocage ;
- dégâts typés, immunités, résistances, vulnérabilités et tirs à la tête explicites ;
- mort idempotente, récompenses uniques et nettoyage complet par partie ;
- capacités `poison_hit` et `explode_on_death` dans un registre extensible ;
- protections PDC, commandes `/zzombie`, événements internes et tests du domaine.

### Modifié

- suppression de `PaperZombieSpawner`, l'adaptateur temporaire du Ticket 005 ;
- version portée à `0.7.0-SNAPSHOT`.

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
