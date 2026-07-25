# Changelog

Toutes les évolutions notables de HeneriaZombie sont consignées ici.

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
