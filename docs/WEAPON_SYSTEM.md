# Système d'armes

**Statut :** moteur classique livré au Ticket 007.

## Architecture

`core.weapon` contient les définitions immuables, le registre, les instances runtime, les calculs
de dégâts et de dispersion, la sélection pondérée de la Mystery Box et les événements internes.
Il ne dépend pas de Paper. `plugin.weapon` charge les YAML hors thread serveur et adapte les items,
le hitscan, les interactions de map, les sons, les commandes et les GUI.

Une arme ne retire jamais directement un zombie. Le chemin obligatoire est :

```text
PaperWeaponService
  -> WeaponDamageCalculator
  -> PaperZombieEngine.damageFromWeapon
  -> ZombieDamageService
  -> retrait/mort idempotente et récompense
```

## Exécution

Chaque instance d'arme possède son chargeur, sa réserve, sa cadence, son rechargement, son niveau
Pack-a-Punch et ses statistiques. Les rafales, charges et rechargements sont traités dans le tick
groupé de `PaperGameRuntime`; aucune tâche Paper n'est créée par arme ou joueur. Les index par UUID,
joueur et partie permettent un nettoyage complet à la fin de l'instance.

Les modes `SEMI_AUTOMATIC`, `AUTOMATIC`, `BURST`, `CHARGE` et `MELEE` partagent le même pipeline.
Le hitscan tient compte des blocs, matériaux pénétrables, cibles successives, distance, dispersion,
recul, multiplicateur de headshot et améliorations.

## Stations de map

- `WEAPON_WALL` achète l'arme définie par `weapon-id`, puis ses munitions si elle est déjà possédée ;
- `MYSTERY_BOX` sélectionne une définition pondérée, avec blacklist et contrôle des Wonder Weapons ;
- `PACK_A_PUNCH` applique le prochain niveau déclaré par l'arme et son coût.

L'éditeur place une arme murale initiale `starter_pistol`. Les propriétés persistées sont
`weapon-id`, `cost` et `ammo-cost`. Les Mystery Box et Pack-a-Punch existants deviennent
interactifs pendant une manche active.

Chaque station exécute une animation dans le tick groupé, sans tâche dédiée. La Mystery Box fait
défiler des `ItemDisplay` pendant cinq secondes avant de débiter puis attribuer l'arme finale. Le
Pack-a-Punch retire temporairement l'arme tenue, l'affiche en rotation, effectue l'achat à la fin
et restitue toujours l'arme, améliorée uniquement si le paiement aboutit. Une déconnexion avant la
fin ne débite rien. `animation-ticks` permet de modifier la durée par objet de map.

## Contenu livré

Les exemples `starter_pistol`, `ak47`, `mp5`, `pump_shotgun` et `raygun` couvrent pistolet,
fusil d'assaut, SMG, fusil à pompe et Wonder Weapon. Ajouter une arme ne nécessite aucun code :
copier un YAML valide dans `plugins/HeneriaZombie/weapons`, puis exécuter `/zweapon reload`.

Les armes déjà distribuées conservent leur snapshot après reload. Les nouvelles distributions
utilisent le registre validé le plus récent.

Le listener de tir observe aussi les interactions pré-annulées par Paper : certains matériaux
utilisés comme modèles d'armes, notamment l'armure de cheval, n'ont aucune action vanilla et
produisent malgré tout un événement déjà annulé. Une arme reconnue reste la seule condition qui
autorise alors le pipeline de tir.

## Intégration économique

`PaperWeaponService` ne possède plus de passerelle booléenne de points. Un impact appelle
`RewardService`. Les achats muraux, de munitions et Mystery Box, ainsi que les améliorations
Pack-a-Punch, construisent un `PurchaseRequest`. Leur clé inclut partie, joueur, objet et tick afin
de neutraliser un double événement sans bloquer une interaction ultérieure.

Max Ammo appelle `refillGame` et Insta-Kill modifie la valeur envoyée au moteur de dégâts, jamais
l'entité directement.
