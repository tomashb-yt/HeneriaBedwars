# Transactions

## Cycle

Chaque requête fournit partie, joueur, montant `long`, `TransactionReason`, métadonnées et
`operationId`. Sous le verrou du `GameEconomy`, `TransactionService` vérifie l'activité, le
portefeuille, le montant, le solde et la limite, calcule le nouveau solde puis crée une
`Transaction` immuable.

Les statuts sont explicites : succès, montant invalide, partie ou joueur inconnu, fonds
insuffisants, limite atteinte, doublon, annulation, partie inactive ou erreur interne. Un booléen
seul n'est jamais utilisé comme résultat métier.

## Idempotence

Le résultat d'une opération est mémorisé pendant toute la partie. Une seconde requête avec le même
`operationId` renvoie `DUPLICATE_TRANSACTION` et référence la transaction originale sans
réappliquer le solde. Les impacts et interactions Paper utilisent la combinaison partie, joueur,
objet/cible et tick serveur ; les commandes utilisent une séquence propre à leur exécution.

## Plafond et journal

Les crédits emploient `Math.addExact`, un plafond configurable et `CLAMP`, `REJECT` ou
`LOG_AND_CLAMP`. Les débits refusent un solde négatif par défaut. Le journal est indexé par UUID de
transaction et par joueur. Sa taille détaillée est bornée sans remettre à zéro les agrégats du
portefeuille.

## Remboursement

Un `RefundRequest` référence obligatoirement le débit original. Les remboursements partiels sont
additionnés et ne peuvent jamais dépasser ce débit. Chaque compensation possède sa propre clé
d'idempotence et une transaction `REFUND` contenant l'UUID original dans ses métadonnées.
