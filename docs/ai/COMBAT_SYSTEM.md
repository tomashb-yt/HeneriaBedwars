# Système de combat

## Responsabilités

`CombatPolicy` décide si un joueur peut recevoir ou infliger un dégât. Il ne connaît ni Bukkit, ni inventaire, ni monde. `BukkitGamePlayListener` vérifie l'instance et le monde runtime, applique la décision à l'événement, adapte le profil 1.8 et enregistre l'attaquant uniquement après un coup accepté. `GameDeathService` reste seul responsable du respawn, de la mort finale, de l'élimination et de la victoire.

```text
Événement Spigot
    ↓
Instance PLAYING + monde hbw_game_*
    ↓
CombatPolicy (spectateur / protection / équipe / même partie)
    ↓
Dégâts + knockback + CombatTracker
    ↓
PlayerDeathEvent
    ↓
GameDeathService (respawn ou final kill)
```

## Profil `legacy_1_8`

- vitesse d'attaque fixée à `1024` pendant la partie lorsque le cooldown est désactivé;
- dégâts de balayage annulés;
- utilisation du bouclier annulée;
- un point ajouté aux coups de mêlée portés avec une épée;
- knockback final normalisé depuis le vecteur Spigot puis réglé par `gameplay.yml`;
- dix ticks d'invulnérabilité par défaut entre deux coups.

Ce profil reproduit les règles utiles d'un BedWars 1.8 sur l'API 1.21 sans prétendre émuler l'ancien protocole. Les valeurs originales de vitesse d'attaque et d'invulnérabilité sont conservées dans `PlayerPreGameSnapshot` puis restaurées au lobby.

## Règles BedWars

Les joueurs en attente restent gérés par la protection du lobby. En `PLAYING`, un spectateur, un joueur en respawn ou protégé ne subit aucun dégât. Un attaquant éliminé ne peut frapper personne. Le friendly-fire dépend de `combat.friendly-fire`. Une chute sous `void.minimum-y` tue immédiatement; `CombatTracker` attribue le kill au dernier ennemi valide si son coup est encore dans la fenêtre configurée.

Les projectiles utilisent leur tireur comme attaquant. Une déconnexion reste une mort finale. Aucune donnée de combat n'est écrite dans les YAML d'arène.
