# Tests et vérifications

**Statut :** suites Tickets 002–003 automatisées et cycle monde validé sur Paper ; validation
multi-clients à réaliser sur un serveur dédié.

## Automatisation

`.\gradlew.bat test` couvre :

- calculs bornés de population, santé, plafond vivant et délai ;
- réservations et comptage idempotent d'une manche ;
- transitions, isolation de deux parties, mise à terre, réanimation et défaite ;
- reconnexion avant échéance et élimination après expiration ;
- validation des nouvelles valeurs de configuration.

- transitions valides, invalides, capacité et snapshots d'instance ;
- création, fermeture et nettoyage après échec via un port monde simulé ;
- registre de services et cycle de vie ;
- unicité de session, réservation et expiration de reconnexion ;
- sélection d'audience et visibilité de deux instances simulées ;
- parser de toutes les commandes ;
- lecture bornée du spawn NBT et détection d'un monde sans manifeste ;
- validation et reload transactionnel de configuration ;
- compteurs de l'API publique.
- pagination vide et collection de plus de mille éléments ;
- historique borné, jetons et nettoyage de session GUI ;
- permissions visibles, verrouillées ou masquées ;
- délai de confirmation et expiration/validation de saisie ;
- valeurs GUI par défaut, thème, tailles, collisions, actions inconnues et snapshot atomique.
- création de définition, session exclusive, mutations et auto-save ;
- zones, portes, spawns, validation des références et navigation ;
- undo/redo, sauvegarde YAML, backup atomique et rechargement intégral.

`.\gradlew.bat clean qualityGate` ajoute Spotless, Checkstyle implicite du compilateur avec
`-Xlint:all`, Javadoc, checks, JAR ombré et `git diff --check`.

## Validation Paper

Préparer un monde valide dans `zombie_templates/crypt` sans manifeste, puis :

1. lancer `.\gradlew.bat :zombie-plugin:runServer` ;
2. vérifier l'activation sans erreur et `/zombie` ;
3. vérifier `/zombie map list`, ouvrir puis fermer `/zombie map preview crypt` ;
4. vérifier que la copie d'aperçu est supprimée, puis créer deux instances avec
   `/zombie instance create crypt` ;
5. connecter trois clients, en garder un au lobby et envoyer les autres dans des instances
   distinctes ;
6. vérifier visibilité, tablist, chat, morts, scoreboards et absence de fuite ;
7. déconnecter/reconnecter un joueur avant puis après le délai ;
8. arrêter une instance et vérifier retour lobby, déchargement et suppression ;
9. arrêter le serveur pendant une instance et vérifier que son dossier est préservé.
10. avec deux joueurs, ouvrir `/zombies` et `/zombie admin`, puis tester navigation, recherche,
    pages, permissions, clics spéciaux et confirmation ;
11. rendre `guis.yml` invalide, vérifier son refus, puis le corriger.
12. exécuter `/zmap create crypt_edit`, placer spawn, zones, porte et spawn zombie ;
13. déplacer, dupliquer, supprimer, tester undo/redo puis valider ;
14. quitter, redémarrer Paper, rouvrir la map et vérifier la restauration.
15. avec un modèle homonyme dans `zombie_templates`, exécuter `/zmap test`, vérifier le clone privé
    et la téléportation, puis quitter et arrêter l'instance ;
16. cliquer sur des cases vides et items sans métadonnées dans les GUI sans exception console.
17. lancer `/zmap test` et vérifier le spawn joueur éditorial dans le clone ainsi que l'absence de
    combustion solaire des zombies.

Une exécution locale sans trois comptes ne valide honnêtement que démarrage, création de monde,
commandes console et arrêt. La validation visuelle multi-clients doit être consignée séparément.

Validation Ticket 002 effectuée sur Paper 1.21.11 build 132 et Java 21 :

- activation du lobby et arrêt propre ;
- copie d'un monde modèle réel et chargement sous
  `zombie_instances/hz_<uuid>` ;
- état `WAITING`, inspection et compteur d'instances ;
- arrêt administratif, déchargement confirmé, suppression du dossier et compteur revenu à zéro ;
- arrêt serveur avec instance active, diagnostic d'interruption, déchargement et conservation du
  dossier.

La visibilité, la tablist, le chat, les scoreboards et la reconnexion sont couverts par les
politiques et simulations automatisées, mais leur rendu visuel avec trois comptes n'a pas pu être
exécuté dans cet environnement.

Les interactions d'inventaire Ticket 003 n'ont pas été exécutées avec plusieurs clients dans cet
environnement. Cette vérification manuelle reste obligatoire avant production.

Les clics de placement et protections de l'outil Ticket 004 n'ont pas été exécutés dans un client
Minecraft dans cet environnement. Le domaine et le stockage sont couverts automatiquement.

La validation manuelle Ticket 005 doit couvrir `/zmap test` à un et plusieurs joueurs, deux
instances, reconnexion, hémorragie, réanimation, arrêt administratif, retour lobby et nettoyage.
Elle n'a pas été exécutée ici faute de serveur et clients Minecraft disponibles.

## Ticket 006 — validation manuelle

1. Démarrer Paper et vérifier la création de `plugins/HeneriaZombie/zombies`.
2. Exécuter `/zzombie types` et vérifier les quatre types livrés.
3. Lancer `/zmap test`, attendre la manche 1 et contrôler ciblage, navigation et attaques.
4. Utiliser `/zzombie spawn toxic_zombie`, subir `poison_hit`, puis le tuer et contrôler
   `explode_on_death`.
5. Bloquer un zombie plus de huit secondes et contrôler le recalcul puis le retour au spawn.
6. Lancer deux instances et vérifier qu'aucune cible, attaque, explosion ou récompense ne traverse.
7. Tester `/zzombie info`, `debug`, `kill`, `removeall` et `reload`.
8. Modifier une définition, recharger et vérifier que seuls les futurs zombies changent.
9. Arrêter une partie et vérifier l'absence d'entités et de tâches résiduelles.

Ces scénarios avec clients Minecraft n'ont pas été exécutés dans cet environnement. Les tests
automatiques couvrent validation, doublons, attributs, sélection, isolation des cibles, dégâts,
headshots, mort idempotente, cooldowns, blocage et nettoyage des index.

## Charge à mesurer

Tester séparément 25, 50, 100 et 200 zombies sur une machine documentée. Relever MSPT moyen et
maximum, recalculs de cible, mises à jour différées, mémoire avant/après nettoyage et nombre
d'entités résiduelles. Le plafond technique de 200 mises à jour par tick répartit la charge ; il ne
constitue pas une promesse de capacité tant que ces mesures serveur ne sont pas réalisées.

## Critères de non-régression

Le thread Paper ne doit effectuer aucune copie de modèle. Un échec de déchargement doit préserver
les fichiers. Une erreur de création ne doit laisser aucune instance active. Aucun test ne doit
dépendre d'un ordre ou d'une connexion réseau.
