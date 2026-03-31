package adfinir.game.screens;

import adfinir.game.Main;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class GameOverScreen implements Screen {

    private final Main game;
    private final int levelReached;
    private Stage stage;
    private Skin skin;

    public GameOverScreen(Main game, int levelReached) {
        this.game         = game;
        this.levelReached = levelReached;
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(640, 480));
        skin  = new Skin(Gdx.files.internal("ui/uiskin.json"));
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        table.center();
        stage.addActor(table);

        table.add(new Label("GAME OVER", skin, "subtitle")).padBottom(16).row();
        table.add(new Label("Niveau atteint : " + levelReached, skin)).padBottom(40).row();

        TextButton btnRetry = new TextButton("Rejouer", skin);
        btnRetry.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(game));
                dispose();
            }
        });
        table.add(btnRetry).width(160).height(40).padBottom(12).row();

        TextButton btnMenu = new TextButton("Menu principal", skin);
        btnMenu.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });
        table.add(btnMenu).width(160).height(40);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.0f, 0.0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
