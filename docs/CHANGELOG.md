# Changelog

Toutes les Ã©volutions notables de HeneriaZombie sont consignÃ©es ici.

## 0.2.3-SNAPSHOT â€” Repli sÃ»r du lobby

### CorrigÃ©

- un refus de crÃ©ation de `zombie_lobby` par Paper ne dÃ©sactive plus le plugin lorsque le monde de
  repli configurÃ© est disponible ;
- le monde de lobby rÃ©ellement utilisÃ© est diagnostiquÃ© clairement au dÃ©marrage.

## 0.2.2-SNAPSHOT â€” CompatibilitÃ© Paper 1.21

### CorrigÃ©

- rÃ©solution compatible de l'attribut de vie maximale renommÃ© entre les premiÃ¨res versions Paper
  1.21 et les versions de maintenance rÃ©centes ;
- retour lobby et initialisation joueur fonctionnels sur Paper 1.21 build 130 ;
- compilation de la frontiÃ¨re Paper contre l'API minimale `1.21-R0.1-SNAPSHOT`.

## 0.2.1-SNAPSHOT â€” Correctif Ticket 002

### CorrigÃ©

- arguments de commandes comme `<id>` affichÃ©s littÃ©ralement sans Ãªtre interprÃ©tÃ©s par
  MiniMessage ;
- message d'usage invalide ne provoquant plus d'exception de commande ;
- crÃ©ation garantie de `zombie_templates` lors de la premiÃ¨re recherche ;
- diagnostic d'une map absente indiquant maintenant le chemin absolu attendu ;
- version du JAR distincte du Ticket 001.

## 0.1.0-SNAPSHOT â€” Ticket 002

### AjoutÃ©

- lobby central et profils joueur sÃ©parÃ©s ;
- modÃ¨le de map minimal versionnÃ© et catalogue asynchrone ;
- cycle complet des instances, registre concurrent et capacitÃ© configurable ;
- clonage, chargement, dÃ©chargement et nettoyage sÃ»r des mondes ;
- sessions exclusives et reconnexion avec expiration ;
- isolation de visibilitÃ©, tablist, chat, morts, audiences et scoreboards ;
- protections configurables des mondes et permission de bypass ;
- commandes temporaires lobby/instance et compteurs API rÃ©els ;
- tests du domaine, des erreurs de crÃ©ation, de la reconnexion et de l'isolation.
- validation Paper 1.21.11 du clonage, chargement, arrÃªt, dÃ©chargement, suppression et arrÃªt
  interrompu.

### SÃ©curitÃ© et exploitation

- aucune copie ou suppression volumineuse sur le thread Paper ;
- aucune suppression aprÃ¨s un dÃ©chargement non confirmÃ© ;
- reload refusÃ© pendant une instance active ;
- dossiers d'instance prÃ©servÃ©s pendant l'arrÃªt serveur.

### Limites assumÃ©es

- aucune manche, aucun zombie, aucun matchmaking et aucune GUI ;
- snapshots joueur non persistÃ©s aprÃ¨s un crash brutal ;
- manifeste de map limitÃ© Ã  la capacitÃ© et au spawn.

## 0.1.0-SNAPSHOT â€” Ticket 001

### AjoutÃ©

- projet Gradle Java 21 en modules `zombie-api`, `zombie-core` et `zombie-plugin` ;
- manifeste Paper 1.21, permissions et commande `/zombie` ;
- configuration v1, messages MiniMessage et reload transactionnel ;
- cycle de vie avec rollback et registre de services explicitement possÃ©dÃ© ;
- API publique de diagnostic, tests, formatage et JAR dÃ©ployable ;
- validation manuelle du dÃ©marrage et de l'arrÃªt sur Paper 1.21.11.
