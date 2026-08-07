package adfinir.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;

/**
 * Petits utilitaires visuels partagés par les écrans de menu (fonds,
 * panneaux arrondis, style de boutons cohérent). Tout est généré au runtime
 * via Pixmap : aucun asset supplémentaire à fournir, aucun risque de casser
 * le chargement du skin existant.
 *
 * Chaque écran qui crée un UiFx doit appeler dispose() dans son propre
 * dispose() pour libérer les textures générées.
 */
public class UiFx {

    private final Array<Texture> generated = new Array<>();

    private void fillRoundedRect(Pixmap pm, int x, int y, int w, int h, int r, Color c) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        pm.setColor(c);
        pm.fillRectangle(x + r, y, w - 2 * r, h);
        pm.fillRectangle(x, y + r, w, h - 2 * r);
        pm.fillCircle(x + r, y + r, r);
        pm.fillCircle(x + w - r - 1, y + r, r);
        pm.fillCircle(x + r, y + h - r - 1, r);
        pm.fillCircle(x + w - r - 1, y + h - r - 1, r);
    }

    /** Crée une texture de rectangle arrondi (avec bordure optionnelle), gérée par cette instance. */
    public Texture roundedRect(int w, int h, int radius, Color fill, Color border, int borderThickness) {
        Pixmap pm = new Pixmap(Math.max(1, w), Math.max(1, h), Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.SourceOver);
        if (border != null && borderThickness > 0) {
            fillRoundedRect(pm, 0, 0, w, h, radius, border);
            fillRoundedRect(pm, borderThickness, borderThickness,
                w - 2 * borderThickness, h - 2 * borderThickness,
                Math.max(0, radius - borderThickness), fill);
        } else {
            fillRoundedRect(pm, 0, 0, w, h, radius, fill);
        }
        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        generated.add(tex);
        return tex;
    }

    /** Construit un style de bouton "carte arrondie" cohérent avec le thème sombre du jeu. */
    public TextButton.TextButtonStyle buildButtonStyle(Skin skin, Color base, Color hover,
                                                         Color down, Color border, Color font) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = skin.getFont("default");
        style.fontColor = font;
        style.overFontColor = font;
        style.downFontColor = font;
        style.up   = new TextureRegionDrawable(new TextureRegion(roundedRect(220, 56, 14, base, border, 2)));
        style.over = new TextureRegionDrawable(new TextureRegion(roundedRect(220, 56, 14, hover, border, 2)));
        style.down = new TextureRegionDrawable(new TextureRegion(roundedRect(220, 56, 14, down, border, 2)));
        return style;
    }

    public void dispose() {
        for (Texture t : generated) {
            t.dispose();
        }
        generated.clear();
    }
}