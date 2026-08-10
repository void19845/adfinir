package adfinir.game.screens;

import adfinir.game.Main;
import adfinir.game.ui.UiFx;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class GameOverScreen implements Screen {

    // Palette braises / sang — cohérente avec le thème sombre du menu principal
    private static final Color BG_TOP    = new Color(0.14f, 0.02f, 0.03f, 1f);
    private static final Color BG_BOTTOM = new Color(0.02f, 0.01f, 0.01f, 1f);
    private static final Color ACCENT    = new Color(0.85f, 0.25f, 0.20f, 1f);
    private static final Color PANEL     = new Color(0f, 0f, 0f, 0.40f);
    private static final Color BTN_BASE  = new Color(0.20f, 0.08f, 0.08f, 0.95f);
    private static final Color BTN_HOVER = new Color(0.36f, 0.14f, 0.12f, 0.95f);
    private static final Color BTN_DOWN  = new Color(0.50f, 0.18f, 0.14f, 0.95f);

    private final Main game;
    private final int levelReached;
    private Stage stage;
    private Skin skin;
    private UiFx uiFx;

    private ShapeRenderer shapeRenderer;
    private Ember[] embers;
    private float time = 0f;

    public GameOverScreen(Main game, int levelReached) {
        this.game         = game;
        this.levelReached = levelReached;
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(640, 480));
        skin  = new Skin(Gdx.files.internal("ui/uiskin.json"));
        uiFx  = new UiFx();
        shapeRenderer = new ShapeRenderer();
        Gdx.input.setInputProcessor(stage);

        embers = new Ember[30];
        for (int i = 0; i < embers.length; i++) {
            embers[i] = new Ember(640, 480);
        }

        Table root = new Table();
        root.setFillParent(true);
        root.center();
        stage.addActor(root);

        Label title = new Label("GAME OVER", skin, "subtitle");
        title.setFontScale(1.7f);
        title.setColor(1f, 1f, 1f, 0f);
        title.setAlignment(Align.center);
        root.add(title).padBottom(24).row();

        // Petite secousse d'impact à l'arrivée, puis fondu d'apparition
        title.addAction(Actions.sequence(
            Actions.parallel(
                Actions.fadeIn(0.5f),
                Actions.sequence(
                    Actions.moveBy(-6, 0, 0.04f), Actions.moveBy(12, 0, 0.05f),
                    Actions.moveBy(-10, 0, 0.05f), Actions.moveBy(8, 0, 0.05f),
                    Actions.moveBy(-4, 0, 0.05f), Actions.moveBy(0, 0, 0.05f)
                )
            )
        ));

        // --- Carte "niveau atteint" ---
        Table statCard = new Table();
        statCard.pad(14, 28, 14, 28);
        statCard.setBackground(new TextureRegionDrawable(
            new TextureRegion(uiFx.roundedRect(260, 60, 14, PANEL, ACCENT, 1))));
        Label levelLabel = new Label("Niveau atteint : " + levelReached, skin);
        levelLabel.setColor(1f, 0.85f, 0.8f, 1f);
        statCard.add(levelLabel);
        statCard.getColor().a = 0f;
        statCard.addAction(Actions.delay(0.35f, Actions.fadeIn(0.5f)));
        root.add(statCard).padBottom(40).row();

        TextButton.TextButtonStyle btnStyle = uiFx.buildButtonStyle(
            skin, BTN_BASE, BTN_HOVER, BTN_DOWN, ACCENT, Color.WHITE);

        TextButton btnRetry = new TextButton("Rejouer", btnStyle);
        btnRetry.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(game));
                dispose();
            }
        });
        root.add(btnRetry).width(220).height(56).padBottom(14).row();
        fadeInStaggered(btnRetry, 0);

        TextButton btnMenu = new TextButton("Menu principal", btnStyle);
        btnMenu.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });
        root.add(btnMenu).width(220).height(56);
        fadeInStaggered(btnMenu, 1);
    }

    private void fadeInStaggered(Actor actor, int slot) {
        actor.getColor().a = 0f;
        actor.addAction(Actions.sequence(
            Actions.delay(0.5f + slot * 0.12f),
            Actions.fadeIn(0.35f)
        ));
    }

    @Override
    public void render(float delta) {
        time += delta;

        Gdx.gl.glClearColor(BG_BOTTOM.r, BG_BOTTOM.g, BG_BOTTOM.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        drawBackground(delta);

        stage.act(delta);
        stage.draw();
    }

    /** Dégradé sombre + braises montantes + respiration rouge en fond, calé sur la caméra du Stage. */
    private void drawBackground(float delta) {
        OrthographicCamera cam = (OrthographicCamera) stage.getViewport().getCamera();
        float w = stage.getViewport().getWorldWidth();
        float h = stage.getViewport().getWorldHeight();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(cam.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.rect(0, 0, w, h, BG_TOP, BG_TOP, BG_BOTTOM, BG_BOTTOM);

        // Respiration rouge lente en fond, façon vignette pulsée
        float pulse = 0.05f + 0.05f * (MathUtils.sin(time * 0.8f) + 1f) / 2f;
        shapeRenderer.setColor(ACCENT.r, ACCENT.g, ACCENT.b, pulse);
        shapeRenderer.rect(0, 0, w, h);

        for (Ember e : embers) {
            e.update(delta, w, h);
            shapeRenderer.setColor(1f, 0.5f + e.flicker * 0.3f, 0.15f, e.alpha);
            shapeRenderer.circle(e.x, e.y, e.size);
        }

        shapeRenderer.end();
    }

    @Override
    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        shapeRenderer.dispose();
        uiFx.dispose();
    }

    /** Braise qui monte lentement depuis le bas de l'écran avec un léger scintillement. */
    private static class Ember {
        float x, y, size, speed, alpha, phase, flicker;

        Ember(float worldW, float worldH) {
            reset(worldW, worldH, MathUtils.random(0f, worldH));
        }

        private void reset(float worldW, float worldH, float startY) {
            x = MathUtils.random(0, worldW);
            y = startY;
            size = MathUtils.random(1f, 2.6f);
            speed = MathUtils.random(6f, 18f);
            phase = MathUtils.random(0f, MathUtils.PI2);
            alpha = MathUtils.random(0.2f, 0.55f);
        }

        void update(float delta, float worldW, float worldH) {
            y += speed * delta;
            x += MathUtils.sin(phase + y * 0.03f) * 8f * delta;
            flicker = MathUtils.sin(phase * 3f + y * 0.1f);
            if (y > worldH) {
                reset(worldW, worldH, 0);
            }
        }
    }
}