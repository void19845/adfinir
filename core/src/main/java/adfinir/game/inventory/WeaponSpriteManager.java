package adfinir.game.inventory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Gère le chargement et le découpage du sprite des armes.
 * L'image est divisée en 3 tiers : Hache, Épée, Lance.
 */
public class WeaponSpriteManager {
    private static Texture sheet;
    private static TextureRegion axeRegion;
    private static TextureRegion swordRegion;
    private static TextureRegion spearRegion;

    public static void init() {
        if (sheet != null) return;

        // The file is actually a PNG with a generated name
        sheet = new Texture("ui/ChatGPT Image 6 août 2026, 18_53_57.png");

        float third = sheet.getWidth() / 3f;

        axeRegion   = new TextureRegion(sheet, 0, 0, (int)third, sheet.getHeight());
        swordRegion = new TextureRegion(sheet, (int)third, 0, (int)third, sheet.getHeight());
        spearRegion = new TextureRegion(sheet, (int)(third * 2), 0, (int)third, sheet.getHeight());
    }

    public static TextureRegion getRegion(WeaponType type) {
        init();
        switch (type) {
            case CLAYMORE: return axeRegion;
            case SWORD:    return swordRegion;
            case SPEAR:    return spearRegion;
            default:       return swordRegion;
        }
    }

    public static void dispose() {
        if (sheet != null) {
            sheet.dispose();
            sheet = null;
        }
    }
}
