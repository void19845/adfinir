# CONTEXTE PROJET — ADFINIR

> Généré à partir de l'archive `game.zip` (39 fichiers `.java`, ~2830 lignes).
> Ce document sert de point de repère pour comprendre rapidement l'état du projet.

## 1. Vue d'ensemble

**ADFINIR** est un jeu d'action-RPG vu du dessus (top-down ARPG / dungeon crawler),
écrit en **Java** avec le framework **LibGDX** et le moteur d'entités **Ashley (ECS)**.

- Package racine : `adfinir.game`
- Génération procédurale de donjons (BSP)
- Combat en temps réel avec combos d'armes et formes d'attaque (cône, arc, rectangle)
- Système d'équipement à 4 slots (Arme / Capacité / Armure / Artefact) avec rareté
- IA ennemie avec poursuite par pathfinding (BFS)
- Overlays UI (stats, inventaire, mini-carte)

**Aucun fichier de build n'est présent** dans l'archive (pas de `build.gradle`,
`pom.xml`, ni dossier `assets/`) — seul le code source `game/` a été fourni.

## 2. Stack technique

| Élément | Détail |
|---|---|
| Langage | Java |
| Framework jeu | LibGDX (`com.badlogic.gdx.*`) |
| ECS | Ashley (`com.badlogic.ashley.*`) |
| Rendu | `ShapeRenderer` (formes colorées, pas encore de sprites sauf armes) |
| UI | Scene2D (menus) + dessin natif SpriteBatch/BitmapFont (overlays stats/inventaire) |

## 3. Arborescence indexée

```
game/
├── Main.java                     Point d'entrée (Game), lance MainMenuScreen
├── GameMap.java                  ⚠️ INUTILISÉ — ancienne carte ASCII, remplacée par dungeon/
│
├── dungeon/
│   ├── DungeonGenerator.java     Génération procédurale par BSP (salles + couloirs en L)
│   ├── DungeonMap.java           Grille de tiles (mur/sol/sortie), conversion pixel↔tile
│   └── DungeonRenderer.java      Dessin des tiles visibles (culling par viewport)
│
├── ecs/
│   ├── components/
│   │   ├── CombatComponent.java        État de combat (arme, combo, cooldown, direction)
│   │   ├── EnemyAIComponent.java       État IA (IDLE/PURSUING), vitesse, détection
│   │   ├── EnemyStatsComponent.java    HP/DEF ennemi (Poolable)
│   │   ├── InventoryComponent.java     4 slots d'équipement + recalcul des bonus de stats
│   │   ├── PlayerInputComponent.java   Vitesse + dernière direction (Poolable)
│   │   ├── PlayerStatsComponent.java   HP/Stamina dynamiques + délégation vers StatSheet
│   │   ├── RenderComponent.java        Taille + couleur de rendu (Poolable)
│   │   ├── TransformComponent.java     Position x/y/rotation (Poolable)
│   │   └── VelocityComponent.java      Vecteur vitesse vx/vy (Poolable)
│   │
│   └── systems/
│       ├── CombatSystem.java           Cooldowns, fenêtre d'attaque active, applyDamage()
│       ├── DeathSystem.java            ⚠️ Retire les ennemis morts — JAMAIS APPELÉ (voir §6)
│       ├── EnemyMovementSystem.java    IA ennemie (poursuite BFS / marche aléatoire) — ⚠️ JAMAIS APPELÉ
│       ├── MovementSystem.java         Applique la vélocité + collisions AABB avec la carte
│       ├── PlayerInputSystem.java      Clavier (ZQSD/flèches), attaque, switch d'arme (X)
│       ├── RenderSystem.java           Dessine entités + hitbox d'attaque selon AttackShape
│       └── StatsSystem.java            Régénère la stamina, synchronise SPD → vitesse input
│
├── inventory/
│   ├── Item.java                 Classe abstraite de base (nom, rareté, bonus de stats)
│   ├── Armor.java                Bonus HP/DEF/SPD selon ArmorType + rareté
│   ├── ArmorType.java            LIGHT / MEDIUM / HEAVY (modificateurs vitesse/défense/HP)
│   ├── Artifact.java             Bonus passif unique (logique simplifiée via Consumer)
│   ├── Capacity.java             Capacité active modulaire façon "Noita" (effet + modificateurs)
│   ├── CapacityEffect.java       Effet de base (dégâts, vitesse, rayon, élément)
│   ├── CapacityModifier.java     BOUNCE / DUPLICATE / ARC / EXPLOSION / RICOCHET / SPEED_UP
│   ├── Weapon.java               Arme avec liste de combos (comboSlots)
│   ├── WeaponAttack.java         Une attaque du combo (dégâts min/max, cooldown, durée, AoE)
│   ├── WeaponType.java           SPEAR / SWORD / CLAYMORE (modificateurs + forme d'attaque)
│   ├── WeaponSpriteManager.java  Charge et découpe le spritesheet des armes (3 régions)
│   ├── Rarity.java                COMMON/RARE/EPIC/LEGENDARY — utilisée partout dans inventory/
│   └── ItemGenerator.java        Génération procédurale (armes, capacités, armures, artefacts)
│
├── player/
│   ├── StatSheet.java             Stats de base + bonus d'items (EnumMap<StatType,Float>)
│   ├── StatType.java              MAX_HP, ATK, MAG, DEF, SPD, MAX_STAMINA, STAMINA_REGEN
│   ├── AttackShape.java           CONE / ARC / RECTANGLE (forme des attaques)
│   └── Rarity.java                ⚠️ DUPLIQUÉ / INUTILISÉ — enum différent de inventory.Rarity
│
├── screens/
│   ├── MainMenuScreen.java       Menu principal (Scene2D, boutons Jouer/Quitter)
│   ├── GameScreen.java           Écran de jeu principal : init ECS, boucle de rendu, caméra
│   └── GameOverScreen.java       Écran de fin (Rejouer / Menu principal)
│
├── ui/
│   ├── StatsOverlay.java         Overlay PV/Stamina/ATK/MAG/DEF/SPD (touche K)
│   ├── InventoryOverlay.java     Overlay des 4 slots d'équipement (touche E)
│   └── MiniMap.java              Mini-carte en haut à droite (murs + position joueur)
│
└── util/
    └── Pathfinding.java          BFS pour trouver la prochaine étape vers une cible
```

## 4. Architecture & flux de jeu

- `Main` (Game) → `MainMenuScreen` → `GameScreen` (ou `GameOverScreen`).
- `GameScreen.show()` construit un `Engine` Ashley, génère un donjon (`DungeonGenerator`,
  50×40 tiles), crée l'entité joueur avec tous ses composants, équipe un stuff aléatoire
  via `ItemGenerator`, puis spawn 5 ennemis sur des cases de sol aléatoires.
- **Le rendu du monde** passe par `ShapeRenderer` (pas de sprites pour l'instant, sauf les
  armes affichées dans l'inventaire via `WeaponSpriteManager`).
- **La caméra** (`ExtendViewport`) suit le joueur avec un lissage exponentiel et est clampée
  aux limites du donjon.
- **Les stats** suivent un modèle base + bonus (`StatSheet`) : l'équipement recalculé par
  `InventoryComponent.updateStats()` réinjecte les bonus dans la `StatSheet` du joueur.

## 5. Points d'attention / bugs identifiés

1. **`EnemyMovementSystem` et `DeathSystem` ne tournent jamais.**
   Dans `GameScreen.render()`, seuls ces systèmes sont explicitement mis à jour :
   `StatsSystem`, `CombatSystem`, `PlayerInputSystem`, `MovementSystem`, puis `RenderSystem`
   séparément. `engine.update(delta)` global n'est jamais appelé, et
   `EnemyMovementSystem`/`DeathSystem` ne sont pas dans la liste manuelle. Conséquence probable :
   - les ennemis ne bougent pas (ni marche aléatoire, ni poursuite) ;
   - les ennemis morts ne sont jamais retirés du moteur (accumulation d'entités mortes).
   → À corriger en ajoutant ces deux `update(delta)` dans la boucle de rendu (dans le bon ordre
   de priorité : `EnemyMovementSystem` avant `MovementSystem`, `DeathSystem` en fin de frame).

2. **`GameMap.java` (racine `game/`) est du code mort.** Générateur de carte ASCII non
   référencé nulle part ; remplacé par `dungeon/DungeonMap.java`. Peut être supprimé.

3. **Doublon `Rarity` :** `adfinir.game.inventory.Rarity` (COMMON/RARE/EPIC/LEGENDARY,
   avec `statMultiplier`/`bonusPropertyCount`) est la version réellement utilisée partout.
   `adfinir.game.player.Rarity` (6 valeurs, aucun champ) n'est importée ni utilisée nulle part
   — probable reliquat d'une itération précédente.

4. **Assets référencés mais absents de l'archive :**
   - `ui/uiskin.json` (Scene2D skin, utilisé par `MainMenuScreen` et `GameOverScreen`)
   - `ui/ChatGPT Image 6 août 2026, 18_53_57.png` (spritesheet des armes, nom de fichier
     généré, chargé en dur dans `WeaponSpriteManager`)
   Sans ces fichiers dans `assets/`, le jeu ne compilera/lancera pas tel quel.

5. **`Pathfinding.findNextStep`** contient une boucle `while` vide (lignes mortes) avant la
   reconstruction du chemin — sans effet mais à nettoyer.

## 6. Contrôles (résumé UX)

| Touche | Action |
|---|---|
| ZQSD / flèches | Déplacement |
| Clic gauche / Espace | Attaque (combo selon `comboIndex`) |
| X | Changer d'arme (Épée → Lance → Claymore → …) |
| E | Ouvrir/fermer l'inventaire |
| K | Ouvrir/fermer les stats |
| Échap | Retour au menu principal |

## 7. Pistes de suite possibles

- Corriger le bug d'update des systèmes ECS (§5.1) — priorité haute, impacte le gameplay.
- Fournir les assets manquants (`uiskin.json`, spritesheet armes) ou adapter le code pour
  fonctionner sans (fallback sur formes/police par défaut déjà en place ailleurs).
- Remplacer `ShapeRenderer` par de vrais sprites pour joueur/ennemis/tiles.
- Nettoyer le code mort (`GameMap.java`, `player/Rarity.java`).
- Ajouter la progression de niveau (le champ `currentLevel` dans `GameScreen` n'est jamais
  incrémenté ni utilisé au-delà de sa déclaration).
