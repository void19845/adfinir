package adfinir.game.screens;

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
import com.badlogic.gdx.utils.viewport.FitViewport;

import adfinir.game.Main;
import adfinir.game.dungeon.DungeonGenerator;
import adfinir.game.dungeon.DungeonMap;
import adfinir.game.dungeon.DungeonRenderer;
import adfinir.game.ecs.components.PlayerInputComponent;
import adfinir.game.ecs.components.RenderComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.ecs.components.VelocityComponent;
import adfinir.game.ecs.systems.MovementSystem;
import adfinir.game.ecs.systems.PlayerInputSystem;
import adfinir.game.ecs.systems.RenderSystem;

public class GameScreen implements Screen {

    // Taille de la vue en pixels (espace monde)
    private static final int VIEW_W = 320;
    private static final int VIEW_H = 240;

    private final Main game;

    // Rendu
    private FitViewport     viewport;
    private OrthographicCamera camera;
    private ShapeRenderer   shapeRenderer;

    // Donjon
    private DungeonMap      dungeonMap;
    private DungeonRenderer dungeonRenderer;

    // ECS Ashley
    private Engine          engine;
    private Entity          player;
    private TransformComponent playerTransform;

    private int currentLevel = 1;

    public GameScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        // --- Caméra & viewport ---
        camera   = new OrthographicCamera();
        viewport = new FitViewport(VIEW_W, VIEW_H, camera);

        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setAutoShapeType(true);

        // --- Donjon généré procéduralement ---
        DungeonGenerator generator = new DungeonGenerator(50, 40);
        dungeonMap      = generator.generate();
        dungeonRenderer = new DungeonRenderer(dungeonMap);

        // --- ECS ---
        engine = new Engine();

        // Systèmes (ordre d'exécution via priorité dans le constructeur)
        engine.addSystem(new PlayerInputSystem());
        engine.addSystem(new MovementSystem(dungeonMap));
        engine.addSystem(new RenderSystem(shapeRenderer));

        // Entité joueur — on le place sur la première tile de sol disponible
        player = new Entity();
        playerTransform = new TransformComponent();
        playerTransform.x = dungeonMap.getSpawnPixelX();
        playerTransform.y = dungeonMap.getSpawnPixelY();

        VelocityComponent playerVel = new VelocityComponent();

        RenderComponent playerRender = new RenderComponent();
        playerRender.color = new Color(0.2f, 0.7f, 1.0f, 1f); // bleu clair
        playerRender.width  = 12f;
        playerRender.height = 12f;

        PlayerInputComponent playerInput = new PlayerInputComponent();
        playerInput.speed = 80f;

        player.add(playerTransform);
        player.add(playerVel);
        player.add(playerRender);
        player.add(playerInput);
        engine.addEntity(player);
    }

    @Override
    public void render(float delta) {
        // --- Échap = menu principal ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MainMenuScreen(game));
            dispose();
            return;
        }

        // --- Mise à jour ECS (hors rendu) ---
        // On met à jour manuellement les systèmes non-rendu d'abord
        engine.getSystem(PlayerInputSystem.class).update(delta);
        engine.getSystem(MovementSystem.class).update(delta);

        // --- Caméra suit le joueur (lerp doux) ---
        float camTargetX = MathUtils.clamp(
            playerTransform.x,
            VIEW_W / 2f,
            dungeonMap.getPixelWidth()  - VIEW_W / 2f
        );
        float camTargetY = MathUtils.clamp(
            playerTransform.y,
            VIEW_H / 2f,
            dungeonMap.getPixelHeight() - VIEW_H / 2f
        );
        camera.position.x += (camTargetX - camera.position.x) * 6f * delta;
        camera.position.y += (camTargetY - camera.position.y) * 6f * delta;
        camera.update();

        // --- Rendu ---
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Viewport pour le culling
        Rectangle viewRect = new Rectangle(
            camera.position.x - VIEW_W / 2f,
            camera.position.y - VIEW_H / 2f,
            VIEW_W,
            VIEW_H
        );
        dungeonRenderer.render(shapeRenderer, viewRect);

        // Rendu des entités (RenderSystem utilise le shapeRenderer déjà ouvert)
        engine.getSystem(RenderSystem.class).update(delta);

        shapeRenderer.end();
    }

    @Override
    public void resize(int w, int h) {
        viewport.update(w, h, true);
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}
