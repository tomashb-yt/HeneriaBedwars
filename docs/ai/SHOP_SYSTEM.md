# Système de boutiques — Ticket 014

## Parcours administrateur

Ouvrir `/bedwars`, choisir l'arène, puis une équipe. La zone **BOUTIQUE** permet de mémoriser la position actuelle, de la visiter ou de la retirer. L'orientation du regard est conservée. Chaque équipe peut avoir son propre PNJ; l'absence de position ne bloque pas encore la validation de l'arène.

## Parcours runtime

Au passage de l'instance en `WAITING`, les positions du monde modèle sont remappées dans le clone `hbw_game_*`. Un villageois protégé est créé pour chaque position configurée. Il n'ouvre la boutique qu'à un membre vivant de la même instance en état `PLAYING`.

Le PNJ est recréé au passage `PLAYING` afin de réparer une création anticipée ou un chunk non prêt. Son apparition ne dépend plus du nombre d'offres valides : un catalogue vide produit un menu indisponible, jamais la disparition silencieuse du villageois. Les anciennes installations reçoivent les offres manquantes dans `shops.yml` au redémarrage, avec sauvegarde et sans écraser leurs valeurs.

Le menu expose quatre catégories : blocs, combat, distance et utilitaires. Chaque article affiche sa quantité, son prix, la monnaie et le solde du joueur. Après achat, la vue et le portefeuille sont rafraîchis.

## Catalogue

Les offres vivent sous `shops.offers.<id>` dans `shops.yml` :

```yaml
shops:
  runtime-enabled: true
  offers:
    wool:
      category: BLOCKS
      material: WHITE_WOOL
      amount: 16
      currency: IRON
      price: 4
      translation-key: shop.offer.wool
      order: 10
```

Catégories acceptées : `BLOCKS`, `COMBAT`, `RANGED`, `UTILITY`. Monnaies acceptées : `IRON`, `GOLD`, `DIAMOND`, `EMERALD`. Un matériau ou une offre invalide est ignoré et signalé dans les journaux.

## Garantie transactionnelle

`ShopPurchaseService` vérifie le contexte métier. `BukkitShopInventory` clone ensuite le stockage du joueur, simule le paiement et l'ajout complet du produit, puis remplace le contenu uniquement si l'ensemble réussit. Un inventaire plein, un solde insuffisant ou une offre invalide ne retire aucune ressource.

## Hors périmètre

Le Ticket 014 ne gère pas les armures équipées, les outils évolutifs, les raccourcis favoris, les pièges ni les améliorations d'équipe. Ces mécaniques appartiennent au Ticket 015.
