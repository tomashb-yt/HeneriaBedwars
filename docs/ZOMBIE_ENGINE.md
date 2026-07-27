# Moteur de zombies

**Statut :** Ticket 006 opérationnel.

## Flux

Une manche réserve une apparition, puis `PaperGameRuntime` sélectionne un spawn valide selon la
manche, la capacité, le cooldown, la distance et son poids. `PaperZombieEngine` choisit ensuite un
type éligible dans le registre immuable, calcule ses attributs et crée l'entité via la fabrique
Paper.

Chaque `ZombieInstance` conserve un snapshot de sa définition. Un reload ne change donc jamais un
ennemi actif. `ZombieTracker` fournit des index O(1) par UUID interne, UUID Bukkit et partie.

La boucle globale de partie appelle l'IA une fois par tick. Au plus 200 ennemis sont traités par
tick et le curseur reprend au tick suivant. Il n'existe aucune tâche par zombie et aucun scan
global des entités.

## Cycle et isolation

Les états vont de `SPAWNING` à `DEAD`, `DESPAWNED` ou `INVALID`. La mort est revendiquée
atomiquement par `claimDeath`; récompense et compteur de manche ne peuvent être appliqués qu'une
fois. Toute cible doit être vivante, non spectatrice, en ligne, dans la même partie et le même
monde.

Paper force chaque mob créé à rester conscient et agressif. Le moteur conserve la cible native
pour la navigation, puis traduit aussi chaque contact natif en une frappe autoritaire soumise à la
portée et au cooldown configurés. Les dégâts vanilla sont annulés afin qu'un contact ne frappe
jamais deux fois. L'équipement généré aléatoirement par Minecraft est vidé avant d'appliquer
l'équipement YAML.

Les disparitions anormales libèrent le compteur vivant. `GAME_ENDED` et `ROUND_CANCELLED`
nettoient sans récompense ni progression artificielle. La fin de partie appelle `removeAll` et
libère les registres.

## Limites actuelles

La navigation `GROUND` utilise l'IA native stable de Paper, sans NMS. Les modes volant, distant,
portes et barricades ont leurs contrats métier, mais leur gameplay arrivera avec leurs systèmes
dédiés. La téléportation anti-blocage revient au spawn d'origine après trois tentatives de
recalcul. Les tests de charge réels en serveur restent à mesurer.

## Récompenses et bonus

Le moteur retourne uniquement les dégâts réellement appliqués et la récompense configurée ; il ne
modifie aucun solde. `RewardService` déduplique les impacts et la mort, conserve les contributions
pour les assistances puis les efface. La position de mort est transmise à l'adaptateur de drops.
Nuke et Insta-Kill repassent par `ZombieDamageService`, et la protection `claimDeath` garantit
qu'une mort concurrente ne récompense jamais deux fois.
