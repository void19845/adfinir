package adfinir.game.screens;

import adfinir.game.Main;
import adfinir.game.dungeon.DungeonGenerator;
import adfinir.game.dungeon.DungeonMap;
import adfinir.game.dungeon.DungeonRenderer;
import adfinir.game.ecs.components.CombatComponent;
import adfinir.game.ecs.components.PlayerInputComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import adfinir.game.ecs.components.RenderComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.ecs.components.VelocityComponent;
import adfinir.game.ecs.systems.CombatSystem;
import adfinir.game.ecs.systems.MovementSystem;
import adfinir.game.ecs.systems.PlayerInputSystem;
import adfinir.game.ecs.systems.RenderSystem;
import adfinir.game.ecs.systems.StatsSystem;
import adfinir.game.ui.StatsOverlay;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
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
    private int currentLevel = 1;

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

        engine = new Engine();
        engine.addSystem(new StatsSystem());
        engine.addSystem(new CombatSystem());
        engine.addSystem(new PlayerInputSystem());
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

        PlayerStatsComponent playerStats = new PlayerStatsComponent();
        CombatComponent playerCombat = new CombatComponent();
        playerCombat.weapon = Weapon.createSword(); // Equip une épée par défaut

        player.add(playerTransform);
        player.add(playerVel);
        player.add(playerRender);
        player.add(playerInput);
        player.add(playerStats);
        player.add(playerCombat);
        engine.addEntity(player);

        statsOverlay = new StatsOverlay();
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

        engine.getSystem(StatsSystem.class).update(delta);
        engine.getSystem(PlayerInputSystem.class).update(delta);
        engine.getSystem(MovementSystem.class).update(delta);

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

        statsOverlay.update(playerStats, delta);
        statsOverlay.draw();
    }

    @Override
    public void resize(int w, int h) {
        viewport.update(w, h, true);
        statsOverlay.resize(w, h);
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        statsOverlay.dispose();
    }
}
