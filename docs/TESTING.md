# Tests et vérifications

**Statut :** suites Tickets 002–003 automatisées et cycle monde validé sur Paper ; validation
multi-clients à réaliser sur un serveur dédié.

## Automatisation

`.\gradlew.bat test` couvre :

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

## Critères de non-régression

Le thread Paper ne doit effectuer aucune copie de modèle. Un échec de déchargement doit préserver
les fichiers. Une erreur de création ne doit laisser aucune instance active. Aucun test ne doit
dépendre d'un ordre ou d'une connexion réseau.
