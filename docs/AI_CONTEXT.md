# Instructions obligatoires pour toute IA

Avant de modifier le projet :

1. lire ce fichier intégralement ;
2. lire `ARCHITECTURE.md`, `DECISIONS.md` et `ROADMAP.md` ;
3. lire les documents du système modifié et les dernières entrées du changelog ;
4. inspecter Git et le code réel ;
5. synchroniser documentation, tests et code avant de terminer.

Le dépôt est la source de vérité. Une conversation externe ne l'est jamais.

## Vision

HeneriaZombie est un mini-jeu Zombies haut de gamme exécuté sur un seul serveur Paper. Un lobby
central et plusieurs parties totalement isolées coexistent dans le même processus. Le projet
reprend les principes familiers du genre sans reproduire de contenu protégé et garde ses
mécaniques originales optionnelles par map.

## Conventions techniques

- Java 21, Gradle Kotlin DSL et Paper 1.21.x ;
- Adventure pour les textes et audiences ;
- YAML versionné pour la configuration lisible ;
- `zombie-api` sans plateforme, `zombie-core` sans Paper et `zombie-plugin` comme frontière Paper ;
- injection explicite par constructeur, sans singleton global mutable ;
- fichiers hors thread serveur, appels Bukkit sur le thread serveur ;
- snapshots de configuration validés puis activés atomiquement.

## État réel — Tickets 004 à 006

Le socle Ticket 001 reste opérationnel. Le Ticket 002 ajoute :

- un lobby central chargé au démarrage et un état joueur de lobby standardisé ;
- des modèles de monde minimaux identifiés par `zombie-map.yml` ;
- des instances simultanées à identifiant UUID, capacité par map et cycle contrôlé ;
- copie asynchrone, chargement Paper, déchargement puis suppression sûre des mondes ;
- sessions joueur exclusives, capture/restauration d'état et reconnexion avec délai ;
- isolation de visibilité, tablist, chat, messages de mort et audiences Adventure ;
- scoreboards indépendants par contexte ;
- protections configurables des mondes d'instance ;
- commandes temporaires de création, inspection, entrée, sortie et arrêt ;
- détection automatique des dossiers de monde et aperçus administratifs isolés sans partie ;
- compteurs API réels pour les modèles et instances.

Le Ticket 004 ajoute un schéma éditorial v2, un registre asynchrone, des sessions d'administration,
les commandes `/zmap`, l'outil protégé, les GUI de placement et un validateur structurel. Les
définitions sont auto-sauvegardées sous `plugins/HeneriaZombie/maps`.

Le correctif 0.5.3 a relié une définition valide à une instance privée de test. Depuis le Ticket
005, cette instance démarre également la boucle de gameplay minimale.

Les états techniques d'instance sont `CREATING`, `WAITING`, `STARTING`, `RUNNING`, `ENDING`,
`CLEANING`, `CLOSED` et `ERROR`. Les états métier de partie sont documentés dans
`GAME_LIFECYCLE.md`. Le matchmaking reste à implémenter.

## Modèle de map minimal du Ticket 002

Le catalogue détecte tout dossier
`<world-container>/zombie_templates/<mapId>/` contenant `level.dat`. Le spawn vanilla est lu hors
thread serveur. `zombie-map.yml` devient une surcharge facultative de capacité et de spawn. Une
visite administrative charge toujours une copie temporaire : le modèle source n'est jamais chargé
ni modifié directement.

## Stratégie joueur

À la première prise en charge, le plugin capture l'état pré-plugin en mémoire. Le lobby reçoit un
profil vide et standardisé. L'entrée en instance capture ce profil lobby, applique un profil de
partie propre et téléporte au spawn du modèle. La sortie restaure le profil lobby.

Lors d'un arrêt normal, les joueurs sont renvoyés au lobby avant le déchargement des mondes. Les
snapshots ne sont pas persistés après un crash brutal du processus ; le serveur conserve toutefois
son propre `playerdata`. Une persistance transactionnelle dédiée sera nécessaire avant
l'introduction d'inventaires de valeur.

## Limites connues

- le catalogue de clonage utilise encore son manifeste technique minimal distinct de l'éditeur ;
- l'ajout simple accepte un dossier de monde, mais pas encore une archive ZIP ;
- une instance créée manuellement reste en attente jusqu'à `/zgame start`; `/zmap test` démarre
  automatiquement sa boucle de jeu ;
- SQLite est configuré mais aucune donnée métier ne justifie encore l'ouverture d'une connexion ;
- le test automatisé simule plusieurs sessions, mais une validation visuelle complète exige trois
  clients Minecraft ;
- un arrêt serveur conserve les dossiers d'instance, car leur suppression serait incertaine ;
- `-1` retire le plafond fonctionnel, sans supprimer les limites matérielles.

## Interfaces livrées aux Tickets 003–004

Le moteur GUI configurable fournit des sessions isolées, navigation, pagination sans plafond,
recherche privée par chat, permissions, confirmations, rafraîchissement partagé et activation
atomique de `guis.yml`. Les menus disponibles sont joueur, administration, maps, instances,
diagnostics et confirmation. Ils réutilisent les services du Ticket 002.

L'éditeur configure les informations, zones, portes, spawns et objets sans YAML manuel. Groupe,
profil, statistiques et export restent désactivés. Le rendu doit encore être validé manuellement.

## Menus et publication 0.10.0

`/zombies` est désormais l'entrée joueur : seules les versions au statut `PUBLISHED` sont
affichées et un clic rejoint une instance publique ou en crée une. `/zombies admin` ouvre la
gestion centrale. La gestion des maps permet création, édition verrouillée, validation, test,
publication, dépublication, historique et restauration.

`MapPublicationService` est dans le core. Il persiste via `YamlMapPublicationPersistence` des
snapshots immuables numérotés. `map.yml` reste la copie de travail. `PaperGameRuntime` utilise la
publication pour une instance publique et la copie de travail pour un test privé. Une partie
capture toujours sa définition.

Voir `MAP_PUBLICATION.md`. Les commandes `/zmap`, `/zgame`, `/zzombie`, `/zweapon` et `/zeconomy`
restent les surfaces avancées. Les statistiques persistantes, groupes, files configurables et
options de simulation qui dépendent des futurs moteurs ne doivent pas être présentés comme actifs.

## Stabilisation 0.10.1

Le schéma GUI courant vaut `2`. Au chargement d'un ancien `guis.yml`, les boutons joueur
embarqués devenus obsolètes sont retirés en mémoire avant la fusion des valeurs manquantes ; cela
évite notamment la collision du slot 24 entre `group` et `leave`.

Le détail administratif propose une suppression irréversible distincte de l'archivage. Elle est
refusée tant qu'une instance ou un éditeur utilise la map, puis supprime hors thread serveur toute
la définition (spawns et objets inclus), l'historique publié, les snapshots et le modèle. Seul un
monde d'édition au chemin possédé `zombie_editing/hz_edit_<mapId>` peut être supprimé ; un monde
serveur externe n'est jamais effacé.

Le correctif `0.10.2` porte le schéma GUI à `3` et migre également l'ancien bouton
`menus.admin-main.buttons.maps` de `nav.maps` vers `maps.admin`. Sans cette redirection, un ancien
fichier ouvrait le sélecteur technique au lieu du gestionnaire donnant accès au test, à la
publication et à la suppression.

## Reprise

Les Tickets 001 à 007 sont terminés dans le code. Une map validée se teste avec
`/zmap edit <map>`, puis `/zmap test`. `/zgame` diagnostique le cycle de partie et `/zzombie`
diagnostique les définitions et ennemis actifs.

La boucle comprend les manches FORMULA, plafonds vivants, types pondérés, IA de mêlée, points,
mise à terre, réanimation, reconnexion, défaite et nettoyage. Le Ticket 007 ajoute armes,
munitions, achats muraux, Mystery Box et Pack-a-Punch. Portes, barricades, courant, atouts et
persistance des résultats restent à livrer. Le `GameResultRepository` actif est toujours sans
stockage. Les
scénarios multi-clients et mesures 25/50/100/200 ennemis restent à valider sur un serveur Paper.

Le spawn joueur éditorial est appliqué au lancement, à une arrivée et à une reconnexion. Portes,
barricades, atouts et pièges restent configurés et validés sans gameplay actif.

## Ticket 006

Le moteur spécialisé est livré dans `core.enemy` et `plugin.enemy`. Les définitions, attributs,
sélection, dégâts, ciblage, états, suivi et événements internes restent sans Paper. La fabrique,
l'IA native, les protections et `/zzombie` sont dans l'adaptateur Paper. Aucun NMS n'est utilisé.

Les types inclus sont `classic_zombie`, `sprinter_zombie`, `armored_zombie` et `toxic_zombie`.
Le comportement complet est `MELEE`/`GROUND`; `poison_hit` et `explode_on_death` valident le
registre de capacités. Vol, distance, portes et barricades restent des extensions préparées.

## Ticket 007

Le moteur data-driven réside dans `core.weapon` et `plugin.weapon`. Cinq armes YAML sont livrées.
Les joueurs commencent avec `starter_pistol`; clic droit tire, `F` ou la tentative de jet recharge.
Les objets `WEAPON_WALL`, `MYSTERY_BOX` et `PACK_A_PUNCH` débitent les points et opèrent pendant
une manche. `/zweapon` et le catalogue GUI fournissent diagnostic, distribution de test et reload.
Voir `WEAPON_SYSTEM.md`.

Le prochain ticket recommandé est l'économie complète : portes, achats partagés et transactions.

## État après le Ticket 008

Le Ticket 008 est terminé. Les classes centrales ajoutées sont `EconomyService`, `GameEconomy`,
`PlayerWallet`, `TransactionService`, `PurchaseService`, `PriceResolver`, `RewardService`,
`PowerUpService` et `PowerUpDropService`. Les adaptateurs Paper sont `PaperPowerUpService`,
`PointDisplayService` et `ZEconomyCommand`.

Les armes/recharges murales, la Mystery Box et le Pack-a-Punch utilisent réellement
`PurchaseService`. Les récompenses d'impact, élimination, assistance et réanimation utilisent
`RewardService`. Double Points, Max Ammo, Insta-Kill et Nuke sont jouables ; chance, plafonds,
cooldowns et durées sont configurables.

L'idempotence conserve un `operationId` pendant toute la partie. Paper compose partie, joueur,
cible/objet et tick ; une commande administrative utilise sa séquence d'exécution. Un échec
d'attribution après débit déclenche un remboursement qui référence le débit et ne peut dépasser
son montant.

Limites connues : seul le financement individuel est actif ; les portefeuilles d'équipe et
contributions sont préparés par les enums mais non branchés. Les événements sont internes et
synchrones. Les transactions détaillées ne sont pas persistées ; les agrégats de fin de partie le
sont via `GameResultRepository`. Les tests avec un véritable client Paper restent manuels.

Commandes utiles : `/zeconomy balance <joueur>`, `/zeconomy history <joueur>`,
`/zeconomy givepowerup <type> [partie]` et `/zeconomy debug <partie>`.

## Stabilisation 0.9.1

`WeaponListener` accepte les clics droits que Paper marque déjà comme annulés pour les matériaux
sans usage vanilla. `MapVisualizationService` possède les repères d'édition et restaure les blocs
remplacés à la fermeture ; les mondes de partie étant jetables, il y matérialise directement les
stations Mystery Box et Pack-a-Punch.

Le moteur Paper force les mobs à être conscients et agressifs. Les contacts natifs sont annulés
puis traduits vers `PaperZombieEngine.attackPlayer`, qui conserve portée, ciblage, cooldown,
capacités et chemin de dégâts central. L'équipement est vidé avant application de la configuration.

## Stabilisation 0.9.2

La mise à terre requiert désormais un allié vivant ; sinon l'élimination et la fin d'équipe sont
immédiates. `DownedPlayerListener` interdit mouvement et actions, tandis que `PaperGameRuntime`
maintient une pose basse fixe et la restaure proprement.

`PaperWeaponService` possède les animations Mystery Box et Pack-a-Punch dans son tick existant.
Les achats n'ont lieu qu'à la fin des cinq secondes et les `ItemDisplay` sont nettoyés avec la
partie. Les hologrammes de machine vivent uniquement dans le clone d'instance. Les impacts zombies
ont un retour Paper court. `PaperPowerUpService` trouve et conserve une ancre de sol sûre par drop.

Le prochain ticket recommandé est le système complet de perks et de machines à atouts.
