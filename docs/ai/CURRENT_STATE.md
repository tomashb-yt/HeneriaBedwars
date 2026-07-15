# État actuel

- Dernier ticket terminé : Ticket 004.
- Version : `0.1.0-SNAPSHOT`.
- Cibles : Java 21, Spigot/Paper 1.21.x.

## Disponible

- bootstrap et cycle de vie déterministes;
- API publique minimale de statut;
- création non destructive de neuf YAML principaux, deux langues et trois dossiers runtime;
- validation INFO/WARNING/ERROR/CRITICAL, défauts contrôlés et masquage des secrets;
- réglages immuables `PluginSettings`, `GameplaySettings`, `LobbySettings`, `StorageSettings` et `MenuSettings`;
- reload transactionnel et accès par registre;
- messages FR/EN, couleurs nommées/hex/legacy et placeholders non interprétés;
- commandes `version`, `reload`, `config`, `language` et `language set`, aussi via `/hbw`;
- permissions et complétion filtrée;
- sauvegarde et contrat de migration prêts, sans migration artificielle.
- migration réelle et ciblée du `config.yml` officiel Ticket 001 non versionné vers la version 1, avec sauvegarde préalable et fusion non destructive des défauts.
- framework GUI interne fonctionnel : modèle immutable, sessions, holder robuste, navigation, pagination, confirmations, refresh, sons, permissions, anti-spam et erreurs centralisées;
- menu de démonstration administrateur via `/bedwars gui` et `/hbw gui`.
- registre d'items immuable intégré au snapshot de configuration, définitions YAML, validation, fallback, héritage borné, placeholders, traductions et construction Bukkit;
- chaque rendu produit un `ItemStack` distinct avec glow, enchantements sûrs, flags, unbreakable, custom model data, cuir, tête de joueur contextuelle et PDC contrôlé;
- le framework GUI et sa démonstration utilisent des clés d'items; `/bedwars item list|give|preview` fournit les outils administratifs de test.

## Non disponible

Aucun éditeur complet d'items, objet de gameplay actif, boutique, menu métier d'arène/lobby, arène, équipe, lit, générateur actif, achat, amélioration, lobby protégé, base de données, resource pack ou PlaceholderAPI. Les fichiers correspondants sont préparatoires.

Les 70 tests automatisés passent. Aucun serveur Minecraft n'est disponible dans l'environnement Codex pour certifier les tests en jeu.
