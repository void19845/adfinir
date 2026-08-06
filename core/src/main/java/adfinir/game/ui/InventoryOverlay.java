package adfinir.game.ui;

import adfinir.game.inventory.*;
import adfinir.game.ecs.components.InventoryComponent;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;

/**
 * Overlay plein écran affichant l'inventaire et l'équipement du joueur.
 */
public class InventoryOverlay implements Disposable {

    private final SpriteBatch    batch;
    private final BitmapFont     font;
    private final ShapeRenderer  shapes;

    private boolean visible = false;
    private InventoryComponent currentInventory;

    // Mise en page
    private static final float MARGIN     = 20f;
    private static final float BOX_W      = 200f;
    private static final float BOX_H      = 350f;
    private static final float SLOT_W     = 160f;
    private static final float SLOT_H     = 60f;
    private static final float PADDING    = 15f;

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

        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        // Positionné à gauche de l'écran
        float boxX = MARGIN;
        float boxY = (screenH - BOX_H) / 2f;

        // 1. Render all shapes first
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.85f);
        shapes.rect(boxX, boxY, BOX_W, BOX_H);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.GRAY);

        float currentY = boxY + BOX_H - PADDING - 60f;

        // Slots alignés verticalement (un en dessous de l'autre)
        shapes.rect(boxX + PADDING, currentY, SLOT_W, SLOT_H);
        currentY -= SLOT_H + PADDING;
        shapes.rect(boxX + PADDING, currentY, SLOT_W, SLOT_H);
        currentY -= SLOT_H + PADDING;
        shapes.rect(boxX + PADDING, currentY, SLOT_W, SLOT_H);
        currentY -= SLOT_H + PADDING;
        shapes.rect(boxX + PADDING, currentY, SLOT_W, SLOT_H);
        shapes.end();

        // 2. Render all sprites and text second
        batch.begin();

        float textY = boxY + BOX_H - PADDING - 20f;
        font.setColor(Color.YELLOW);
        font.draw(batch, "--- INVENTAIRE ---", boxX + PADDING, textY);

        float slotY = boxY + BOX_H - PADDING - 60f;

        drawSlotContent(batch, font, boxX + PADDING, slotY, "Arme", currentInventory.weapon);
        slotY -= SLOT_H + PADDING;
        drawSlotContent(batch, font, boxX + PADDING, slotY, "Capacité", currentInventory.capacity);
        slotY -= SLOT_H + PADDING;
        drawSlotContent(batch, font, boxX + PADDING, slotY, "Armure", currentInventory.armor);
        slotY -= SLOT_H + PADDING;
        drawSlotContent(batch, font, boxX + PADDING, slotY, "Artéfact", currentInventory.artifact);

        float footerY = boxY + PADDING + 20f;
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "[E] fermer l'inventaire", boxX + PADDING, footerY);

        batch.end();
    }

    private void drawSlotContent(SpriteBatch batch, BitmapFont font, float x, float y, String label, Item item) {
        // Label
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, label, x + 5, y + SLOT_H - 5);

        if (item != null) {
            // Draw sprite if it's a weapon
            if (item instanceof Weapon) {
                com.badlogic.gdx.graphics.g2d.TextureRegion region = WeaponSpriteManager.getRegion(((Weapon) item).type);

                // Scale the sprite to fit nicely in the slot (max 40x40)
                float w = region.getRegionWidth();
                float h = region.getRegionHeight();
                float scale = Math.min(40f / w, 40f / h);
                float finalW = w * scale;
                float finalH = h * scale;

                batch.draw(region, x + (SLOT_W - finalW) / 2f, y + (SLOT_H - finalH) / 2f, finalW, finalH);
            }

            // Name and Rarity
            font.setColor(getRarityColor(item.rarity));
            font.draw(batch, item.name, x + 5, y + SLOT_H - 20);

            // Short description
            font.setColor(Color.WHITE);
            String desc = item.getDescription();
            if (desc.length() > 25) desc = desc.substring(0, 22) + "...";
            font.draw(batch, desc, x + 5, y + SLOT_H - 35);
        } else {
            font.setColor(Color.DARK_GRAY);
            font.draw(batch, "Vide", x + 5, y + SLOT_H - 20);
        }
    }

    private Color getRarityColor(Rarity rarity) {
        switch (rarity) {
            case LEGENDARY: return Color.ORANGE;
            case EPIC:      return Color.PURPLE;
            case RARE:      return Color.CYAN;
            case COMMON:   return Color.WHITE;
            default:        return Color.WHITE;
        }
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
