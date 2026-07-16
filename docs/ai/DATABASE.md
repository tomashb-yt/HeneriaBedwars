# Base de données

Aucune base de données ni pilote JDBC n'est implémenté aux Tickets 001 à 009. La valeur `storage.type: sqlite` exprime uniquement le choix futur par défaut. Les métadonnées de cartes utilisent des YAML locaux et atomiques; elles ne constituent pas un stockage de statistiques ou de parties. Les joueurs, équipes, timers et statistiques du Ticket 009 restent strictement en mémoire pendant la vie d'une `GameInstance`.

La cible prévoit SQLite par défaut, MySQL/MariaDB, des migrations versionnées, un pool de connexions borné, des repositories, des requêtes asynchrones, un cache maîtrisé et éventuellement Redis. Aucune requête lente ne devra s'exécuter sur le thread Paper. Les détails de schéma et de cohérence seront décidés dans un ticket dédié.
