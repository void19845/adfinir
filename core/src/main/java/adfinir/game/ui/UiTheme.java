package adfinir.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Palette et primitives partagées par tous les overlays UI (inventaire,
 * boutique, hotbar, stats, mini-map) pour un look cohérent : panneaux
 * ardoise foncé avec bevel pixel-art (clair en haut/gauche, sombre en
 * bas/droite = "en relief" ; inversé = "en creux" pour les slots), sans
 * texture bois/pierre — sobre plutôt que cartoon.
 *
 * panel()/slotSunken()/bevel() dessinent uniquement des rect() : à appeler
 * à l'intérieur d'un bloc shapes.begin(ShapeType.Filled) déjà ouvert par
 * l'appelant (pas de begin/end ici, pour rester compatible avec le batching
 * existant de chaque overlay).
 */
public final class UiTheme {
    private UiTheme() {}

    public static final float BEVEL = 2f;

    // Panneaux pleins (fenêtres : inventaire, boutique...)
    public static final Color PANEL_BG      = new Color(0.07f, 0.07f, 0.10f, 0.96f);
    public static final Color PANEL_EDGE_HI = new Color(0.34f, 0.36f, 0.42f, 1f);
    public static final Color PANEL_EDGE_LO = new Color(0.01f, 0.01f, 0.02f, 1f);
    public static final Color PANEL_ACCENT  = new Color(0.55f, 0.85f, 0.95f, 0.95f);

    // Bandeau d'en-tête (titre)
    public static final Color HEADER_BG = new Color(0.11f, 0.12f, 0.16f, 1f);

    // Slots (case d'objet / socket)
    public static final Color SLOT_BG        = new Color(0.14f, 0.14f, 0.18f, 1f);
    public static final Color SLOT_BG_EMPTY  = new Color(0.09f, 0.09f, 0.12f, 1f);
    public static final Color SLOT_BG_HOVER  = new Color(0.21f, 0.20f, 0.27f, 1f);
    public static final Color SLOT_BG_SELECT = new Color(0.14f, 0.24f, 0.28f, 1f);
    public static final Color SLOT_EDGE_HI   = new Color(0.38f, 0.38f, 0.44f, 1f);
    public static final Color SLOT_EDGE_LO   = new Color(0f, 0f, 0f, 1f);

    public static final Color TEXT_TITLE = new Color(1f, 0.86f, 0.4f, 1f);
    public static final Color TEXT_BODY  = new Color(0.85f, 0.85f, 0.88f, 1f);
    public static final Color TEXT_DIM   = new Color(0.5f, 0.5f, 0.56f, 1f);

    /** Panneau "en relief" : fond plein + bevel clair (haut/gauche) / sombre (bas/droite). */
    public static void panel(ShapeRenderer s, float x, float y, float w, float h) {
        s.setColor(PANEL_BG);
        s.rect(x, y, w, h);
        bevel(s, x, y, w, h, PANEL_EDGE_HI, PANEL_EDGE_LO);
    }

    /** Slot "en creux" : fond plein + bevel inversé (sombre haut/gauche, clair bas/droite = enfoncé). */
    public static void slotSunken(ShapeRenderer s, float x, float y, float w, float h, Color bg) {
        s.setColor(bg);
        s.rect(x, y, w, h);
        bevel(s, x, y, w, h, SLOT_EDGE_LO, SLOT_EDGE_HI);
    }

    private static void bevel(ShapeRenderer s, float x, float y, float w, float h, Color hi, Color lo) {
        s.setColor(hi);
        s.rect(x, y + h - BEVEL, w, BEVEL);   // haut
        s.rect(x, y, BEVEL, h);               // gauche
        s.setColor(lo);
        s.rect(x, y, w, BEVEL);               // bas
        s.rect(x + w - BEVEL, y, BEVEL, h);   // droite
    }

    /** Simple cadre plein (accent de sélection / rareté), à appeler dans un bloc ShapeType.Line. */
    public static void outline(ShapeRenderer s, float x, float y, float w, float h, Color c) {
        s.setColor(c);
        s.rect(x, y, w, h);
    }
}
