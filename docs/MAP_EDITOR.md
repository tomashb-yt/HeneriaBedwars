# Éditeur universel de maps

**Statut :** opérationnel au Ticket 004.

## Parcours administrateur

1. `/zmap create <id>` crée `plugins/HeneriaZombie/maps/<id>/map.yml`, associe le monde courant,
   mémorise l'auteur et les dates, puis ouvre l'éditeur.
2. `/zmap edit <id>` ouvre une session exclusive et donne l'outil protégé dans le slot 9.
3. La GUI donne accès aux informations, zones, portes, spawns, objets, validation et sauvegarde.
   Un clic droit dans l'air ou accroupi + clic droit avec l'outil la rouvre à tout moment.
4. `/zmap leave` sauvegarde, ferme la session et retire l'outil. Une déconnexion fait de même.

`/zmap test` exige une validation sans erreur et un monde source homonyme dans
`zombie_templates/<mapId>`. La commande ferme proprement l'éditeur, supprime un éventuel aperçu,
crée une instance privée puis y téléporte l'administrateur. Cette instance teste le clone, le
spawn, l'isolation et la définition ; elle ne lance pas encore de manches ni de zombies.

Une session contient la définition courante, l'outil, la sélection, le presse-papiers et un
historique undo/redo borné à 64 versions. Un administrateur ne possède jamais deux sessions.

## Édition

Nom, description, icône, image, joueurs maximum, musique, difficulté et mode sont saisis de façon
privée dans le chat. Auteur et monde sont fixés à la création. Le spawn joueur se place avec
l'outil.

Chaque collection est sans plafond métier. Ajouter sélectionne l'outil de placement ; un clic
gauche ou droit avec l'outil sur un bloc place ou déplace l'élément. Dans une liste GUI, un clic
gauche prépare le déplacement, un clic droit demande confirmation avant suppression et Maj-clic
duplique. Les objets couvrent barricade, Mystery Box, Pack-a-Punch, perk, piège, téléporteur,
power, objectif, quête et boss. Leurs paramètres extensibles sont stockés dans `properties`.

Zones, portes et spawns possèdent des champs typés complets dans `MapDefinition`. Leur création en
jeu fournit des valeurs sûres et leurs références de zone. Ces paramètres seront consommés par les
futurs systèmes de gameplay sans migration du schéma.

## Sauvegarde et concurrence

`MapEditorService` applique une mutation à un snapshot immuable, alimente `UndoManager`, remplace
le registre puis déclenche l'auto-save. Les écritures d'une même map sont sérialisées sur le pool
I/O. Le fichier temporaire est remplacé atomiquement et l'ancienne version devient `map.yml.bak`.
Aucune lecture ou écriture YAML n'a lieu sur le thread Paper.

Le chargement refuse une version inconnue. L'identifiant est validé et le chemin normalisé ne peut
pas sortir du dossier `maps`.

## Validation

`/zmap validate` et la GUI produisent erreurs, avertissements et conseils. Le validateur contrôle
spawn joueur, zones vides, références des portes, spawns et objets, cohérence power, Mystery Box,
Pack-a-Punch et connexité du graphe de zones. Une définition invalide reste éditable mais ne devra
pas être activée par la future boucle de jeu.

## Responsabilités et sécurité

- le modèle, registre, validateur et service sont indépendants de Paper ;
- session, sélection, presse-papiers, outil et historique portent seulement l'état léger ;
- `YamlMapPersistence` adapte le stockage asynchrone et versionné ;
- `EditorGuiModule` utilise exclusivement le moteur GUI du Ticket 003 ;
- le listener traduit les clics Paper en commandes métier ;
- l'outil est identifié par Persistent Data Container, jamais par son nom.

L'outil ne peut ni être jeté, ni déplacé dans un inventaire, ni utilisé sans session et permission.

## Consommation par le moteur

Le moteur utilise le spawn joueur éditorial et les `ZombieSpawn` validés. Pour chaque spawn, zone,
poids, capacité, manches, distances, types autorisés et cooldown alimentent la sélection runtime.
Portes et barricades restent des données d'édition jusqu'à leur ticket gameplay ; le moteur
d'ennemis ne les modifie pas directement.
