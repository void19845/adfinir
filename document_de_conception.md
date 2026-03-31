# GDD — adfinir : Dungeon Adventure Roguelike

> Jeu d'aventure action-RPG roguelike, vue du dessus 2D, développé avec libGDX (Java/Kotlin).

---

## 1. Vision du jeu

**adfinir** est un roguelike action-RPG en vue top-down. Le joueur explore des donjons générés procéduralement, combat des ennemis en temps réel, ramasse des équipements et tente de descendre le plus profondément possible. La mort est permanente : chaque run recommence depuis le début.

---

## 2. Game Loop

```
Lancement
    └─> Menu principal
            └─> Création / sélection du personnage
                    └─> Niveau 1 du donjon
                            ├─> Explorer les salles
                            ├─> Combattre ennemis
                            ├─> Ramasser loot / équipement
                            ├─> Descendre au niveau suivant
                            │       └─> Boss (fin de chaque niveau)
                            │               └─> Niveau suivant...
                            └─> Mort → Game Over → Retour au menu
```

---

## 3. Mécaniques principales

### 3.1 Mouvement du joueur

- **Grille logique** : le monde est basé sur des tiles (ex. 16×16 px).
- **Mouvement fluide** : le joueur se déplace en pixels avec interpolation, mais reste aligné sur la grille à l'arrêt.
- **Contrôles** : ZQSD / WASD ou joystick (libGDX controllers).

### 3.2 Combat (temps réel)

Trois modes d'attaque disponibles simultanément :

| Mode | Description | Exemple |
|------|-------------|---------|
| Mêlée | Attaque courte portée, zone devant le joueur | Épée, hache |
| Distance | Projectile lancé dans la direction visée | Arc, baguette |
| Compétence spéciale | Cooldown, effet puissant (zone, dash, invoc.) | Boule de feu, téléportation |

**Attributs de combat :**
- PV (points de vie)
- ATK (attaque physique)
- MAG (puissance magique)
- DEF (défense)
- SPD (vitesse de déplacement et d'attaque)

### 3.3 Inventaire & Équipement

- **Slots d'équipement** : arme principale, arme secondaire, armure, accessoire ×2.
- **Types d'items** : armes (mêlée / distance), armures, bijoux (bonus passifs), consommables (potions).
- **Rareté** : Commun → Peu commun → Rare → Épique → Légendaire.
- **Gestion** : grille d'inventaire limité (ex. 4×6 slots), drag & drop via Scene2D.

### 3.4 Progression (Roguelike)

- **Mort permanente** : à la mort, tout est perdu. Nouvelle run = nouveau donjon.
- **Progression intra-run** : montée en niveau, nouveaux équipements, compétences débloquées.
- **Aucune progression persistante** entre les runs (roguelike pur).

---

## 4. Structure du donjon

### 4.1 Génération procédurale

Algorithme suggéré : **BSP (Binary Space Partitioning)** ou **méthode des salles aléatoires avec couloirs**.

- Chaque niveau est une grille de tiles générée à chaque run.
- Salles de tailles variées connectées par des couloirs.
- Placement aléatoire de : ennemis, coffres, équipements, escalier vers le niveau suivant.

### 4.2 Niveaux de donjon

| Niveau | Thème | Difficulté | Boss |
|--------|-------|------------|------|
| 1–2 | Cavernes | Facile | Mini-boss de fin de niveau 2 |
| 3–4 | Tombeaux | Moyen | Boss de fin de niveau 4 |
| 5–6 | Enfer souterrain | Difficile | Boss final de niveau 6 |

### 4.3 Boss

- Chaque niveau se termine par une salle de boss verrouillée.
- Le boss a des patterns d'attaque distincts (phases multiples).
- Loot garanti à la mort du boss.

---

## 5. Architecture technique (libGDX)

### 5.1 Structure des Screens

```
Main (ApplicationAdapter)
    ├─> MainMenuScreen       // Menu principal
    ├─> CharacterSelectScreen // Sélection perso
    ├─> GameScreen            // Boucle de jeu principale
    ├─> PauseScreen           // Pause (overlay)
    ├─> InventoryScreen       // Inventaire (overlay)
    └─> GameOverScreen        // Écran de mort
```

Utiliser l'interface `Screen` de libGDX avec un `Game` comme gestionnaire central.

### 5.2 Entity Component System — Ashley

Tous les objets du jeu (joueur, ennemis, projectiles, items) sont des `Entity` Ashley.

**Composants principaux :**

| Composant | Rôle |
|-----------|------|
| `TransformComponent` | Position (x, y), rotation |
| `VelocityComponent` | Vitesse (vx, vy) |
| `RenderComponent` | Sprite / TextureRegion à afficher |
| `CollisionComponent` | Hitbox Box2D fixture |
| `HealthComponent` | PV actuels / max |
| `CombatComponent` | ATK, MAG, DEF, SPD |
| `PlayerInputComponent` | Tag : entité contrôlée par le joueur |
| `EnemyAIComponent` | État IA (idle, chase, attack, flee) |
| `ProjectileComponent` | Dégâts, portée, direction |
| `ItemComponent` | Données de l'item (type, rareté, stats) |
| `EquipmentComponent` | Slots d'équipement actifs |

**Systèmes Ashley :**

| Système | Rôle |
|---------|------|
| `MovementSystem` | Applique vélocité → position |
| `PlayerInputSystem` | Lit clavier/souris → vélocité + actions |
| `EnemyAISystem` | Pathfinding, décisions ennemis |
| `CombatSystem` | Gère attaques, dégâts, mort |
| `ProjectileSystem` | Déplacement et cycle de vie des projectiles |
| `RenderSystem` | Tri et dessin des sprites (SpriteBatch) |
| `CollisionSystem` | Intégration Box2D, résolution collisions |
| `AnimationSystem` | Mise à jour des StateTime d'animation |

### 5.3 Box2D — Physique & Collisions

- **Monde Box2D** : `World(new Vector2(0, 0), true)` (pas de gravité, top-down).
- **Bodies** :
    - Joueur : `DYNAMIC`
    - Ennemis : `DYNAMIC`
    - Murs / tiles solides : `STATIC`
    - Projectiles : `DYNAMIC`, sensor activé
- **ContactListener** : détecte projectile→ennemi, joueur→item, joueur→porte.
- **Unités** : 1 mètre Box2D = 1 tile (ex. 16 px). Utiliser un `PPM` constant.

### 5.4 Rendu du donjon (TileMap maison)

Sans fichier `.tmx`, générer le donjon directement en mémoire :

```java
// Structure de données
int[][] tileGrid; // 0 = vide, 1 = mur, 2 = sol, 3 = porte...

// Rendu via SpriteBatch + TextureRegion
// Dessiner uniquement les tiles visibles à l'écran (culling)
```

Ou utiliser `TiledMap` libGDX avec génération procédurale en remplissant les layers en code.

### 5.5 Caméra

```java
OrthographicCamera camera = new OrthographicCamera();
// Suit le joueur en douceur (lerp)
camera.position.lerp(playerPosition, 0.1f);
```

---

## 6. Interface utilisateur (Scene2D / HUD)

### 6.1 HUD en jeu

- Barre de vie (ProgressBar)
- Icônes des 3 capacités avec cooldown
- Minimap (optionnel)
- Indicateur niveau actuel

### 6.2 Inventaire (overlay Scene2D)

- Fenêtre `Window` Scene2D
- Grille d'items avec `ImageButton`
- Panel d'équipement avec slots dédiés
- Tooltip sur survol d'un item

### 6.3 Skin UI

Utiliser le skin existant `uiskin.json` déjà présent dans le projet.

---

## 7. Fichiers & Organisation du projet

```
core/src/main/java/adfinir/game/
    ├── Main.java                    // ApplicationAdapter principal
    ├── screens/
    │   ├── MainMenuScreen.java
    │   ├── GameScreen.java
    │   ├── GameOverScreen.java
    │   └── PauseScreen.java
    ├── ecs/
    │   ├── components/              // Tous les composants Ashley
    │   └── systems/                 // Tous les systèmes Ashley
    ├── dungeon/
    │   ├── DungeonGenerator.java    // Génération procédurale
    │   ├── Room.java
    │   └── Tile.java
    ├── entities/
    │   ├── PlayerFactory.java       // Crée l'entité joueur
    │   ├── EnemyFactory.java
    │   └── ItemFactory.java
    ├── combat/
    │   ├── AttackResolver.java
    │   └── AbilitySystem.java
    └── ui/
        ├── HUD.java
        └── InventoryUI.java
```

---

## 8. Roadmap de développement

### Phase 1 — Fondations
- [ ] Mise en place des Screens (Menu, Game, GameOver)
- [ ] Rendu d'une grille de tiles statique
- [ ] Joueur avec mouvement fluide (Ashley + Box2D)

### Phase 2 — Donjon
- [ ] Algorithme de génération procédurale (salles + couloirs)
- [ ] Collisions murs/joueur via Box2D
- [ ] Caméra qui suit le joueur

### Phase 3 — Combat
- [ ] Attaque mêlée (hitbox temporaire)
- [ ] Projectiles (arc / magie)
- [ ] Ennemis basiques avec IA simple (chase + attack)
- [ ] Système de PV + mort

### Phase 4 — Contenu
- [ ] Inventaire & équipement
- [ ] Génération de loot dans les salles
- [ ] Plusieurs niveaux de donjon
- [ ] Boss de fin de niveau

### Phase 5 — Polish
- [ ] Animations sprites
- [ ] Effets sonores & musique
- [ ] HUD complet
- [ ] Équilibrage difficulté
