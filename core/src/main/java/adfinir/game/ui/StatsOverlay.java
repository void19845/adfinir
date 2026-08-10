package adfinir.game.ui;

import adfinir.game.ecs.components.PlayerStatsComponent;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;

/**
 * Overlay stats dessiné directement en coordonnées écran.
 * Pas de Scene2D — SpriteBatch + BitmapFont natif libGDX.
 * Taille toujours cohérente quelle que soit la résolution.
 */
public class StatsOverlay implements Disposable {

    private final SpriteBatch    batch;
    private final BitmapFont     font;
    private final ShapeRenderer  shapes;

    private boolean visible = false;

    // Mise en page
    private static final float MARGIN     = 12f;
    private static final float LINE_H     = 20f;
    private static final float PAD        = 8f;
    private static final float COL_VAL_X  = 90f; // décalage colonne valeur

    // Contenu courant (mis à jour chaque frame)
    private String hp       = "—";
    private String stamina  = "—";
    private String atk      = "—";
    private String mag      = "—";
    private String def      = "—";
    private String spd      = "—";
    private String gold     = "—";

    public StatsOverlay() {
        batch  = new SpriteBatch();
        font   = new BitmapFont(); // police par défaut libGDX (Arial 15px, nette à toute résolution)
        font.setColor(Color.WHITE);
        shapes = new ShapeRenderer();
    }

    public void toggle() {
        visible = !visible;
    }

    public boolean isVisible() { return visible; }

    public void update(PlayerStatsComponent stats, float delta) {
        if (!visible) return;
        hp      = String.format("%.0f / %.0f", stats.currentHp,      stats.maxHp());
        stamina = String.format("%.0f / %.0f", stats.currentStamina, stats.maxStamina());
        atk     = String.format("%.0f",        stats.atk());
        mag     = String.format("%.0f",        stats.mag());
        def     = String.format("%.0f",        stats.def());
        spd     = String.format("%.0f px/s",   stats.spd());
        gold    = String.valueOf(stats.gold);
    }

    public void draw() {
        if (!visible) return;

        int screenH = Gdx.graphics.getHeight();

        // Lignes : titre + 7 stats + hint = 9 lignes
        int lines   = 9;
        float boxW  = 200f;
        float boxH  = PAD * 2 + lines * LINE_H;
        float boxX  = MARGIN;
        float boxY  = screenH - MARGIN - boxH;

        // Fond semi-transparent
        // Correction : On s'assure que le blending est activé pour la transparence
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.72f);
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();

        // Le blending doit être désactivé si d'autres éléments du jeu ne l'utilisent pas,
        // mais libGDX SpriteBatch l'active généralement.
        // Pour éviter les bugs de rendu, on peut laisser activé ou gérer finement.
        // Gdx.gl.glDisable(GL20.GL_BLEND);

        // Texte
        batch.begin();

        float x    = boxX + PAD;
        float xVal = boxX + PAD + COL_VAL_X;
        float y    = boxY + boxH - PAD - LINE_H * 0.25f; // libGDX : y = baseline

        font.setColor(Color.YELLOW);
        font.draw(batch, "-- STATS --", x, y);
        y -= LINE_H;

        drawRow(batch, font, x, xVal, y, "PV",      hp,      Color.GREEN);      y -= LINE_H;
        drawRow(batch, font, x, xVal, y, "Stamina", stamina, Color.YELLOW);     y -= LINE_H;
        drawRow(batch, font, x, xVal, y, "ATK",     atk,     Color.ORANGE);     y -= LINE_H;
        drawRow(batch, font, x, xVal, y, "MAG",     mag,     Color.CYAN);       y -= LINE_H;
        drawRow(batch, font, x, xVal, y, "DEF",     def,     Color.LIGHT_GRAY); y -= LINE_H;
        drawRow(batch, font, x, xVal, y, "SPD",     spd,     Color.WHITE);      y -= LINE_H;
        drawRow(batch, font, x, xVal, y, "Or",      gold,    Color.GOLD);       y -= LINE_H;

        font.setColor(0.5f, 0.5f, 0.5f, 1f);
        font.draw(batch, "[K] fermer", x, y);

        batch.end();
    }

    private void drawRow(SpriteBatch b, BitmapFont f,
                         float xLabel, float xVal, float y,
                         String label, String value, Color valueColor) {
        f.setColor(Color.LIGHT_GRAY);
        f.draw(b, label, xLabel, y);
        f.setColor(valueColor);
        f.draw(b, value, xVal, y);
    }

    public void resize(int w, int h) {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, w, h);
        shapes.getProjectionMatrix().setToOrtho2D(0, 0, w, h);
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        shapes.dispose();
    }
}
