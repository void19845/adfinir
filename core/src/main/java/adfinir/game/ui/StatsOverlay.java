package adfinir.game.ui;

import adfinir.game.ecs.components.PlayerStatsComponent;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

/**
 * HUD de stats du joueur, en deux parties :
 *  - drawHud() : barres PV/Stamina + or, toujours affichées, ancrées juste
 *    au-dessus de la hotbar (LootBarOverlay) pour former un même cluster
 *    "bas d'écran" façon Minecraft — mais sobre (barres pleines, pas de cœurs).
 *  - draw()    : panneau détaillé (ATK/MAG/DEF/SPD...), replié par défaut,
 *    bascule avec [K].
 * Pas de Scene2D — SpriteBatch + BitmapFont natif libGDX, taille cohérente
 * quelle que soit la résolution.
 */
public class StatsOverlay implements Disposable {

    private final SpriteBatch    batch;
    private final BitmapFont     font;
    private final ShapeRenderer  shapes;

    private boolean visible = false;

    // Mise en page — panneau détaillé [K]
    private static final float MARGIN     = 12f;
    private static final float LINE_H     = 20f;
    private static final float PAD        = 10f;
    private static final float COL_VAL_X  = 92f; // décalage colonne valeur

    // Mise en page — barres HUD persistantes
    private static final float BAR_H       = 16f;
    private static final float BAR_GAP     = 5f;
    private static final float HUD_MARGIN  = 6f; // espace entre la hotbar et les barres

    // Contenu courant (recalculé chaque frame)
    private float hpCur, hpMax, stCur, stMax;
    private String atk = "—", mag = "—", def = "—", spd = "—", gold = "—";

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
        hpCur = stats.currentHp;
        hpMax = stats.stats.maxHp();
        stCur = stats.currentStamina;
        stMax = stats.stats.maxStamina();
        atk   = String.format("%.0f",      stats.atk());
        mag   = String.format("%.0f",      stats.mag());
        def   = String.format("%.0f",      stats.def());
        spd   = String.format("%.0f px/s", stats.spd());
        gold  = String.valueOf(stats.gold);
    }

    // ------------------------------------------------------------------
    // HUD persistant (PV / Stamina / Or) — toujours visible
    // ------------------------------------------------------------------

    public void drawHud(int screenW) {
        float barW = (LootBarOverlay.totalWidth() - BAR_GAP) / 2f;
        float startX = LootBarOverlay.startX(screenW);
        float y = LootBarOverlay.topY() + HUD_MARGIN;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        UiTheme.slotSunken(shapes, startX, y, barW, BAR_H, UiTheme.SLOT_BG_EMPTY);
        UiTheme.slotSunken(shapes, startX + barW + BAR_GAP, y, barW, BAR_H, UiTheme.SLOT_BG_EMPTY);
        drawBarFill(startX, y, barW, hpMax > 0 ? hpCur / hpMax : 0f, new Color(0.78f, 0.16f, 0.18f, 1f));
        drawBarFill(startX + barW + BAR_GAP, y, barW, stMax > 0 ? stCur / stMax : 0f, new Color(0.75f, 0.62f, 0.15f, 1f));
        shapes.end();

        // Badge Or, centré au-dessus des deux barres
        float goldY = y + BAR_H + BAR_GAP;
        float goldW = 70f;
        float goldX = startX + (LootBarOverlay.totalWidth() - goldW) / 2f;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        UiTheme.slotSunken(shapes, goldX, goldY, goldW, BAR_H, UiTheme.SLOT_BG_EMPTY);
        shapes.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, String.format("%.0f / %.0f", hpCur, hpMax), startX, y + BAR_H - 3f, barW, Align.center, false);
        font.draw(batch, String.format("%.0f / %.0f", stCur, stMax), startX + barW + BAR_GAP, y + BAR_H - 3f, barW, Align.center, false);
        font.setColor(new Color(1f, 0.86f, 0.35f, 1f));
        font.draw(batch, gold + " or", goldX, goldY + BAR_H - 3f, goldW, Align.center, false);
        batch.end();
    }

    /** Remplissage proportionnel d'une barre, inséré à l'intérieur du bevel en creux (pas par-dessus). */
    private void drawBarFill(float x, float y, float w, float ratio, Color color) {
        float inset = UiTheme.BEVEL;
        float fillW = Math.max(0f, (w - inset * 2) * Math.min(1f, Math.max(0f, ratio)));
        shapes.setColor(color);
        shapes.rect(x + inset, y + inset, fillW, BAR_H - inset * 2);
    }

    // ------------------------------------------------------------------
    // Panneau détaillé — [K]
    // ------------------------------------------------------------------

    public void draw() {
        if (!visible) return;

        int screenH = Gdx.graphics.getHeight();

        // Lignes : titre + 6 stats (ATK/MAG/DEF/SPD/PV/Stamina) + hint = 8 lignes
        int lines   = 8;
        float boxW  = 210f;
        float boxH  = PAD * 2 + lines * LINE_H;
        float boxX  = MARGIN;
        float boxY  = screenH - MARGIN - boxH;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        UiTheme.panel(shapes, boxX, boxY, boxW, boxH);
        shapes.end();

        batch.begin();

        float x    = boxX + PAD;
        float xVal = boxX + PAD + COL_VAL_X;
        float y    = boxY + boxH - PAD - LINE_H * 0.25f; // libGDX : y = baseline

        font.setColor(UiTheme.TEXT_TITLE);
        font.draw(batch, "STATISTIQUES", x, y);
        y -= LINE_H;

        drawRow(x, xVal, y, "PV",      String.format("%.0f / %.0f", hpCur, hpMax), new Color(0.85f, 0.3f, 0.3f, 1f)); y -= LINE_H;
        drawRow(x, xVal, y, "Stamina", String.format("%.0f / %.0f", stCur, stMax), new Color(0.85f, 0.75f, 0.3f, 1f)); y -= LINE_H;
        drawRow(x, xVal, y, "ATK",     atk,     Color.ORANGE);      y -= LINE_H;
        drawRow(x, xVal, y, "MAG",     mag,     Color.CYAN);        y -= LINE_H;
        drawRow(x, xVal, y, "DEF",     def,     UiTheme.TEXT_BODY); y -= LINE_H;
        drawRow(x, xVal, y, "SPD",     spd,     Color.WHITE);       y -= LINE_H;
        drawRow(x, xVal, y, "Or",      gold,    new Color(1f, 0.86f, 0.35f, 1f)); y -= LINE_H;

        font.setColor(UiTheme.TEXT_DIM);
        font.draw(batch, "[K] fermer", x, y);

        batch.end();
    }

    private void drawRow(float xLabel, float xVal, float y, String label, String value, Color valueColor) {
        font.setColor(UiTheme.TEXT_DIM);
        font.draw(batch, label, xLabel, y);
        font.setColor(valueColor);
        font.draw(batch, value, xVal, y);
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
