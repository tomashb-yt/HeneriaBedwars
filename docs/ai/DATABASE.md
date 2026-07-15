# Base de données

Aucune base de données ni pilote JDBC n'est implémenté au Ticket 001. La valeur `storage.type: sqlite` exprime uniquement le choix futur par défaut.

La cible prévoit SQLite par défaut, MySQL/MariaDB, des migrations versionnées, un pool de connexions borné, des repositories, des requêtes asynchrones, un cache maîtrisé et éventuellement Redis. Aucune requête lente ne devra s'exécuter sur le thread Paper. Les détails de schéma et de cohérence seront décidés dans un ticket dédié.
