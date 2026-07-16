# Système de menus interne

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

## Menus d'arènes

`/bedwars arena menu` ouvre la liste paginée construite par `ArenaMenuFactory`. Chaque entrée utilise `arena.entry` dans `items.yml` et ouvre un détail avec monde, positions, validation, activation/désactivation et suppression. Les actions revérifient leur permission spécialisée au clic. La suppression passe par `ConfirmationGui` et `ArenaService.delete`; elle n'est jamais effectuée directement par le menu.

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
