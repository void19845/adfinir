package adfinir.game.ui;

import adfinir.game.inventory.*;
import adfinir.game.ecs.components.InventoryComponent;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

import java.util.List;

/**
 * Overlay plein écran affichant l'inventaire et l'équipement du joueur.
 *
 * Layout entièrement dérivé de constantes (BOX_W / BOX_H calculés à partir
 * du header, des 4 slots et du footer) pour éviter tout chevauchement,
 * quelle que soit la taille de police ou d'icône utilisée.
 *
 * Un panneau de détails est affiché à droite de chaque slot :
 *  - Armure / Artéfact  : bonus de stats (+X PV Max, +X DEF, ...)
 *  - Arme               : liste des combos (dégâts, cooldown)
 *  - Capacité           : effet principal + modificateurs
 */
public class InventoryOverlay implements Disposable {

    private final SpriteBatch   batch;
    private final BitmapFont    font;
    private final ShapeRenderer shapes;

    private boolean visible = false;
    private InventoryComponent currentInventory;

    // --- Mise en page ---
    private static final float MARGIN     = 20f;
    private static final float PADDING    = 15f;  // marge intérieure + espace vertical entre blocs
    private static final float SLOT_W     = 190f;
    private static final float SLOT_H     = 64f;
    private static final float SLOT_GAP   = 12f;  // espace entre deux slots consécutifs
    private static final float HEADER_H   = 26f;  // hauteur réservée au titre
    private static final float FOOTER_H   = 20f;  // hauteur réservée au texte de fermeture
    private static final float SPRITE_MAX = 34f;  // taille max de l'icône dans un slot
    private static final int   SLOT_COUNT = 4;

    private static final float DETAIL_W        = 280f; // largeur du panneau de détails
    private static final float DETAIL_LINE_H   = 13f;
    private static final float DETAIL_SCALE    = 0.8f;
    private static final int   DETAIL_MAX_LINES = 5;   // sécurité anti-débordement du slot

    private static final float BOX_W =
        PADDING + SLOT_W + PADDING + DETAIL_W + PADDING;

    private static final float BOX_H =
        PADDING                                   // bord haut
            + HEADER_H
            + PADDING                                   // séparation titre / slots
            + SLOT_COUNT * SLOT_H + (SLOT_COUNT - 1) * SLOT_GAP
            + PADDING                                   // séparation slots / footer
            + FOOTER_H
            + PADDING;                                  // bord bas

    public InventoryOverlay() {
        batch  = new SpriteBatch();
        font   = new BitmapFont();
        font.setColor(Color.WHITE);
        shapes = new ShapeRenderer();
    }

    public void toggle() {
        visible = !visible;
    }

    public boolean isVisible() { return visible; }

    public void update(InventoryComponent inventory) {
        this.currentInventory = inventory;
    }

    public void draw() {
        if (!visible || currentInventory == null) return;

        int screenH = Gdx.graphics.getHeight();

        float boxX = MARGIN;
        float boxY = (screenH - BOX_H) / 2f;
        float slotX = boxX + PADDING;
        float detailX = slotX + SLOT_W + PADDING;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Fond du panneau
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.85f);
        shapes.rect(boxX, boxY, BOX_W, BOX_H);
        shapes.end();

        // Y du haut du premier slot (juste sous le titre)
        float firstSlotTop = boxY + BOX_H - PADDING - HEADER_H - PADDING;

        // Cadres des 4 slots
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.GRAY);
        for (int i = 0; i < SLOT_COUNT; i++) {
            float slotY = firstSlotTop - i * (SLOT_H + SLOT_GAP) - SLOT_H;
            shapes.rect(slotX, slotY, SLOT_W, SLOT_H);
            shapes.rect(detailX, slotY, DETAIL_W, SLOT_H);
        }
        shapes.end();

        batch.begin();

        // Titre : positionné au-dessus des slots, ne les chevauche plus
        font.setColor(Color.YELLOW);
        font.draw(batch, "--- INVENTAIRE ---", slotX, boxY + BOX_H - PADDING);

        String[] labels = { "Arme", "Capacité", "Armure", "Artéfact" };
        Item[]   items  = {
            currentInventory.weapon,
            currentInventory.capacity,
            currentInventory.armor,
            currentInventory.artifact
        };

        for (int i = 0; i < SLOT_COUNT; i++) {
            float slotY = firstSlotTop - i * (SLOT_H + SLOT_GAP) - SLOT_H;
            drawSlotContent(batch, slotX, slotY, labels[i], items[i]);
            drawDetailPanel(batch, detailX, slotY, items[i]);
        }

        // Pied de page
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "[E] fermer l'inventaire", slotX, boxY + PADDING + FOOTER_H);

        batch.end();
    }

    private void drawSlotContent(SpriteBatch batch, float x, float y, String label, Item item) {
        // Zone icône réservée à droite du slot : le texte ne l'empiète jamais
        float iconZoneW = SPRITE_MAX + 8f;
        float textZoneW = SLOT_W - iconZoneW - 6f;

        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, label, x + 6, y + SLOT_H - 6);

        if (item != null) {
            if (item instanceof Weapon) {
                TextureRegion region = WeaponSpriteManager.getRegion(((Weapon) item).type);
                float w = region.getRegionWidth();
                float h = region.getRegionHeight();
                float scale = Math.min(SPRITE_MAX / w, SPRITE_MAX / h);
                float finalW = w * scale;
                float finalH = h * scale;
                float iconX = x + SLOT_W - iconZoneW + (iconZoneW - finalW) / 2f;
                float iconY = y + (SLOT_H - finalH) / 2f;
                batch.draw(region, iconX, iconY, finalW, finalH);
            }

            // Nom + couleur de rareté, contraint et tronqué dans la zone de texte
            font.setColor(ItemDetails.rarityColor(item.rarity));
            font.draw(batch, item.name, x + 6, y + SLOT_H - 22, textZoneW, Align.left, true);

            // Description courte, une seule ligne
            font.setColor(Color.WHITE);
            String desc = item.getDescription();
            int maxChars = 20;
            if (desc.length() > maxChars) desc = desc.substring(0, maxChars - 3) + "...";
            font.draw(batch, desc, x + 6, y + SLOT_H - 38, textZoneW, Align.left, true);
        } else {
            font.setColor(Color.DARK_GRAY);
            font.draw(batch, "Vide", x + 6, y + SLOT_H - 22);
        }
    }

    /**
     * Panneau de détails à droite du slot : stats (Armure/Artéfact),
     * combos (Arme) ou effet + modificateurs (Capacité).
     */
    private void drawDetailPanel(SpriteBatch batch, float x, float slotY, Item item) {
        if (item == null) return;

        List<String> lines = ItemDetails.buildDetailLines(item);
        if (lines.isEmpty()) return;

        font.getData().setScale(DETAIL_SCALE);
        font.setColor(Color.LIGHT_GRAY);

        float textX = x + 6;
        float textW = DETAIL_W - 12;
        float y = slotY + SLOT_H - 6;

        int shown = Math.min(lines.size(), DETAIL_MAX_LINES);
        for (int i = 0; i < shown; i++) {
            font.draw(batch, lines.get(i), textX, y - i * DETAIL_LINE_H, textW, Align.left, true);
        }

        font.getData().setScale(1f);
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
