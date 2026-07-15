# Limitations connues

- Aucun gameplay, arène, équipe, lit, générateur, boutique ou menu.
- Aucune commande joueur ; seule la commande administrative de version existe.
- Aucun stockage fonctionnel malgré la valeur d'amorçage `sqlite`.
- Configuration minimale sans reload, migration ni messages traduits.
- API publique minimale non encore publiée auprès de Paper.
- Compatibilité compilée pour Paper 1.21.11 ; le chargement réel sur un serveur doit encore être validé manuellement.
- Le document historique `docs/ARCHITECTURE.md` décrit une cible plus large que les trois modules actuels.
- Le build désactive temporairement les caches/incréments et agrège les sources dans le module final pour contourner l'isolation du classpath Windows observée pendant le Ticket 001 ; cette mesure devra être réévaluée sur CI.
