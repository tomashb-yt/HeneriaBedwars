# Système de générateurs

## Phase 1 — cœur pur

Une `GeneratorDefinition` est un snapshot immuable contenant identifiant, ressource, position runtime, niveau, intervalle, quantité par émission, capacité locale et stratégie d'empilement. `RuntimeGenerator` ajoute seulement la prochaine échéance et des compteurs runtime; ces données ne doivent jamais être écrites dans les YAML administratifs.

`GameGeneratorService` reçoit la collection des parties depuis une unique boucle de plateforme. Seules les instances `PLAYING` avec un monde attaché sont parcourues. Le budget d'émissions est global et le point de départ tourne entre les passages afin de préserver l'équité.

Une échéance due avance directement jusqu'à la première date future. Même après une longue pause, un générateur ne produit donc qu'une seule émission. `GeneratorCapacityView` indique combien d'items compatibles sont déjà proches; l'émission est réduite à la place restante ou bloquée lorsque la capacité est atteinte.

## Phase suivante

La prochaine phase doit ajouter les définitions administratives et leur persistance, l'assistant GUI, le remappage modèle vers clone, puis un adaptateur Bukkit qui fusionne ou crée les items. Le ticker existant appellera le service; aucune tâche par générateur ne sera créée.
