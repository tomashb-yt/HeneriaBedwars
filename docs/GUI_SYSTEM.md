# Système GUI

**Statut :** framework configurable et parcours joueur/administrateur opérationnels.

## Architecture

`GuiService` est l'unique moteur d'ouverture, navigation, rafraîchissement et nettoyage.
`GuiRegistry` reçoit les écrans et `GuiActionRegistry` leurs callbacks nommés. `GuiListener` annule
les interactions d'inventaire et transmet uniquement un bouton lié au bon joueur et au jeton
courant. La logique métier reste dans les services, jamais dans le listener.

Un écran implémente `Gui` ou utilise `StandardGui`. Son rendu reçoit un `GuiView` qui applique le
thème, crée les items Adventure et lie les actions typées aux slots. Aucun item n'est identifié par
son nom.

## Cycle de vie et sessions

1. `openHome` crée une `GuiSession` isolée et fixe son accueil.
2. L'ouverture tourne un UUID de vue et empile éventuellement le menu précédent.
3. Un clic n'est accepté que si joueur, inventaire, session et UUID correspondent.
4. L'action peut naviguer, rafraîchir, demander une saisie ou une confirmation.
5. Fermeture terminale, déconnexion, expiration ou arrêt libèrent toutes les références.

Chaque session possède menu/contexte, accueil, historique LIFO borné à 32 entrées, page, recherche,
filtres, données temporaires, confirmation, saisie et prochaine actualisation. Rien n'est partagé
entre joueurs.

## Composants

- `GuiButton` : item, permission et actions gauche, droite ou Maj ;
- boutons configurés visibles, masqués ou affichés verrouillés ;
- information non cliquable ;
- navigation retour, accueil, page précédente et suivante ;
- `GuiPagination`, sans plafond total arbitraire ;
- `GuiConfirmation`, avec cible, conséquences et délai ;
- `GuiInputRequest`, avec invite, expiration, validation, acceptation et annulation.

Les boutons booléens et sélecteurs numériques utilisent les mêmes actions gauche/droite/Maj : ils
ne nécessitent aucun listener supplémentaire.

## Menus livrés

- `/zombies` : accueil joueur, catalogue publié et sortie de partie ;
- `/zombies admin` : accueil administration consolidé ;
- maps joueur : informations, état temps réel et entrée automatique ;
- maps administrateur : création, édition, validation, test, publication et versions ;
- maps : recherche, pagination, aperçu et création d'instance ;
- instances : état dynamique, entrée et arrêt confirmé ;
- diagnostics : versions, compteurs, lobby, stockage et avertissements ;
- confirmation partagée.

Groupe, profil et statistiques persistantes restent absents tant que leur modèle de données n'est
pas livré. Les commandes techniques restent disponibles pour la console et le diagnostic.

## Configuration et thèmes

`plugins/HeneriaZombie/guis.yml` est créé hors thread serveur. Le schéma courant `2` définit
`default-theme`, `themes` et `menus`. Un bouton configure slot, matériau, nom MiniMessage, lore, permission,
visibilité verrouillée, son et actions.

Le candidat est complété récursivement en mémoire par chaque valeur feuille embarquée manquante,
puis validé. Cette fusion permet à un `guis.yml` d'une ancienne version de recevoir de nouveaux
menus sans perdre ses personnalisations. Avant cette fusion, la migration du schéma 1 retire
uniquement les anciens boutons joueur encore reliés à leurs actions embarquées et redirige
`play` vers le catalogue publié. Elle empêche la collision historique du slot 24.

La validation contrôle ensuite :

- taille multiple de neuf entre 9 et 54 ;
- slots et collisions ;
- matériaux et sons ;
- thèmes et actions référencés.

Une erreur agrégée refuse atomiquement le candidat et conserve le snapshot valide. Le rendu ne lit
jamais le disque. Couleur, icône et texte décrivent ensemble chaque état.

```yaml
schema-version: 2
default-theme: dark
themes:
  dark:
    background: {material: BLACK_STAINED_GLASS_PANE, name: " "}
menus:
  custom:
    title: "<aqua>Mon menu"
    size: 27
    theme: dark
    buttons:
      back:
        slot: 18
        material: ARROW
        name: "<blue>Retour"
        actions: {left: nav.back}
```

Pour ajouter ce menu, enregistrer un `StandardGui("custom", ...)` dans `GuiRegistry` et ses
callbacks stables dans `GuiActionRegistry`. Les travaux lourds partent sur le pool I/O avant de
revenir sur le thread Paper.

## Saisie, confirmations et rafraîchissement

L'adaptateur actuel intercepte `AsyncChatEvent` uniquement pour le joueur ayant une demande active.
`annuler` ou l'expiration rouvre le menu. `GuiInputRequest` permettra un futur adaptateur enclume,
livre ou panneau.

La confirmation est locale à la session ; le bouton reste sans effet avant le délai configuré.
Fermer le menu détruit le callback. Une seule tâche partagée entretient toutes les sessions toutes
les 20 ticks et actualise les inventaires en place selon leur intervalle.

## Permissions et erreurs

Les permissions historiques restent compatibles. Le parcours courant utilise
`zombies.menu.player`, `zombies.menu.admin`, `zombies.admin.maps.*`,
`zombies.admin.games.view`, `zombies.admin.config.view` et `zombies.admin.diagnostics`. Elles sont
revérifiées au clic.

Une taille invalide, collision, action ou thème inconnu est journalisé et refuse le reload. Une
exception d'action est journalisée et signalée au joueur sans casser le moteur.

## Validation manuelle

Avec deux clients Paper, tester isolation, retour/accueil, recherche, longues listes, permissions,
clics gauche/droit/Maj, touches numériques, double clic, collecte au curseur, glisser-déposer,
reload invalide puis valide et disparition d'une instance avant confirmation.

## Catalogue des armes

`weapon-browser` pagine toutes les définitions actives et affiche catégorie, rareté, dégâts et
chargeur. Un clic ouvre `weapon-detail`; un Maj-clic administrateur distribue l'arme uniquement si
le joueur est dans une partie. Le bouton du menu administrateur et tous les slots restent
configurables dans `guis.yml`.
