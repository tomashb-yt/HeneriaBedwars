# IA des zombies

## Ciblage et navigation

`ZombieTargetSelector` applique d'abord les règles d'isolation, puis la stratégie configurée :
plus proche, santé la plus faible, points les plus élevés, aléatoire ou dernier attaquant. Le
moteur Paper construit les candidats uniquement depuis les joueurs connus de la partie.

Le comportement `MELEE` est complet : sélection échelonnée, cible native Paper, déplacement au
sol, ligne de vue, portée, cooldown et dégâts via le service central. Les dégâts vanilla des
ennemis enregistrés sont annulés afin d'éviter une seconde frappe.

## Blocage

Chaque définition configure intervalle, distance minimale, timeout, tentatives et secours. Une
position réellement déplacée remet le compteur à zéro. Après les tentatives de recalcul, le zombie
est téléporté à son spawn chargé ; sinon il est supprimé avec `DESPAWNED_STUCK` afin de ne jamais
bloquer la manche.

Les modifications d'entités restent exclusivement sur le thread serveur. Le chargement YAML est
asynchrone et publie ensuite un snapshot atomique.
