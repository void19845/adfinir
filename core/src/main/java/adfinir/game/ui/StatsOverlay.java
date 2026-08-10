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
    private static final float BAR_H      = 14f;
    private static final float BAR_ROW_H  = BAR_H + 6f;

    // Contenu courant (mis à jour chaque frame)
    private String hp       = "—";
    private String stamina  = "—";
    private String atk      = "—";
    private String mag      = "—";
    private String def      = "—";
    private String spd      = "—";
    private float  hpRatio      = 1f;
    private float  staminaRatio = 1f;
    private int    level        = 1;
    private int    gold         = 0;

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

    public void update(PlayerStatsComponent stats, float delta, int level) {
        if (!visible) return;
        this.level = level;
        this.gold  = stats.gold;
        hp      = String.format("%.0f / %.0f", stats.currentHp,      stats.maxHp());
        stamina = String.format("%.0f / %.0f", stats.currentStamina, stats.maxStamina());
        atk     = String.format("%.0f",        stats.atk());
        mag     = String.format("%.0f",        stats.mag());
        def     = String.format("%.0f",        stats.def());
        spd     = String.format("%.0f px/s",   stats.spd());
        hpRatio      = stats.maxHp()      > 0 ? stats.currentHp      / stats.maxHp()      : 0f;
        staminaRatio = stats.maxStamina() > 0 ? stats.currentStamina / stats.maxStamina() : 0f;
    }

    public void draw() {
        if (!visible) return;

        int screenH = Gdx.graphics.getHeight();

        // Lignes : titre + 2 barres + 4 stats + or + hint = 8 lignes de texte + 2 barres
        int textLines = 8;
        float boxW  = 200f;
        float boxH  = PAD * 2 + textLines * LINE_H + 2 * BAR_ROW_H;
        float boxX  = MARGIN;
        float boxY  = screenH - MARGIN - boxH;
        float barW  = boxW - PAD * 2;

        // Fond semi-transparent
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.72f);
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();

        float x    = boxX + PAD;
        float xVal = boxX + PAD + COL_VAL_X;
        float y    = boxY + boxH - PAD - LINE_H * 0.25f; // libGDX : y = baseline

        // Barres HP / Stamina (fond + remplissage), dessinées avant le texte
        float hpBarY      = y - LINE_H - BAR_H;
        float staminaBarY = hpBarY - LINE_H - BAR_H;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawBar(x, hpBarY,      barW, BAR_H, hpRatio,      new Color(0.15f, 0.15f, 0.15f, 1f), Color.RED);
        drawBar(x, staminaBarY, barW, BAR_H, staminaRatio, new Color(0.15f, 0.15f, 0.15f, 1f), Color.YELLOW);
        shapes.end();

        // Texte
        batch.begin();

        font.setColor(Color.YELLOW);
        font.draw(batch, "-- STATS (Niveau " + level + ") --", x, y);
        y -= LINE_H;

        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "PV", x, y);
        centerTextOnBar(x, hpBarY, barW, BAR_H, hp);
        y = hpBarY - LINE_H * 0.75f;

        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Stamina", x, y);
        centerTextOnBar(x, staminaBarY, barW, BAR_H, stamina);
        y = staminaBarY - LINE_H;

        drawRow(batch, font, x, xVal, y, "ATK",     atk,     Color.ORANGE);     y -= LINE_H;
        drawRow(batch, font, x, xVal, y, "MAG",     mag,     Color.CYAN);       y -= LINE_H;
        drawRow(batch, font, x, xVal, y, "DEF",     def,     Color.LIGHT_GRAY); y -= LINE_H;
        drawRow(batch, font, x, xVal, y, "SPD",     spd,     Color.WHITE);      y -= LINE_H;
        drawRow(batch, font, x, xVal, y, "Or",      String.valueOf(gold), Color.GOLD); y -= LINE_H;

        font.setColor(0.5f, 0.5f, 0.5f, 1f);
        font.draw(batch, "[K] fermer", x, y);

        batch.end();
    }

    /** Dessine une barre de progression (fond sombre + remplissage coloré selon ratio). */
    private void drawBar(float x, float y, float w, float h, float ratio, Color bg, Color fill) {
        ratio = com.badlogic.gdx.math.MathUtils.clamp(ratio, 0f, 1f);
        shapes.setColor(bg);
        shapes.rect(x, y, w, h);
        shapes.setColor(fill);
        shapes.rect(x, y, w * ratio, h);
    }

    /** Affiche un texte centré horizontalement par-dessus une barre. */
    private void centerTextOnBar(float x, float y, float w, float h, String text) {
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, text);
        font.setColor(Color.WHITE);
        font.draw(batch, text, x + (w - layout.width) / 2f, y + h - (h - layout.height) / 2f);
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
