# Configuration

Le fichier embarqué est `bedwars-plugin/src/main/resources/config.yml`. Paper le copie vers `plugins/HeneriaBedWars/config.yml` au premier démarrage.

## Valeurs

- `plugin.language` : `fr_FR` par défaut ; valeur non vide préparant l'internationalisation ;
- `plugin.debug` : `false` par défaut ; active les messages de diagnostic internes ;
- `server.shutdown-on-critical-startup-error` : `true` par défaut ; politique réservée aux erreurs critiques ;
- `storage.type` : `sqlite` par défaut et seule valeur déclarée à ce stade.

Les valeurs absentes utilisent les défauts embarqués. Une langue vide ou un stockage non déclaré provoque une erreur explicite au démarrage, journalisée avec sa cause, puis le plugin est désactivé. Validation avancée, migrations, reload et menus de configuration appartiennent au Ticket 002.
