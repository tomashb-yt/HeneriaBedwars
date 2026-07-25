# Système GUI

**Statut :** framework configurable et écrans fondamentaux opérationnels au Ticket 003.

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

- `/zombies` : menu joueur et accès aux instances ;
- `/zombie admin` : accueil administration ;
- maps : recherche, pagination, aperçu et création d'instance ;
- instances : état dynamique, entrée et arrêt confirmé ;
- diagnostics : versions, compteurs, lobby, stockage et avertissements ;
- confirmation partagée.

Groupe, profil, statistiques, paramètres, éditeurs, duplication, export, suppression de map,
observation et nettoyage forcé sont explicitement indiqués comme futurs.

## Configuration et thèmes

`plugins/HeneriaZombie/guis.yml` est créé hors thread serveur. Il définit `default-theme`,
`themes` et `menus`. Un bouton configure slot, matériau, nom MiniMessage, lore, permission,
visibilité verrouillée, son et actions.

Le candidat est complété en mémoire par les valeurs embarquées, puis valide :

- taille multiple de neuf entre 9 et 54 ;
- slots et collisions ;
- matériaux et sons ;
- thèmes et actions référencés.

Une erreur agrégée refuse atomiquement le candidat et conserve le snapshot valide. Le rendu ne lit
jamais le disque. Couleur, icône et texte décrivent ensemble chaque état.

```yaml
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

Les permissions sont `zombie.gui.player`, `admin`, `maps`, `instances`, `settings`, `diagnostics`
et `dangerous-actions`. Elles sont revérifiées au clic. Le YAML choisit si un refus masque le
bouton ou affiche une barrière avec la permission requise.

Une taille invalide, collision, action ou thème inconnu est journalisé et refuse le reload. Une
exception d'action est journalisée et signalée au joueur sans casser le moteur.

## Validation manuelle

Avec deux clients Paper, tester isolation, retour/accueil, recherche, longues listes, permissions,
clics gauche/droit/Maj, touches numériques, double clic, collecte au curseur, glisser-déposer,
reload invalide puis valide et disparition d'une instance avant confirmation.
