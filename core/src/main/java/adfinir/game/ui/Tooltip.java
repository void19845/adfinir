package adfinir.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Petite infobulle flottante près du curseur (nom de l'objet survolé). */
public class Tooltip {
    private static final float PAD = 6f;

    public static void draw(ShapeRenderer shapes, SpriteBatch batch, BitmapFont font, float mouseX, float mouseY, String text) {
        GlyphLayout layout = new GlyphLayout(font, text);
        float w = layout.width + PAD * 2;
        float h = layout.height + PAD * 2;
        float x = mouseX + 14f;
        float y = mouseY - h - 4f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.92f);
        shapes.rect(x, y, w, h);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.LIGHT_GRAY);
        shapes.rect(x, y, w, h);
        shapes.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, text, x + PAD, y + h - PAD);
        batch.end();
    }
}
