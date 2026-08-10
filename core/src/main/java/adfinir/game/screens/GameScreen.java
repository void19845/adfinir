package adfinir.game.screens;

import adfinir.game.Main;
import adfinir.game.dungeon.DungeonGenerator;
import adfinir.game.dungeon.DungeonMap;
import adfinir.game.dungeon.DungeonRenderer;
import adfinir.game.ecs.components.CombatComponent;
import adfinir.game.ecs.components.EnemyAIComponent;
import adfinir.game.ecs.components.EnemyStatsComponent;
import adfinir.game.ecs.components.LootBarComponent;
import adfinir.game.ecs.components.LootComponent;
import adfinir.game.ecs.components.PlayerInputComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import adfinir.game.ecs.components.RenderComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.ecs.components.VelocityComponent;
import adfinir.game.ecs.systems.CombatSystem;
import adfinir.game.ecs.systems.DeathSystem;
import adfinir.game.ecs.systems.EnemyAttackSystem;
import adfinir.game.ecs.systems.EnemyMovementSystem;
import adfinir.game.ecs.systems.LootPickupSystem;
import adfinir.game.ecs.systems.MovementSystem;
import adfinir.game.ecs.systems.PlayerInputSystem;
import adfinir.game.ecs.systems.RenderSystem;
import adfinir.game.ecs.systems.StatsSystem;
import adfinir.game.inventory.ItemGenerator;
import adfinir.game.ecs.components.InventoryComponent;
import adfinir.game.inventory.Weapon;

import adfinir.game.save.SaveData;
import adfinir.game.save.SaveManager;
import adfinir.game.ui.MiniMap;
import adfinir.game.ui.InventoryOverlay;
import adfinir.game.ui.LootBarOverlay;
import adfinir.game.ui.ShopOverlay;
import adfinir.game.ui.SocketInteractionState;
import adfinir.game.ui.StatsOverlay;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

public class GameScreen implements Screen {

    // Taille de vue de base — ExtendViewport agrandit cette zone
    // pour remplir l'écran sans étirement ni bandes noires
    private static final int VIEW_W = 320;
    private static final int VIEW_H = 240;

    /** Augmentation du threatFactor par étage descendu (+20% par étage). */
    private static final float THREAT_STEP = 0.2f;

    private final Main game;

    private ExtendViewport      viewport;
    private OrthographicCamera  camera;
    private ShapeRenderer       shapeRenderer;

    private DungeonMap      dungeonMap;
    private DungeonRenderer dungeonRenderer;

    private Engine               engine;
    private Entity               player;
    private TransformComponent   playerTransform;
    private PlayerStatsComponent playerStats;

    private StatsOverlay statsOverlay;
    private InventoryOverlay inventoryOverlay;
    private LootBarOverlay lootBarOverlay;
    private ShopOverlay shopOverlay;
    private SocketInteractionState socketInteraction;
    private MiniMap      miniMap;
    private OrthographicCamera uiCamera;
    private int screenW, screenH;

    // --- Habillage visuel (n'affecte aucune logique de jeu) ---
    /** 1 = écran couvert de noir (fondu d'entrée / changement d'étage), 0 = normal. */
    private float transitionAlpha = 1f;
    /** Flash rouge bref à la réception de dégâts. */
    private float damageFlashAlpha = 0f;
    /** Pulsation rouge continue quand les PV sont bas. */
    private float lowHpPulse = 0f;
    private float previousHp = -1f;
    private float totalTime = 0f;

    /** Étage courant (1 = premier étage). Détermine le threatFactor. */
    private int currentLevel = 1;

    /** Sauvegarde à restaurer au premier show(), ou null pour une nouvelle partie. */
    private SaveData pendingLoad;

    /** Famille stable (ne dépend pas de l'étage) utilisée par CombatSystem pour cibler les ennemis. */
    private Family enemyFamily;

    /** Nouvelle partie. */
    public GameScreen(Main game) {
        this(game, null);
    }

    /** Reprend une partie sauvegardée si saveToLoad != null, sinon nouvelle partie. */
    public GameScreen(Main game, SaveData saveToLoad) {
        this.game = game;
        this.pendingLoad = saveToLoad;
    }

    @Override
    public void show() {
        camera   = new OrthographicCamera();
        // ExtendViewport : garantit qu'on voit AU MOINS VIEW_W x VIEW_H
        // et étend la vue pour couvrir le reste — pas de bandes noires, pas d'étirement
        viewport = new ExtendViewport(VIEW_W, VIEW_H, camera);

        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setAutoShapeType(true);

        generateFloor(true);
    }

    /** Multiplicateur appliqué aux stats/dégâts/vitesse des ennemis et à la qualité du loot. */
    private float getThreatFactor() {
        return 1f + (currentLevel - 1) * THREAT_STEP;
    }

    /**
     * Génère (ou régénère) l'étage courant.
     *
     * firstFloor = true  : construit l'Engine et le joueur (appel initial depuis show()).
     * firstFloor = false : conserve le joueur, ses stats et son équipement (sauvegarde
     *                      de la progression), nettoie les ennemis/loot de l'étage
     *                      précédent, et repositionne le joueur sur le nouveau spawn.
     */
    private void generateFloor(boolean firstFloor) {
        if (firstFloor && pendingLoad != null) {
            // L'étage + le layout sauvegardés pilotent la reconstruction du 1er floor.
            currentLevel = pendingLoad.currentLevel;
            dungeonMap = SaveManager.toDungeonMap(pendingLoad);
        } else {
            DungeonGenerator generator = new DungeonGenerator(50, 40);
            dungeonMap = generator.generate();
        }
        dungeonRenderer = new DungeonRenderer(dungeonMap);

        float threatFactor = getThreatFactor();

        if (firstFloor) {
            enemyFamily = Family.all(EnemyStatsComponent.class, TransformComponent.class).get();

            engine = new Engine();
            engine.addSystem(new StatsSystem());
            engine.addSystem(new PlayerInputSystem(enemyFamily));
            engine.addSystem(new MovementSystem(dungeonMap));
            engine.addSystem(new RenderSystem(shapeRenderer));

            player          = new Entity();
            playerTransform = new TransformComponent();
            if (pendingLoad != null) {
                // Position exacte restaurée : le layout est identique à celui sauvegardé.
                playerTransform.x = pendingLoad.playerX;
                playerTransform.y = pendingLoad.playerY;
            } else {
                playerTransform.x = dungeonMap.getSpawnPixelX();
                playerTransform.y = dungeonMap.getSpawnPixelY();
            }

            VelocityComponent    playerVel   = new VelocityComponent();
            RenderComponent      playerRender = new RenderComponent();
            playerRender.color  = new Color(0.2f, 0.7f, 1.0f, 1f);
            playerRender.width  = 12f;
            playerRender.height = 12f;

            PlayerInputComponent playerInput = new PlayerInputComponent();
            playerInput.speed = 80f;

            playerStats = new PlayerStatsComponent();
            CombatComponent playerCombat = new CombatComponent();

            InventoryComponent inventory;
            if (pendingLoad != null) {
                inventory = SaveManager.toInventory(pendingLoad);
            } else {
                inventory = new InventoryComponent();
                inventory.equipWeapon(ItemGenerator.generateWeapon());
                inventory.equipArmor(ItemGenerator.generateArmor());
                inventory.equipCapacity(ItemGenerator.generateCapacity());
                inventory.equipArtifact(ItemGenerator.generateArtifact());
            }

            // Synchronise les stats avec l'équipement (généré ou restauré)
            inventory.updateStats(playerStats.stats);

            LootBarComponent lootBar;
            if (pendingLoad != null) {
                // Restaure les PV/Stamina sauvegardés, bornés au maximum actuel
                // (au cas où l'équipement rechargé donnerait un max différent).
                playerStats.currentHp      = Math.min(pendingLoad.currentHp, playerStats.stats.maxHp());
                playerStats.currentStamina = Math.min(pendingLoad.currentStamina, playerStats.stats.maxStamina());
                playerStats.gold           = pendingLoad.gold;
                lootBar = SaveManager.toLootBar(pendingLoad);
                pendingLoad = null; // sauvegarde consommée
            } else {
                lootBar = new LootBarComponent();
            }

            playerCombat.weapon = inventory.weapon;

            player.add(playerTransform);
            player.add(playerVel);
            player.add(playerRender);
            player.add(playerInput);
            player.add(playerStats);
            player.add(playerCombat);
            player.add(inventory);
            player.add(lootBar);
            engine.addEntity(player);
            engine.addSystem(new CombatSystem(player, enemyFamily));
            engine.addSystem(new DeathSystem(player));

            statsOverlay = new StatsOverlay();
            inventoryOverlay = new InventoryOverlay();
            lootBarOverlay = new LootBarOverlay();
            shopOverlay = new ShopOverlay();
            socketInteraction = new SocketInteractionState();
            miniMap      = new MiniMap();
            uiCamera     = new OrthographicCamera();
        } else {
            // Le joueur, ses stats et son inventaire restent inchangés : seule
            // sa position est réinitialisée sur le spawn du nouvel étage.
            playerTransform.x = dungeonMap.getSpawnPixelX();
            playerTransform.y = dungeonMap.getSpawnPixelY();

            clearFloorEntities();
            replaceSystem(MovementSystem.class, new MovementSystem(dungeonMap));
        }

        // Systèmes dépendants de la carte / des entités de l'étage : toujours reconstruits
        replaceSystem(EnemyMovementSystem.class, new EnemyMovementSystem(dungeonMap, player));
        replaceSystem(EnemyAttackSystem.class, new EnemyAttackSystem(player));
        replaceSystem(LootPickupSystem.class,
            new LootPickupSystem(player, player.getComponent(LootBarComponent.class)));

        spawnEnemiesForFloor(threatFactor);
        spawnLootForFloor(threatFactor);
    }

    /** Remplace un système existant par une nouvelle instance (ex : dépendante de la nouvelle carte). */
    private <T extends EntitySystem> void replaceSystem(Class<T> type, T newSystem) {
        T existing = engine.getSystem(type);
        if (existing != null) {
            engine.removeSystem(existing);
        }
        engine.addSystem(newSystem);
    }

    /** Retire les ennemis et objets de loot restants de l'étage précédent. */
    private void clearFloorEntities() {
        Array<Entity> toRemove = new Array<>();
        for (Entity e : engine.getEntitiesFor(Family.one(EnemyStatsComponent.class, LootComponent.class).get())) {
            toRemove.add(e);
        }
        for (Entity e : toRemove) {
            engine.removeEntity(e);
        }
    }

    private void spawnEnemiesForFloor(float threatFactor) {
        int enemyCount = 5 + (currentLevel - 1); // un peu plus d'ennemis par étage
        for (int i = 0; i < enemyCount; i++) {
            com.badlogic.gdx.math.Vector2 pos = dungeonMap.getRandomFloorPosition();
            spawnEnemy(pos.x, pos.y, threatFactor);
        }
    }

    private void spawnEnemy(float x, float y, float threatFactor) {
        Entity enemy = new Entity();

        TransformComponent transform = new TransformComponent();
        transform.x = x;
        transform.y = y;

        VelocityComponent vel = new VelocityComponent();

        RenderComponent render = new RenderComponent();
        render.color = new Color(1.0f, 0.2f, 0.2f, 1f); // Rouge pour les ennemis
        render.width = 12f;
        render.height = 12f;

        EnemyStatsComponent stats = new EnemyStatsComponent();
        stats.applyThreatFactor(threatFactor); // HP / DEF / dégâts d'attaque montent avec l'étage

        EnemyAIComponent ai = new EnemyAIComponent();

        // Vitesse random mais mise à l'échelle par le threatFactor, bornée pour rester jouable
        ai.speed = com.badlogic.gdx.math.MathUtils.random(20f, 50f) * threatFactor;
        ai.pursuitSpeed = Math.min(ai.speed * 1.2f, 140f);
        ai.detectionRange = com.badlogic.gdx.math.MathUtils.random(80f, 150f);

        CombatComponent combat = new CombatComponent();

        enemy.add(transform);
        enemy.add(vel);
        enemy.add(render);
        enemy.add(stats);
        enemy.add(ai);
        enemy.add(combat);

        engine.addEntity(enemy);
    }

    /** Parcourt la carte et fait apparaître un objet au sol sur chaque tile TILE_LOOT. */
    private void spawnLootForFloor(float threatFactor) {
        int ts = DungeonMap.TILE_SIZE;
        for (int r = 0; r < dungeonMap.rows; r++) {
            for (int c = 0; c < dungeonMap.cols; c++) {
                if (dungeonMap.getTile(c, r) == DungeonMap.TILE_LOOT) {
                    spawnLoot(c * ts + ts / 2f, r * ts + ts / 2f, threatFactor);
                }
            }
        }
    }

    private void spawnLoot(float x, float y, float threatFactor) {
        Entity lootEntity = new Entity();

        TransformComponent transform = new TransformComponent();
        transform.x = x;
        transform.y = y;

        RenderComponent render = new RenderComponent();
        render.color = new Color(1.0f, 0.85f, 0.2f, 1f); // Doré
        render.width = 8f;
        render.height = 8f;

        LootComponent loot = new LootComponent();
        LootComponent.LootType[] types = LootComponent.LootType.values();
        loot.type = types[com.badlogic.gdx.math.MathUtils.random(types.length - 1)];
        loot.threatFactor = threatFactor;

        lootEntity.add(transform);
        lootEntity.add(render);
        lootEntity.add(loot);

        engine.addEntity(lootEntity);
    }

    /** Appelé quand le joueur atteint la tile de sortie (case verte). */
    private void goToNextFloor() {
        transitionAlpha = 1f; // fondu au noir le temps de générer le nouvel étage
        currentLevel++;
        generateFloor(false);
        // Recentre immédiatement la caméra pour éviter un panoramique à travers l'ancien étage
        camera.position.set(playerTransform.x, playerTransform.y, 0);
        camera.update();
        // Checkpoint : sauvegarde automatique à chaque nouvel étage
        SaveManager.save(currentLevel, dungeonMap, playerTransform, playerStats,
            player.getComponent(InventoryComponent.class),
            player.getComponent(LootBarComponent.class));
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (inventoryOverlay.isHoldingMod()) {
                inventoryOverlay.cancelHeld();
                return;
            }
            SaveManager.save(currentLevel, dungeonMap, playerTransform, playerStats,
                player.getComponent(InventoryComponent.class),
                player.getComponent(LootBarComponent.class));
            game.setScreen(new MainMenuScreen(game));
            dispose();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            statsOverlay.toggle();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            inventoryOverlay.toggle();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            shopOverlay.toggle();
        }
        if (shopOverlay.isVisible()) {
            shopOverlay.update(delta, player.getComponent(LootBarComponent.class), playerStats);
        }

        totalTime += delta;
        transitionAlpha = Math.max(0f, transitionAlpha - delta / 0.6f);
        damageFlashAlpha = Math.max(0f, damageFlashAlpha - delta * 1.8f);

        float hpRatio = playerStats.stats.maxHp() > 0
            ? playerStats.currentHp / (float) playerStats.stats.maxHp() : 1f;
        lowHpPulse = hpRatio < 0.25f
            ? 0.12f + 0.13f * (MathUtils.sin(totalTime * 6f) + 1f) / 2f
            : 0f;

        lootBarOverlay.update(player.getComponent(LootBarComponent.class),
            player.getComponent(InventoryComponent.class),
            player.getComponent(CombatComponent.class),
            playerStats,
            socketInteraction);
        lootBarOverlay.handleInput();

        inventoryOverlay.update(player.getComponent(InventoryComponent.class),
            player.getComponent(LootBarComponent.class),
            playerStats.stats,
            socketInteraction);
        inventoryOverlay.handleInput();

        engine.getSystem(StatsSystem.class).update(delta);
        engine.getSystem(CombatSystem.class).update(delta);
        engine.getSystem(PlayerInputSystem.class).update(delta);
        engine.getSystem(EnemyMovementSystem.class).update(delta);
        engine.getSystem(EnemyAttackSystem.class).update(delta);
        engine.getSystem(MovementSystem.class).update(delta);
        engine.getSystem(LootPickupSystem.class).update(delta);
        engine.getSystem(DeathSystem.class).update(delta);

        // Détecte une perte de PV pour déclencher un flash d'impact à l'écran
        if (previousHp < 0f) {
            previousHp = playerStats.currentHp;
        } else if (playerStats.currentHp < previousHp - 0.01f) {
            damageFlashAlpha = 0.5f;
        }
        previousHp = playerStats.currentHp;

        // Mort du joueur → écran de game over (permadeath : la sauvegarde est effacée)
        if (playerStats.isDead) {
            SaveManager.deleteSave();
            game.setScreen(new GameOverScreen(game, currentLevel));
            dispose();
            return;
        }

        // Détecte l'arrivée sur la case de sortie (verte) → étage suivant
        int playerCol = (int) (playerTransform.x / DungeonMap.TILE_SIZE);
        int playerRow = (int) (playerTransform.y / DungeonMap.TILE_SIZE);
        if (dungeonMap.getTile(playerCol, playerRow) == DungeonMap.TILE_EXIT) {
            goToNextFloor();
        }

        // Clamp caméra en tenant compte de la vue étendue
        float halfW = camera.viewportWidth  / 2f;
        float halfH = camera.viewportHeight / 2f;
        float camTargetX = MathUtils.clamp(playerTransform.x,
            halfW,  dungeonMap.getPixelWidth()  - halfW);
        float camTargetY = MathUtils.clamp(playerTransform.y,
            halfH, dungeonMap.getPixelHeight() - halfH);

        camera.position.x += (camTargetX - camera.position.x) * 6f * delta;
        camera.position.y += (camTargetY - camera.position.y) * 6f * delta;
        camera.update();

        // Teinte de fond légèrement plus rouge à mesure que le danger augmente avec l'étage
        float danger = MathUtils.clamp((currentLevel - 1) * 0.03f, 0f, 0.16f);
        Gdx.gl.glClearColor(danger, 0f, 0.01f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Rectangle viewRect = new Rectangle(
            camera.position.x - camera.viewportWidth  / 2f,
            camera.position.y - camera.viewportHeight / 2f,
            camera.viewportWidth,
            camera.viewportHeight
        );
        dungeonRenderer.render(shapeRenderer, viewRect);
        engine.getSystem(RenderSystem.class).update(delta);
        shapeRenderer.end();

        inventoryOverlay.draw();

        lootBarOverlay.draw();

        shopOverlay.draw(screenW, screenH, player.getComponent(LootBarComponent.class), playerStats);

        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        miniMap.draw(shapeRenderer, dungeonMap, playerTransform, screenW, screenH);

        statsOverlay.update(playerStats, delta);
        statsOverlay.draw();

        drawScreenOverlays();
    }

    /** Fondu de transition, flash de dégâts et pulsation "PV bas" — habillage écran-entier, sans logique de jeu. */
    private void drawScreenOverlays() {
        if (transitionAlpha <= 0.001f && damageFlashAlpha <= 0.001f && lowHpPulse <= 0.001f) {
            return;
        }
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (transitionAlpha > 0.001f) {
            shapeRenderer.setColor(0f, 0f, 0f, transitionAlpha);
            shapeRenderer.rect(0, 0, screenW, screenH);
        }
        if (lowHpPulse > 0.001f) {
            shapeRenderer.setColor(0.6f, 0f, 0f, lowHpPulse);
            shapeRenderer.rect(0, 0, screenW, screenH);
        }
        if (damageFlashAlpha > 0.001f) {
            shapeRenderer.setColor(0.85f, 0.05f, 0.05f, damageFlashAlpha);
            shapeRenderer.rect(0, 0, screenW, screenH);
        }

        shapeRenderer.end();
    }

    @Override
    public void resize(int w, int h) {
        screenW = w;
        screenH = h;
        viewport.update(w, h, true);
        statsOverlay.resize(w, h);
        inventoryOverlay.resize(w, h);
        lootBarOverlay.resize(w, h);
        shopOverlay.resize(w, h);
        uiCamera.viewportWidth = w;
        uiCamera.viewportHeight = h;
        uiCamera.position.set(w / 2f, h / 2f, 0);
        uiCamera.update();
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        statsOverlay.dispose();
        inventoryOverlay.dispose();
        lootBarOverlay.dispose();
        shopOverlay.dispose();
    }
}
