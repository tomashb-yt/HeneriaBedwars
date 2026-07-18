# Système de générateurs

## Phase 1 — cœur pur

Une `GeneratorDefinition` est un snapshot immuable contenant identifiant, ressource, position runtime, niveau, intervalle, quantité par émission, capacité locale et stratégie d'empilement. `RuntimeGenerator` ajoute seulement la prochaine échéance et des compteurs runtime; ces données ne doivent jamais être écrites dans les YAML administratifs.

`GameGeneratorService` reçoit la collection des parties depuis une unique boucle de plateforme. Seules les instances `PLAYING` avec un monde attaché sont parcourues. Le budget d'émissions est global et le point de départ tourne entre les passages afin de préserver l'équité.

Une échéance due avance directement jusqu'à la première date future. Même après une longue pause, un générateur ne produit donc qu'une seule émission. `GeneratorCapacityView` indique combien d'items compatibles sont déjà proches; l'émission est réduite à la place restante ou bloquée lorsque la capacité est atteinte.

## Phase 2 — administration et drops Bukkit

`ArenaGeneratorDefinition` persiste l'identifiant, la ressource, la position dans le monde modèle, le niveau, l'intervalle, la quantité, la capacité et l'empilement sous `generators.definitions` dans chaque fichier d'arène. L'assistant permet d'ajouter les quatre ressources à la position actuelle, de visiter un point, de le déplacer avec Shift+gauche et de le supprimer avec confirmation.

L'unicité porte sur le couple ressource + bloc. Deux générateurs de fer ne peuvent pas occuper le même bloc, mais du fer et de l'or peuvent volontairement partager exactement le même point comme sur les cartes BedWars classiques. Le menu v8 sépare le guide, les quatre actions de placement et la liste des points; chaque entrée indique combien de minerais partagent son bloc.

Quand le clone atteint `WAITING`, ses générateurs runtime sont enregistrés une seule fois. Le ticker global appelle ensuite le service chaque tick; seules les parties `PLAYING` produisent. L'adaptateur Bukkit remplace le monde modèle par le clone, fusionne les piles compatibles proches et crée les objets restants par piles valides.

À l'entrée en `PLAYING`, `GeneratorPacingPolicy` recalcule l'intervalle depuis le nombre d'équipes configurées et de joueurs présents. Le facteur reste entre `0.85` et `1.60`; la première échéance repart de cet instant, donc un long lobby ne déclenche pas de drop immédiat.

Chaque drop appartient à une partie et un générateur via PDC. Il apparaît au centre du bloc, sans gravité ni vitesse, et le ticker partagé le replace si un effet externe le déplace. Les objets jetés par les joueurs ne portent pas ces PDC et ne sont ni comptés, ni fusionnés, ni immobilisés. Les générateurs diamant et émeraude possèdent un `TextDisplay` indiquant leur niveau et le temps restant calculé depuis l'échéance réelle.

Les valeurs par défaut viennent de `generators.yml`. Modifier ces valeurs affecte les nouveaux générateurs; ceux déjà enregistrés conservent leur snapshot dans le YAML de l'arène.

## Phase suivante

Les particules, sons, niveaux automatiques et une éventuelle pagination au-delà de quatorze points restent à ajouter sans modifier le moteur d'échéances.
