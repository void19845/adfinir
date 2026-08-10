package adfinir.game.ui;

import com.badlogic.gdx.Gdx;

/** Position de la souris convertie dans l'espace UI (origine en bas à gauche, comme les overlays). */
public class UiInput {
    public static float mouseX() {
        return Gdx.input.getX();
    }

    public static float mouseY() {
        return Gdx.graphics.getHeight() - Gdx.input.getY();
    }
}
