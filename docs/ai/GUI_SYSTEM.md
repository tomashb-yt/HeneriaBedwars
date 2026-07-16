# Système de menus interne

## Intégration Ticket 007

`MapMenuFactory` réutilise le framework pour la liste paginée, les informations, la création par chat, les actions de monde et la confirmation de suppression. Les apparences `map.*` viennent de `items.yml`; le menu ne lit jamais les YAML. La copie et la phase fichier de suppression quittent le thread serveur, puis le résultat revient sur le thread Bukkit avant tout message ou changement de vue. L'éditeur d'arènes ouvre aussi un sélecteur des cartes `BEDWARS` valides.

## Architecture

Le framework Ticket 003 est interne et séparé en deux frontières :

- `bedwars-core/gui` : modèle immutable `Gui`/`GuiButton`/`GuiItem`, actions, contextes, sessions, navigation, pagination, confirmation, slots et exécution protégée;
- `bedwars-plugin/gui` : `BukkitGuiService`, holder, listener, rendu `ItemStack`, sons, boutons standards et démonstration.

`GuiService` est enregistré dans le registre interne. `BukkitGuiService` est aussi un composant de cycle de vie : il enregistre le listener et une unique tâche centrale de rafraîchissement au démarrage, puis ferme les vues et annule la tâche à l'arrêt.

## Menu immutable

```java
Gui menu = Gui.builder()
    .id("example")
    .title("§bExample")
    .rows(3)
    .fillEmptySlots(true)
    .button(13, GuiButton.builder()
        .item(GuiItem.of("DIAMOND", "§bAction"))
        .onLeftClick(context -> context.refresh())
        .build())
    .build();
```

L'identifiant et le titre sont obligatoires, les lignes sont limitées de 1 à 6 et les slots doivent entrer dans la taille finale. Un doublon lève `DuplicateGuiSlotException`; `replaceButton` rend un remplacement explicite.

## Sessions et identification

Une ouverture racine crée un `GuiSession` par UUID joueur. La session contient un `sessionId`, un `viewId`, le menu racine/courant, l'historique limité, la page, les données, les timestamps anti-spam et les compteurs de rafraîchissement. Aucun `Player` n'est conservé dans le cœur.

`GuiInventoryHolder` porte `sessionId`, `viewId` et `menuId`. Le titre n'est jamais utilisé pour reconnaître un menu. Chaque navigation renouvelle `viewId`; un événement de fermeture de l'ancienne vue ne correspond donc plus à la nouvelle et ne peut pas supprimer sa session.

## Navigation

Dans une action :

```java
context.open(child);    // ajoute le menu courant à l'historique
context.replace(child); // remplace sans historique
context.back();         // entrée précédente ou fermeture
context.root();
context.close();
context.refresh();
```

La profondeur vient de `navigation.max-history-size`. Chaque changement exécute la fermeture logique de l'ancienne vue et l'ouverture de la nouvelle. Un refresh reconstruit les items dans le même inventaire, conserve session/page/historique et n'exécute pas une nouvelle ouverture logique.

## Boutons et clics

`GuiButton` accepte une source d'item statique ou dynamique, actions spécifiques/génériques, permission, `visibleWhen`, `enabledWhen` et cooldown. Les clics reconnus sont gauche/droit, Shift gauche/droit, milieu, double, touche numérique, drop, Ctrl-drop, créatif et inconnu. Une action générique ne s'applique qu'aux cinq clics usuels; les clics dangereux ne peuvent agir que s'ils ont été explicitement configurés.

`GuiClickContext` fournit menu, session, slot, type, données et opérations de navigation/message/son. Toute exception passe par `GuiActionExecutor`; l'adaptateur journalise menu, session, joueur, slot, clic et stack trace, envoie le message traduit et conserve la session.

## Sécurité inventaire

`GuiListener` couvre ouverture, clic, drag, fermeture, quit, kick et désactivation. Par défaut :

- tout clic dans l'inventaire GUI est annulé;
- les clics dans l'inventaire joueur sont annulés pendant une vue GUI;
- Shift, touches numériques, double collecte, drop et créatif n'exécutent aucune action générique;
- tout drag touchant le haut est annulé;
- les stacks affichées sont recréées, jamais partagées;
- les sessions sont supprimées par correspondance joueur + session + vue.

## Pagination et confirmation

`Pagination<T>` gère liste vide, pages bornées, première/dernière/suivante/précédente et recadrage après réduction de la liste. `GuiSlots.rectangle(row, column, rows, columns)` produit les zones de contenu. La démonstration affiche 50 éléments sur 21 slots.

`ConfirmationGui` construit un menu information/confirmation/annulation, avec permission facultative et cooldown par défaut de 500 ms. L'annulation peut revenir à la vue précédente.

## Items, traductions et sons

`BukkitGuiService` résout désormais les clés par `ItemService`; `GuiItemRenderer` ne subsiste que pour les rendus dynamiques explicitement fournis par du code. Bordure, retour, fermeture, confirmation, annulation, pagination, refresh, informations et démonstration viennent de `items.yml`. Les placeholders passent par `ItemContext` et le système Ticket 002 après interprétation des couleurs, ce qui empêche l'injection de balises par un nom de joueur.

```java
GuiButton.builder()
    .itemKey("gui.close")
    .onLeftClick(context -> context.close())
    .build();
```

Une clé peut être dynamique avec `itemKey(renderContext -> ...)` et recevoir des valeurs avec `itemPlaceholders`. Le builder refuse une clé et un `GuiItem` simultanés. Les actions, permissions et slots ne sont jamais lus depuis `items.yml`. Le holder/session/slot reste l'identité d'une action; le PDC `heneriabedwars:item_key` n'est qu'un complément pour les futurs objets hors GUI. Le nom ou le lore ne doit jamais servir d'identifiant.

`/bedwars item preview` est un menu paginé. Il reconstruit les items depuis le snapshot actif, ajoute leur clé au lore de prévisualisation et donne une copie au clic seulement avec `heneriabedwars.admin.item.give` et une place libre. Un reload valide apparaît au prochain refresh/réouverture; un reload refusé laisse les vues et l'ancien registre intacts.

Les sons configurés sont `open`, `click`, `success`, `error`, `back` et `close`. Une valeur invalide devient un warning de configuration et utilise le défaut. Le système peut être désactivé globalement.

## Thread et rafraîchissement

Les inventaires sont toujours modifiés sur le thread serveur. Une ouverture hors thread est replanifiée avec le scheduler Bukkit. Une seule tâche centrale examine les sessions ayant `autoRefresh`; aucune tâche n'est créée par bouton ou clic. L'intervalle global minimum vient de `refresh.minimum-interval-ticks`.

## Démonstration

`/bedwars gui` et `/hbw gui`, permission `heneriabedwars.admin.gui`, ouvrent un menu de 6 lignes avec informations dynamiques, cinq types de clic, 50 éléments paginés, confirmation, sous-menu, retour, refresh, fermeture et erreur contrôlée visible uniquement en debug. Ce menu ne modifie aucune donnée BedWars.

## Éditeur d'arènes

`/bedwars setup` ouvre le menu administratif principal et `/bedwars arena` ou `/bedwars arena menu` ouvre directement la liste. `ArenaEditorMenuFactory` produit les vues suivantes : accueil, configuration, liste, éditeur, mondes, joueurs, équipes, limites, validation et confirmations. La console reçoit un message adapté au lieu de tenter d'ouvrir un inventaire.

La liste est paginée avec filtre `ALL/ENABLED/DISABLED/INVALID/DRAFT` et tri `ID/NAME/STATUS/UPDATED`. `ArenaEditorStateStore` conserve filtre, tri et page lors des retours et rafraîchissements. Clic gauche ouvre l'éditeur, clic droit la validation et Shift-droit la suppression. La création ferme le menu puis démarre une saisie d'identifiant.

`TextInputManager` autorise une saisie active par joueur. `BukkitTextInputService` annule le chat avant toute diffusion, traite le contenu sur le thread serveur, limite la longueur et gère validation, mots d'annulation, timeout, déconnexion et arrêt. Une saisie terminée ou expirée rouvre la vue pertinente; une déconnexion ou un arrêt ne tente pas de rouvrir un inventaire.

L'éditeur sauvegarde automatiquement le nom, le monde, les positions, joueurs, équipes générales, limites et statut au moyen d'`ArenaService`. Les valeurs numériques offrent des pas au clic et une saisie directe au clic milieu. Les changements d'équipes passent par une confirmation montrant l'ancienne et la nouvelle capacité. La validation utilise une apparence INFO/WARNING/ERROR/CRITICAL et chaque problème ouvre la section associée.

Chaque vue d'édition capture la révision de l'arène. Si une autre commande ou un autre administrateur a déjà sauvegardé, l'action obsolète est refusée avec `CONFLICT`; le joueur doit rafraîchir. Les positions, mondes et points de limite sont téléportables uniquement avec `heneriabedwars.admin.arena.teleport`. La suppression d'une arène active demande une seconde confirmation et passe toujours par la sauvegarde de `ArenaService.delete`.

Les items visuels sont les clés `admin.*`, `arena.*`, `world.*`, `players.*`, `teams.*`, `boundary.*` et `validation.*` de `items.yml`. Les actions, permissions et données restent en Java. Les boutons retour utilisent l'historique, actualiser reconstruit la vue, fermer termine la session et une arène supprimée pendant l'édition renvoie vers une vue sûre.

## Créer un nouveau menu

1. Utiliser un identifiant stable et un titre traduit.
2. Valider les slots et préférer `GuiSlots` pour les zones.
3. Construire des items ne contenant aucune logique métier.
4. Placer autorisations et conditions sur le bouton.
5. Utiliser le contexte pour naviguer; ne jamais ouvrir directement un inventaire Bukkit.
6. Laisser l'exception remonter jusqu'à `GuiActionExecutor`.
7. Ajouter des tests purs pour modèle, navigation et pagination.
8. Tester manuellement toutes les manipulations d'inventaire sur Paper.

Erreurs fréquentes : identifier par titre, stocker `Player` dans une session, réutiliser un `ItemStack` mutable, supprimer une session sans `viewId`, créer une tâche par vue, appeler Bukkit hors thread ou coder un texte directement en Java.

Checklist : identité, taille, traductions FR/EN, permissions, conditions, cooldown, retour, fermeture, drag/clics dangereux, erreur contrôlée, cleanup, tests, documentation.
