package adfinir.game.screens;

import adfinir.game.Main;
import adfinir.game.dungeon.DungeonGenerator;
import adfinir.game.dungeon.DungeonMap;
import adfinir.game.dungeon.DungeonRenderer;
import adfinir.game.ecs.components.CombatComponent;
import adfinir.game.ecs.components.EnemyAIComponent;
import adfinir.game.ecs.components.EnemyStatsComponent;
import adfinir.game.ecs.components.PlayerInputComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import adfinir.game.ecs.components.RenderComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.ecs.components.VelocityComponent;
import adfinir.game.ecs.systems.CombatSystem;
import adfinir.game.ecs.systems.DeathSystem;
import adfinir.game.ecs.systems.EnemyMovementSystem;
import adfinir.game.ecs.systems.MovementSystem;
import adfinir.game.ecs.systems.PlayerInputSystem;
import adfinir.game.ecs.systems.RenderSystem;
import adfinir.game.ecs.systems.StatsSystem;
import adfinir.game.inventory.ItemGenerator;
import adfinir.game.ecs.components.InventoryComponent;
import adfinir.game.ecs.components.LootComponent;
import adfinir.game.inventory.Weapon;

import adfinir.game.ui.MiniMap;
import adfinir.game.ui.HotbarOverlay;
import adfinir.game.ui.InventoryOverlay;
import adfinir.game.ui.ShopOverlay;
import adfinir.game.ui.StatsOverlay;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

public class GameScreen implements Screen {

    // Taille de vue de base — ExtendViewport agrandit cette zone
    // pour remplir l'écran sans étirement ni bandes noires
    private static final int VIEW_W = 320;
    private static final int VIEW_H = 240;

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
    private HotbarOverlay hotbarOverlay;
    private ShopOverlay shopOverlay;
    private MiniMap      miniMap;
    private OrthographicCamera uiCamera;
    private int screenW, screenH;
    private int currentLevel = 1;

    private final ComponentMapper<TransformComponent> transformMapper = ComponentMapper.getFor(TransformComponent.class);
    private Family enemyFamily;
    private Family lootFamily;

    public GameScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera   = new OrthographicCamera();
        // ExtendViewport : garantit qu'on voit AU MOINS VIEW_W x VIEW_H
        // et étend la vue pour couvrir le reste — pas de bandes noires, pas d'étirement
        viewport = new ExtendViewport(VIEW_W, VIEW_H, camera);

        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setAutoShapeType(true);

        DungeonGenerator generator = new DungeonGenerator(50, 40);
        dungeonMap      = generator.generate();
        dungeonRenderer = new DungeonRenderer(dungeonMap);

        enemyFamily = Family.all(EnemyStatsComponent.class, TransformComponent.class).get();

        lootFamily = Family.all(LootComponent.class, TransformComponent.class).get();

        engine = new Engine();
        engine.addSystem(new StatsSystem());
        engine.addSystem(new MovementSystem(dungeonMap));
        engine.addSystem(new RenderSystem(shapeRenderer));

        player          = new Entity();
        playerTransform = new TransformComponent();
        playerTransform.x = dungeonMap.getSpawnPixelX();
        playerTransform.y = dungeonMap.getSpawnPixelY();

        VelocityComponent    playerVel   = new VelocityComponent();
        RenderComponent      playerRender = new RenderComponent();
        playerRender.color  = new Color(0.2f, 0.7f, 1.0f, 1f);
        playerRender.width  = 12f;
        playerRender.height = 12f;

        PlayerInputComponent playerInput = new PlayerInputComponent();
        playerInput.speed = 80f;

        playerStats = new PlayerStatsComponent();
        CombatComponent playerCombat = new CombatComponent();

        InventoryComponent inventory = new InventoryComponent();
        inventory.equipWeapon(ItemGenerator.generateWeapon());
        inventory.equipArmor(ItemGenerator.generateArmor());
        inventory.equipSpell(0, ItemGenerator.generateCapacity());
        inventory.equipArtifact(ItemGenerator.generateArtifact());

        // Synchronise les stats de départ avec l'équipement généré
        inventory.updateStats(playerStats.stats);

        playerCombat.weapon = inventory.weapon;

        player.add(playerTransform);
        player.add(playerVel);
        player.add(playerRender);
        player.add(playerInput);
        player.add(playerStats);
        player.add(playerCombat);
        player.add(inventory);
        engine.addEntity(player);

        // MAINTENANT on ajoute les systèmes qui dépendent du joueur/des familles
        engine.addSystem(new EnemyMovementSystem(dungeonMap, player));
        engine.addSystem(new CombatSystem(player, enemyFamily));
        engine.addSystem(new PlayerInputSystem(enemyFamily, lootFamily));
        engine.addSystem(new DeathSystem(player));

        // Ajout de quelques ennemis fixes dans des zones accessibles
        for (int i = 0; i < 5; i++) {
            com.badlogic.gdx.math.Vector2 pos = dungeonMap.getRandomFloorPosition();
            spawnEnemy(pos.x, pos.y);
        }

        statsOverlay = new StatsOverlay();
        inventoryOverlay = new InventoryOverlay();
        hotbarOverlay = new HotbarOverlay();
        shopOverlay = new ShopOverlay();
        miniMap      = new MiniMap();
        uiCamera     = new OrthographicCamera();
    }

    /** Régénère un nouveau donjon (plus difficile) quand le joueur atteint la sortie. */
    private void loadNextLevel() {
        currentLevel++;

        DungeonGenerator generator = new DungeonGenerator(50, 40);
        dungeonMap      = generator.generate();
        dungeonRenderer = new DungeonRenderer(dungeonMap);

        // Retire les ennemis de l'ancien niveau
        ImmutableArray<Entity> oldEnemies = engine.getEntitiesFor(enemyFamily);
        for (int i = oldEnemies.size() - 1; i >= 0; i--) {
            engine.removeEntity(oldEnemies.get(i));
        }

        // MovementSystem et EnemyMovementSystem référencent la carte en dur : on les recrée
        engine.removeSystem(engine.getSystem(MovementSystem.class));
        engine.removeSystem(engine.getSystem(EnemyMovementSystem.class));
        engine.addSystem(new MovementSystem(dungeonMap));
        engine.addSystem(new EnemyMovementSystem(dungeonMap, player));

        // Replace le joueur au nouveau spawn (stats/équipement conservés)
        playerTransform.x = dungeonMap.getSpawnPixelX();
        playerTransform.y = dungeonMap.getSpawnPixelY();

        for (int i = 0; i < 5; i++) {
            com.badlogic.gdx.math.Vector2 pos = dungeonMap.getRandomFloorPosition();
            spawnEnemy(pos.x, pos.y);
        }

        Gdx.app.log("GameScreen", "Niveau " + currentLevel + " chargé.");
    }

    private void spawnEnemy(float x, float y) {
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
        EnemyAIComponent ai = new EnemyAIComponent();

        // Vitesse random mais pas excessive (max 60f, le joueur est à 80f)
        ai.speed = com.badlogic.gdx.math.MathUtils.random(20f, 50f);
        ai.pursuitSpeed = ai.speed * 1.2f; // Un peu plus rapide en poursuite, mais reste < 80
        ai.detectionRange = com.badlogic.gdx.math.MathUtils.random(80f, 150f);

        CombatComponent combat = new CombatComponent();
        adfinir.game.enemy.EnemyGenerator.configureEnemy(stats, combat, ai, currentLevel);

        enemy.add(transform);
        enemy.add(vel);
        enemy.add(render);
        enemy.add(stats);
        enemy.add(ai);
        enemy.add(combat);

        engine.addEntity(enemy);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
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
            InventoryComponent inv = player.getComponent(InventoryComponent.class);
            CombatComponent combat = player.getComponent(CombatComponent.class);
            shopOverlay.update(delta, inv, playerStats, combat);
        }

        if (!inventoryOverlay.isVisible() && !shopOverlay.isVisible()) {
            engine.getSystem(StatsSystem.class).update(delta);
            engine.getSystem(CombatSystem.class).update(delta);
            engine.getSystem(PlayerInputSystem.class).update(delta);
            engine.getSystem(EnemyMovementSystem.class).update(delta);
            engine.getSystem(MovementSystem.class).update(delta);
            engine.getSystem(DeathSystem.class).update(delta);

            if (playerStats.isDead) {
                game.setScreen(new GameOverScreen(game, currentLevel));
                dispose();
                return;
            }

            int playerCol = (int) (playerTransform.x / DungeonMap.TILE_SIZE);
            int playerRow = (int) (playerTransform.y / DungeonMap.TILE_SIZE);
            if (playerCol == dungeonMap.exitCol && playerRow == dungeonMap.exitRow) {
                loadNextLevel();
            }
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

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
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

        inventoryOverlay.update(player.getComponent(InventoryComponent.class));
        inventoryOverlay.draw();

        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        ImmutableArray<Entity> enemies = engine.getEntitiesFor(enemyFamily);
        miniMap.draw(shapeRenderer, dungeonMap, playerTransform, screenW, screenH, enemies, transformMapper);

        statsOverlay.update(playerStats, delta, currentLevel);
        statsOverlay.draw();

        hotbarOverlay.update(player.getComponent(InventoryComponent.class), player.getComponent(CombatComponent.class));
        hotbarOverlay.draw(screenW);

        shopOverlay.draw(screenW, screenH, player.getComponent(InventoryComponent.class), playerStats,
            player.getComponent(CombatComponent.class));
    }

    @Override
    public void resize(int w, int h) {
        screenW = w;
        screenH = h;
        viewport.update(w, h, true);
        statsOverlay.resize(w, h);
        inventoryOverlay.resize(w, h);
        hotbarOverlay.resize(w, h);
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
        hotbarOverlay.dispose();
        shopOverlay.dispose();
    }
}
