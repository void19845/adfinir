package adfinir.game.screens;

import adfinir.game.Main;
import adfinir.game.save.SaveData;
import adfinir.game.save.SaveManager;
import adfinir.game.ui.SettingsOverlay;
import adfinir.game.ui.UiFx;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class MainMenuScreen implements Screen {

    // Palette sombre "donjon" — reprise sur GameOverScreen pour une identité visuelle cohérente
    private static final Color BG_TOP    = new Color(0.11f, 0.08f, 0.18f, 1f);
    private static final Color BG_BOTTOM = new Color(0.02f, 0.02f, 0.04f, 1f);
    private static final Color ACCENT    = new Color(0.62f, 0.42f, 0.95f, 1f);
    private static final Color PANEL     = new Color(0f, 0f, 0f, 0.35f);
    private static final Color BTN_BASE  = new Color(0.16f, 0.13f, 0.24f, 0.95f);
    private static final Color BTN_HOVER = new Color(0.30f, 0.22f, 0.42f, 0.95f);
    private static final Color BTN_DOWN  = new Color(0.42f, 0.30f, 0.60f, 0.95f);

    private final Main game;
    private Stage stage;
    private Skin skin;
    private UiFx uiFx;
    private SettingsOverlay settingsOverlay;
    private boolean settingsWasVisible = false;

    private ShapeRenderer shapeRenderer;
    private Mote[] motes;
    private float time = 0f;

    public MainMenuScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(640, 480));
        skin  = new Skin(Gdx.files.internal("ui/uiskin.json"));
        uiFx  = new UiFx();
        shapeRenderer = new ShapeRenderer();
        settingsOverlay = new SettingsOverlay();
        settingsOverlay.configure(false, null); // pas de ligne "Quitter" depuis le menu principal
        Gdx.input.setInputProcessor(stage);

        motes = new Mote[36];
        for (int i = 0; i < motes.length; i++) {
            motes[i] = new Mote(640, 480);
        }

        Table root = new Table();
        root.setFillParent(true);
        root.center();
        stage.addActor(root);

        // --- Titre avec léger flottement continu ---
        Label title = new Label("ADFINIR", skin, "subtitle");
        title.setFontScale(1.6f);
        title.setColor(1f, 1f, 1f, 0f);
        title.setAlignment(Align.center);
        root.add(title).padBottom(6).row();
        title.addAction(Actions.sequence(
            Actions.fadeIn(0.6f),
            Actions.forever(Actions.sequence(
                Actions.moveBy(0, 5, 1.4f, Interpolation.sine),
                Actions.moveBy(0, -5, 1.4f, Interpolation.sine)
            ))
        ));

        Label subtitle = new Label("Descend. Combats. Survis.", skin);
        subtitle.setColor(ACCENT.r, ACCENT.g, ACCENT.b, 0f);
        root.add(subtitle).padBottom(36).row();
        subtitle.addAction(Actions.delay(0.3f, Actions.fadeIn(0.6f)));

        // --- Panneau contenant les boutons ---
        Table panel = new Table();
        panel.pad(28, 36, 28, 36);
        panel.setBackground(new TextureRegionDrawable(
            new TextureRegion(uiFx.roundedRect(320, 260, 18, PANEL, ACCENT, 1))));
        root.add(panel).row();

        TextButton.TextButtonStyle btnStyle = uiFx.buildButtonStyle(
            skin, BTN_BASE, BTN_HOVER, BTN_DOWN, ACCENT, Color.WHITE);

        boolean hasSave = SaveManager.saveExists();
        int slot = 0;

        if (hasSave) {
            TextButton btnContinue = new TextButton("Continuer", btnStyle);
            btnContinue.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    SaveData save = SaveManager.load();
                    game.setScreen(new GameScreen(game, save));
                    dispose();
                }
            });
            panel.add(btnContinue).width(220).height(56).padBottom(14).row();
            fadeInStaggered(btnContinue, slot++);
        }

        TextButton btnPlay = new TextButton(hasSave ? "Nouvelle partie" : "Jouer", btnStyle);
        btnPlay.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(game));
                dispose();
            }
        });
        panel.add(btnPlay).width(220).height(56).padBottom(14).row();
        fadeInStaggered(btnPlay, slot++);

        TextButton btnSettings = new TextButton("Paramètres", btnStyle);
        btnSettings.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.input.setInputProcessor(null); // évite les clics à travers vers les boutons du Stage
                settingsOverlay.open();
            }
        });
        panel.add(btnSettings).width(220).height(56).padBottom(14).row();
        fadeInStaggered(btnSettings, slot++);

        TextButton btnQuit = new TextButton("Quitter", btnStyle);
        btnQuit.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });
        panel.add(btnQuit).width(220).height(56);
        fadeInStaggered(btnQuit, slot);
    }

    private void fadeInStaggered(Actor actor, int slot) {
        actor.getColor().a = 0f;
        actor.addAction(Actions.sequence(
            Actions.delay(0.15f + slot * 0.12f),
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

        if (settingsOverlay.isVisible()) {
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
                settingsOverlay.close();
            } else {
                settingsOverlay.handleInput();
            }
            settingsOverlay.draw();
        }
        if (settingsWasVisible && !settingsOverlay.isVisible()) {
            Gdx.input.setInputProcessor(stage); // rend la main aux boutons du Stage à la fermeture
        }
        settingsWasVisible = settingsOverlay.isVisible();
    }

    /** Dégradé nocturne + poussière ambiante flottante, calé sur la caméra du Stage. */
    private void drawBackground(float delta) {
        OrthographicCamera cam = (OrthographicCamera) stage.getViewport().getCamera();
        float w = stage.getViewport().getWorldWidth();
        float h = stage.getViewport().getWorldHeight();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(cam.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.rect(0, 0, w, h, BG_TOP, BG_TOP, BG_BOTTOM, BG_BOTTOM);

        for (Mote m : motes) {
            m.update(delta, w, h);
            shapeRenderer.setColor(ACCENT.r, ACCENT.g, ACCENT.b, m.alpha);
            shapeRenderer.circle(m.x, m.y, m.size);
        }

        shapeRenderer.end();
    }

    @Override
    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
        settingsOverlay.resize(w, h);
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
        settingsOverlay.dispose();
    }

    /** Petite particule de poussière ambiante qui monte lentement à l'écran. */
    private static class Mote {
        float x, y, size, speed, alpha, phase;

        Mote(float worldW, float worldH) {
            reset(worldW, worldH, MathUtils.random(0f, worldH));
        }

        private void reset(float worldW, float worldH, float startY) {
            x = MathUtils.random(0, worldW);
            y = startY;
            size = MathUtils.random(1f, 2.4f);
            speed = MathUtils.random(4f, 12f);
            phase = MathUtils.random(0f, MathUtils.PI2);
            alpha = MathUtils.random(0.12f, 0.4f);
        }

        void update(float delta, float worldW, float worldH) {
            y += speed * delta;
            x += MathUtils.sin(phase + y * 0.02f) * 6f * delta;
            if (y > worldH) {
                reset(worldW, worldH, 0);
            }
        }
    }
}