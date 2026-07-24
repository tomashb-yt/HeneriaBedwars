# Changelog

Toutes les évolutions notables de HeneriaZombie sont consignées ici.

## 0.1.0-SNAPSHOT — Ticket 001

### Ajouté

- projet Gradle Java 21 en modules `zombie-api`, `zombie-core` et `zombie-plugin` ;
- manifeste Paper 1.21, permissions et commande `/zombie` ;
- configuration v1, messages MiniMessage et reload transactionnel ;
- cycle de vie avec rollback et registre de services explicitement possédé ;
- API publique de diagnostic et composition Paper ;
- tests unitaires, formatage, JAR déployable et documentation centrale complète.
- validation manuelle du chargement, des commandes et de l'arrêt sur Paper 1.21.11 build 132.

### Limites assumées

- aucun gameplay, lobby, éditeur, map ou système d'instance ;
- SQLite est ciblé et validé dans la configuration mais pas encore ouvert.
