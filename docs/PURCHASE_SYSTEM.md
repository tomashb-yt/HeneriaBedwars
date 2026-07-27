# Système d'achats

## Pipeline atomique

`PurchaseService` exécute, sous un verrou par partie :

1. validation du contexte et du mode de financement ;
2. résolution du prix ;
3. débit idempotent ;
4. attribution Paper ;
5. remboursement compensatoire si l'attribution échoue ;
6. mémorisation du résultat et émission des événements.

Une opération terminée ne peut donc ni attribuer deux fois l'objet, ni débiter deux fois, ni
rembourser au-delà du débit original.

## Prix

`PriceResolver` applique dans cet ordre : prix de base, modificateurs fixes, multiplicateurs,
arrondi (`FLOOR`, `CEIL` ou `NEAREST`), puis bornage minimum/maximum. Les calculs intermédiaires
utilisent `BigDecimal` et le résultat reste un `long`.

## Intégrations actives

Les armes murales, recharges murales, Mystery Box et niveaux Pack-a-Punch utilisent ce service.
L'attribution d'une arme qui échoue après paiement déclenche un remboursement intégral. Les types
Perk, porte, piège, téléporteur et spécial sont prêts dans l'API sans simuler ces gameplays.

Le financement `INDIVIDUAL` est le seul activé. Les autres valeurs de `PurchaseFundingMode` sont
refusées explicitement afin qu'un futur appel ne produise jamais un achat gratuit accidentel.
