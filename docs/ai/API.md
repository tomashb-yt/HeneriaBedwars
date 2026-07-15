# API publique

L'API du Ticket 001 est volontairement minimale : `HeneriaBedWarsApi` expose seulement la version et l'état général, avec `PluginStatus`. Elle n'est pas encore publiée dans le `ServicesManager` Paper et ne constitue pas une API d'addons complète.

Les futurs contrats devront être stables, versionnés, documentés par JavaDoc et exposer des modèles immuables. Une API obsolète sera dépréciée avant suppression. Les événements publics seront documentés et aucune implémentation interne ne sera exposée inutilement.
