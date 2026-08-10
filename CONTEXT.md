# CONTEXTE PROJET — ADFINIR

> Généré à partir de l'archive `game.zip` (51 fichiers `.java`, ~4507 lignes).
> Ce document sert de point de repère pour comprendre rapidement l'état du projet.
> Mise à jour : ré-indexation complète après ajout du système de loot, du save/load
> avec layout, du game over et de la correction du dispatch ECS.

## 1. Vue d'ensemble

**ADFINIR** est un jeu d'action-RPG vu du dessus (top-down ARPG / dungeon crawler),
écrit en **Java** avec le framework **LibGDX** et le moteur d'entités **Ashley (ECS)**.

- Package racine : `adfinir.game`
- Génération procédurale de donjons (BSP), **non seedée**
- Combat en temps réel avec combos d'armes et formes d'attaque (cône, arc, rectangle)
- Système d'équipement à 4 slots (Arme / Capacité / Armure / Artefact) + **barre de loot à 8 slots**
- IA ennemie : poursuite par pathfinding BFS + **attaque de contact** (`EnemyAttackSystem`)
- Progression multi-étages avec `threatFactor` scalant stats ennemies et rareté du loot
- Sauvegarde/chargement complet (inventaire + **layout exact du donjon**), suppression au décès (permadeath)
- Overlays UI (stats, inventaire, mini-carte, barre de loot, détails d'objet)
- Écran de Game Over stylisé (Scene2D)

**Toujours aucun fichier de build** dans l'archive (pas de `build.gradle`, `pom.xml`,
ni dossier `assets/`) — seul le code source `game/` est fourni.

## 2. Stack technique

| Élément | Détail |
|---|---|
| Langage | Java |
| Framework jeu | LibGDX (`com.badlogic.gdx.*`) |
| ECS | Ashley (`com.badlogic.ashley.*`) |
| Rendu monde | `ShapeRenderer` (formes colorées ; sprites uniquement pour les armes en inventaire) |
| UI | Scene2D (menus/GameOver) + dessin natif SpriteBatch/BitmapFont/ShapeRenderer (overlays) |
| Sauvegarde | `com.badlogic.gdx.utils.Json` → `save.json` local, un seul slot |

## 3. Arborescence indexée

```
game/
├── Main.java                     Point d'entrée (Game), lance MainMenuScreen
├── GameMap.java                  ⚠️ TOUJOURS INUTILISÉ — ancienne carte ASCII (58 lignes), non supprimé
│
├── dungeon/
│   ├── DungeonGenerator.java     Génération procédurale par BSP (salles + couloirs en L)
│   ├── DungeonMap.java           Grille de tiles, conversion pixel↔tile ; constructeur additionnel
│   │                             (grid, spawnCol, spawnRow, exitCol, exitRow) pour reconstruction depuis save
│   └── DungeonRenderer.java      Dessin des tiles visibles (culling par viewport)
│
├── ecs/
│   ├── components/
│   │   ├── CombatComponent.java        État de combat (arme, combo, cooldown, direction)
│   │   ├── EnemyAIComponent.java       État IA (IDLE/PURSUING), vitesse, détection
│   │   ├── EnemyStatsComponent.java    HP/DEF/dégâts de contact ennemi (Poolable), scalé par threatFactor
│   │   ├── InventoryComponent.java     4 slots équipés + equipFromBar()/equipFromBarAndSync()
│   │   ├── LootBarComponent.java       🆕 Barre de 8 slots d'objets ramassés, en attente d'équipement
│   │   ├── LootComponent.java          🆕 Marque une entité loot au sol (type + threatFactor + item concret optionnel)
│   │   ├── PlayerInputComponent.java   Vitesse + dernière direction (Poolable)
│   │   ├── PlayerStatsComponent.java   HP/Stamina dynamiques + délégation vers StatSheet + isDead
│   │   ├── RenderComponent.java        Taille + couleur de rendu (Poolable)
│   │   ├── TransformComponent.java     Position x/y/rotation (Poolable)
│   │   └── VelocityComponent.java      Vecteur vitesse vx/vy (Poolable)
│   │
│   └── systems/
│       ├── CombatSystem.java           Cooldowns, fenêtre d'attaque active, applyDamage()
│       ├── DeathSystem.java            Retire les entités mortes — ✅ APPELÉ (voir §5)
│       ├── EnemyAttackSystem.java      🆕 Dégâts de contact ennemi → joueur (portée depuis EnemyStatsComponent)
│       ├── EnemyMovementSystem.java    IA ennemie (poursuite BFS / marche aléatoire) — ✅ APPELÉ
│       ├── LootPickupSystem.java       🆕 Ramassage auto vers LootBarComponent ; échange manuel [F] si barre pleine
│       ├── MovementSystem.java         Applique la vélocité + collisions AABB avec la carte
│       ├── PlayerInputSystem.java      Clavier (ZQSD/flèches), attaque, switch d'arme (X)
│       ├── RenderSystem.java           Dessine entités + hitbox d'attaque selon AttackShape
│       └── StatsSystem.java            Régénère la stamina, synchronise SPD → vitesse input
│
├── inventory/
│   ├── Item.java                 Classe abstraite de base (nom, rareté, bonus de stats)
│   ├── Armor.java / ArmorType.java / Artifact.java
│   ├── Capacity.java / CapacityEffect.java / CapacityModifier.java
│   ├── Weapon.java / WeaponAttack.java / WeaponType.java
│   ├── WeaponSpriteManager.java  Charge `ui/ChatGPT Image 6 août 2026, 18_53_57.png` (toujours en dur, absent)
│   ├── Rarity.java                COMMON/RARE/EPIC/LEGENDARY — seule version utilisée (le doublon dans
│   │                              `player/` a été supprimé, voir §5)
│   └── ItemGenerator.java        Génération procédurale + createArtifactById() (reconstruction save)
│
├── player/
│   ├── StatSheet.java             Stats de base + bonus d'items (EnumMap<StatType,Float>)
│   ├── StatType.java              MAX_HP, ATK, MAG, DEF, SPD, MAX_STAMINA, STAMINA_REGEN
│   └── AttackShape.java           CONE / ARC / RECTANGLE (forme des attaques)
│                                   (`player/Rarity.java` dupliqué : supprimé depuis la dernière indexation)
│
├── save/                          🆕 Package
│   ├── SaveData.java              DTO plat (primitifs/String) : niveau, HP/stamina, position joueur,
│   │                              **grille complète du donjon + spawn/exit**, et les 4 items équipés
│   └── SaveManager.java           save()/load()/deleteSave() ; JSON via com.badlogic.gdx.utils.Json ;
│                                  reconstruit Weapon/Armor/Capacity via constructeurs, Artifact via
│                                  ItemGenerator.createArtifactById() (lambda non sérialisable)
│
├── screens/
│   ├── MainMenuScreen.java       Menu principal (Scene2D), charge `ui/uiskin.json`, bouton Continuer si save
│   ├── GameScreen.java           Écran de jeu principal : init ECS, boucle de rendu, caméra, save/load
│   ├── GameOverScreen.java       Écran de fin stylisé (palette braises/sang), Rejouer / Menu principal
│   └── UiFx.java                 🆕 Utilitaires visuels runtime (Pixmap : panneaux arrondis, style boutons),
│                                  partagés par MainMenuScreen et GameOverScreen — package réel `adfinir.game.ui`
│
├── ui/
│   ├── StatsOverlay.java         Overlay PV/Stamina/ATK/MAG/DEF/SPD (touche K)
│   ├── InventoryOverlay.java     Overlay des 4 slots d'équipement (touche E)
│   ├── LootBarOverlay.java       🆕 Barre de 8 cercles en haut d'écran ; sélection [1-8]/clic, tooltip au survol
│   ├── ItemDetails.java          🆕 Construit les lignes de détail d'un item (stats/combos/capacité),
│   │                              factorisé entre InventoryOverlay et LootBarOverlay
│   └── MiniMap.java              Mini-carte en haut à droite (murs + position joueur)
│
└── util/
    └── Pathfinding.java          BFS + anti-corner-cutting — ✅ boucle morte nettoyée (voir §5)
```

**Note d'emplacement :** `UiFx.java` est physiquement dans `screens/` mais déclare
`package adfinir.game.ui;` — à corriger ou ignorer selon convention voulue.

## 4. Architecture & flux de jeu

- `Main` (Game) → `MainMenuScreen` (Nouvelle partie / **Continuer** si `SaveManager.saveExists()` / Quitter)
  → `GameScreen` (avec ou sans `SaveData` à restaurer) → `GameOverScreen` en cas de mort.
- `GameScreen.generateFloor(firstFloor)` :
  - `firstFloor = true` + save présente : reconstruit le `DungeonMap` **exact** via
    `SaveManager.toDungeonMap()` (grille sauvegardée) et restaure position/HP/stamina/équipement.
  - `firstFloor = true` sans save, ou `firstFloor = false` (étage suivant) : régénère un donjon
    50×40 via `DungeonGenerator` (aléatoire, non seedé).
- **Boucle de rendu (`GameScreen.render()`)** — dispatch manuel, ordre explicite :
  `StatsSystem → CombatSystem → PlayerInputSystem → EnemyMovementSystem → EnemyAttackSystem
  → MovementSystem → LootPickupSystem → DeathSystem`, puis `RenderSystem` séparément dans le bloc rendu.
  `engine.update(delta)` global n'est toujours pas utilisé — tout système ajouté doit être
  explicitement inséré ici pour s'exécuter.
- **Sauvegarde** : à chaque changement d'étage (`goToNextFloor()`) et à la sortie vers le menu (Échap).
  **Suppression** de la sauvegarde à la mort du joueur (`playerStats.isDead` → `SaveManager.deleteSave()`
  puis `GameOverScreen`) — permadeath effectif.
- **Loot** : les tiles `TILE_LOOT` du donjon font apparaître des entités `LootComponent`. Le ramassage
  remplit `LootBarComponent` (8 slots) via `LootPickupSystem` ; l'équipement effectif se fait ensuite
  par `InventoryComponent.equipFromBarAndSync()` (touches 1-8 ou clic sur `LootBarOverlay`), qui échange
  l'item avec l'équipement actif du même type et resynchronise `CombatComponent`/`StatSheet`.
- **Les stats** suivent toujours le modèle base + bonus (`StatSheet`) : `InventoryComponent.updateStats()`
  réinjecte les bonus armure/artefact dans la `StatSheet` du joueur.

## 5. Points corrigés depuis la dernière indexation

1. ✅ **Dispatch ECS complet.** `EnemyMovementSystem` et `DeathSystem` sont désormais appelés
   dans `GameScreen.render()`, avec `EnemyAttackSystem` et `LootPickupSystem` ajoutés au passage.
2. ✅ **`Pathfinding.findNextStep`** ne contient plus de boucle morte — code propre.
3. ✅ **`player/Rarity.java`** (doublon inutilisé) a été supprimé. `inventory/Rarity.java` reste
   la seule version.
4. ✅ **Persistance du layout.** Contrairement à la contrainte précédente ("non seedé → layout non
   sauvegardable"), `SaveData` sérialise désormais la grille complète (`dungeonTiles[][]`) + spawn/exit,
   ce qui permet de restaurer position et étage exacts au chargement. Limite documentée dans
   `SaveData` : ennemis et loot déjà traités ne sont pas sauvegardés — ils réapparaissent sur les
   tiles `TILE_LOOT` au rechargement, comme un étage neuf.
5. ✅ **Suppression de save au décès** implémentée (`DeathSystem` + `GameScreen.render()` +
   `SaveManager.deleteSave()` + `GameOverScreen`).

## 6. Points d'attention / bugs restants

1. **`GameMap.java` (racine `game/`) toujours présent.** Code mort non référencé (58 lignes),
   remplacé par `dungeon/DungeonMap.java`. Suppression toujours en attente.
2. **Assets référencés mais absents de l'archive :**
   - `ui/uiskin.json` (Scene2D skin — `MainMenuScreen` **et** `GameOverScreen`)
   - `ui/ChatGPT Image 6 août 2026, 18_53_57.png` (spritesheet armes, chargé en dur dans
     `WeaponSpriteManager`)
   Sans `assets/`, le jeu ne compile/lance pas tel quel.
3. **`UiFx.java` mal rangé** : fichier physiquement dans `screens/`, package déclaré `adfinir.game.ui`.
   À déplacer dans `ui/` par cohérence (ou accepter le multi-package si volontaire).
4. **Pas de fichier de build** (`build.gradle`/`pom.xml`) toujours absent de l'archive.
5. **Limite de save connue** (documentée dans `SaveData.java`) : ennemis/loot en cours ne sont pas
   persistés — comportement accepté, pas un bug à corriger sauf changement de design voulu.

## 7. Contrôles (résumé UX)

| Touche | Action |
|---|---|
| ZQSD / flèches | Déplacement |
| Clic gauche / Espace | Attaque (combo selon `comboIndex`) |
| X | Changer d'arme (Épée → Lance → Claymore → …) |
| E | Ouvrir/fermer l'inventaire |
| K | Ouvrir/fermer les stats |
| 1-8 / clic | Sélectionner et équiper un item de la barre de loot |
| F | Échanger l'item au sol avec l'item sélectionné (barre de loot pleine) |
| Échap | Sauvegarder et retour au menu principal |

## 8. Pistes de suite possibles

- Supprimer le code mort restant (`GameMap.java`).
- Fournir les assets manquants (`uiskin.json`, spritesheet armes) ou fallback sans assets.
- Corriger l'emplacement/package de `UiFx.java`.
- Ajouter un fichier de build (`build.gradle`) pour rendre le projet compilable en l'état.
- Remplacer `ShapeRenderer` par de vrais sprites pour joueur/ennemis/tiles.
- Décider si le loot/les ennemis en cours doivent être persistés (actuellement non, par design documenté).
