# Changelog

Toutes les évolutions notables de HeneriaZombie sont consignées ici.

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
