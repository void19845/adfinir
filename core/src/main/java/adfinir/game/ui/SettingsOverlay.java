package adfinir.game.ui;

import adfinir.game.input.GameAction;
import adfinir.game.input.KeyBindings;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

/**
 * Menu paramètres : liste des touches configurables (voir GameAction /
 * KeyBindings), cliquables pour les réassigner. Réutilisé tel quel dans deux
 * contextes :
 *  - MainMenuScreen : bouton "Paramètres" dédié, pas de ligne "Quitter".
 *  - GameScreen : ouvert par [Échap] (voir configure(true, ...)), avec une
 *    ligne "Quitter vers le menu principal" en plus — [Échap] referme le
 *    panneau (reprend la partie) plutôt que de quitter instantanément comme
 *    avant, ce qui évite aussi les sorties accidentelles.
 * L'ouverture/fermeture (y compris sur [Échap]) reste décidée par l'appelant
 * (GameScreen/MainMenuScreen) ; cet overlay ne gère que son propre contenu.
 */
public class SettingsOverlay implements Disposable {

    private static final float PADDING  = 16f;
    private static final float ROW_H    = 26f;
    private static final float ROW_GAP  = 2f;
    private static final float HEADER_H = 28f;
    private static final float FOOTER_H = 18f;
    private static final float BOX_W    = 380f;
    private static final float KEY_COL_X = 220f;

    private static final GameAction[] ACTIONS = GameAction.values();
    private static final int MAX_KEYCODE = 255;

    private final SpriteBatch   batch;
    private final BitmapFont    font;
    private final ShapeRenderer shapes;

    private boolean visible = false;
    private GameAction rebindingAction;

    private boolean showQuitRow = false;
    private Runnable onQuit;

    public SettingsOverlay() {
        batch  = new SpriteBatch();
        font   = new BitmapFont();
        shapes = new ShapeRenderer();
    }

    /** À appeler une fois après construction (GameScreen) ou avant ouverture (MainMenuScreen). */
    public void configure(boolean showQuitRow, Runnable onQuit) {
        this.showQuitRow = showQuitRow;
        this.onQuit = onQuit;
    }

    public boolean isVisible() { return visible; }

    public void open() {
        visible = true;
        rebindingAction = null;
    }

    public void close() {
        visible = false;
        rebindingAction = null;
    }

    private int rowCount() {
        return ACTIONS.length + 1 + (showQuitRow ? 1 : 0); // + réinitialiser [+ quitter]
    }

    private float boxH() {
        return PADDING + HEADER_H + PADDING
            + rowCount() * (ROW_H + ROW_GAP) - ROW_GAP
            + PADDING + FOOTER_H + PADDING;
    }

    public void handleInput() {
        if (!visible) return;

        if (rebindingAction != null) {
            pollRebind();
            return;
        }

        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) return;

        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();
        float boxX = (screenW - BOX_W) / 2f;
        float boxY = (screenH - boxH()) / 2f;
        float mx = Gdx.input.getX();
        float my = screenH - Gdx.input.getY();

        float rowX = boxX + PADDING;
        float rowW = BOX_W - PADDING * 2;
        float rowTop = boxY + boxH() - PADDING - HEADER_H - PADDING;

        int row = 0;
        for (GameAction action : ACTIONS) {
            float rowY = rowTop - (row + 1) * (ROW_H + ROW_GAP) + ROW_GAP;
            if (mx >= rowX && mx <= rowX + rowW && my >= rowY && my <= rowY + ROW_H) {
                rebindingAction = action;
                return;
            }
            row++;
        }

        // Ligne "Réinitialiser"
        float resetY = rowTop - (row + 1) * (ROW_H + ROW_GAP) + ROW_GAP;
        if (mx >= rowX && mx <= rowX + rowW && my >= resetY && my <= resetY + ROW_H) {
            KeyBindings.resetDefaults();
            return;
        }
        row++;

        if (showQuitRow) {
            float quitY = rowTop - (row + 1) * (ROW_H + ROW_GAP) + ROW_GAP;
            if (mx >= rowX && mx <= rowX + rowW && my >= quitY && my <= quitY + ROW_H) {
                if (onQuit != null) onQuit.run();
            }
        }
    }

    /** Écoute la prochaine touche pressée pour l'assigner à rebindingAction ; [Échap] annule juste le rebind. */
    private void pollRebind() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            rebindingAction = null;
            return;
        }
        for (int code = 0; code <= MAX_KEYCODE; code++) {
            if (Gdx.input.isKeyJustPressed(code)) {
                KeyBindings.rebind(rebindingAction, code);
                rebindingAction = null;
                return;
            }
        }
    }

    public void draw() {
        if (!visible) return;

        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();
        float boxH = boxH();
        float boxX = (screenW - BOX_W) / 2f;
        float boxY = (screenH - boxH) / 2f;

        float mx = Gdx.input.getX();
        float my = screenH - Gdx.input.getY();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        float rowX = boxX + PADDING;
        float rowW = BOX_W - PADDING * 2;
        float rowTop = boxY + boxH - PADDING - HEADER_H - PADDING;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        UiTheme.panel(shapes, boxX, boxY, BOX_W, boxH);

        int row = 0;
        for (GameAction action : ACTIONS) {
            float rowY = rowTop - (row + 1) * (ROW_H + ROW_GAP) + ROW_GAP;
            boolean hovered = mx >= rowX && mx <= rowX + rowW && my >= rowY && my <= rowY + ROW_H;
            boolean rebindingThis = rebindingAction == action;
            Color bg = rebindingThis ? UiTheme.SLOT_BG_SELECT : (hovered ? UiTheme.SLOT_BG_HOVER : UiTheme.SLOT_BG);
            UiTheme.slotSunken(shapes, rowX, rowY, rowW, ROW_H, bg);
            row++;
        }

        float resetY = rowTop - (row + 1) * (ROW_H + ROW_GAP) + ROW_GAP;
        boolean resetHovered = mx >= rowX && mx <= rowX + rowW && my >= resetY && my <= resetY + ROW_H;
        UiTheme.slotSunken(shapes, rowX, resetY, rowW, ROW_H, resetHovered ? UiTheme.SLOT_BG_HOVER : UiTheme.SLOT_BG_EMPTY);
        row++;

        float quitY = 0f;
        boolean quitHovered = false;
        if (showQuitRow) {
            quitY = rowTop - (row + 1) * (ROW_H + ROW_GAP) + ROW_GAP;
            quitHovered = mx >= rowX && mx <= rowX + rowW && my >= quitY && my <= quitY + ROW_H;
            UiTheme.slotSunken(shapes, rowX, quitY, rowW, ROW_H, quitHovered ? new Color(0.35f, 0.14f, 0.14f, 1f) : UiTheme.SLOT_BG_EMPTY);
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(resetHovered ? UiTheme.PANEL_ACCENT : UiTheme.TEXT_DIM);
        shapes.rect(rowX, resetY, rowW, ROW_H);
        if (showQuitRow) {
            shapes.setColor(quitHovered ? new Color(0.85f, 0.35f, 0.3f, 1f) : UiTheme.TEXT_DIM);
            shapes.rect(rowX, quitY, rowW, ROW_H);
        }
        shapes.end();

        batch.begin();

        font.getData().setScale(1.2f);
        font.setColor(UiTheme.TEXT_TITLE);
        font.draw(batch, "PARAMÈTRES", rowX, boxY + boxH - PADDING - 4f);
        font.getData().setScale(1f);

        row = 0;
        for (GameAction action : ACTIONS) {
            float rowY = rowTop - (row + 1) * (ROW_H + ROW_GAP) + ROW_GAP;
            boolean rebindingThis = rebindingAction == action;

            font.setColor(UiTheme.TEXT_BODY);
            font.draw(batch, action.label, rowX + 10f, rowY + ROW_H - 8f);

            if (rebindingThis) {
                font.setColor(UiTheme.PANEL_ACCENT);
                font.draw(batch, "appuie sur une touche… (Échap annule)", rowX + KEY_COL_X, rowY + ROW_H - 8f);
            } else {
                font.setColor(UiTheme.TEXT_TITLE);
                font.draw(batch, KeyBindings.keyName(action), rowX + KEY_COL_X, rowY + ROW_H - 8f);
            }
            row++;
        }

        font.setColor(UiTheme.TEXT_DIM);
        font.draw(batch, "Réinitialiser les touches par défaut", rowX + 10f, resetY + ROW_H - 8f);
        row++;

        if (showQuitRow) {
            font.setColor(new Color(0.9f, 0.55f, 0.5f, 1f));
            font.draw(batch, "Quitter vers le menu principal (sauvegarde)", rowX + 10f, quitY + ROW_H - 8f);
        }

        font.setColor(UiTheme.TEXT_DIM);
        font.draw(batch, "[Échap] fermer  —  clic sur une touche pour la réassigner",
            rowX, boxY + PADDING + FOOTER_H - 4f, rowW, Align.left, true);

        batch.end();
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
