package adfinir.game.ui;

import adfinir.game.inventory.*;
import adfinir.game.ecs.components.InventoryComponent;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;

/**
 * Overlay d'inventaire à onglets : ARME / ARMURE / SORT / ARTEFACT.
 * Onglets cliquables (ou Tab / Shift+Tab au clavier), tooltip au survol d'un objet.
 */
public class InventoryOverlay implements Disposable {

    private enum Tab { ARME, ARMURE, SORT, ARTEFACT }

    private static final Color BG          = new Color(0.06f, 0.06f, 0.09f, 0.93f);
    private static final Color SLOT_BG     = new Color(0.14f, 0.14f, 0.19f, 1f);
    private static final Color SLOT_BG_HOV = new Color(0.22f, 0.22f, 0.30f, 1f);
    private static final Color TAB_BG      = new Color(0.10f, 0.10f, 0.14f, 1f);
    private static final Color TAB_BG_SEL  = new Color(0.32f, 0.30f, 0.55f, 1f);

    private final SpriteBatch    batch;
    private final BitmapFont     font;
    private final ShapeRenderer  shapes;

    private boolean visible = false;
    private InventoryComponent currentInventory;
    private Tab currentTab = Tab.ARME;

    // Mise en page
    private static final float MARGIN     = 20f;
    private static final float BOX_W      = 240f;
    private static final float BOX_H      = 280f;
    private static final float SLOT_W     = 200f;
    private static final float SLOT_H     = 70f;
    private static final float SMALL_SLOT = 92f;
    private static final float PADDING    = 15f;
    private static final float TAB_H      = 26f;

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

        // Navigation clavier : Tab (suivant) / Shift+Tab (précédent)
        if (visible && Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            int dir = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ? -1 : 1;
            cycleTab(dir);
        }
    }

    private void cycleTab(int dir) {
        int next = (currentTab.ordinal() + dir + Tab.values().length) % Tab.values().length;
        currentTab = Tab.values()[next];
    }

    public void draw() {
        if (!visible || currentInventory == null) return;

        int screenH = Gdx.graphics.getHeight();
        float boxX = MARGIN;
        float boxY = (screenH - BOX_H) / 2f;
        float mouseX = UiInput.mouseX();
        float mouseY = UiInput.mouseY();
        boolean clicked = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(BG);
        shapes.rect(boxX, boxY, BOX_W, BOX_H);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.LIGHT_GRAY);
        shapes.rect(boxX, boxY, BOX_W, BOX_H);
        shapes.end();

        drawTabHeader(boxX, boxY, mouseX, mouseY, clicked);

        Item hoveredItem = null;
        switch (currentTab) {
            case ARME:     hoveredItem = drawSingleSlot(boxX, boxY, "Arme", currentInventory.weapon, mouseX, mouseY); break;
            case ARMURE:   hoveredItem = drawSingleSlot(boxX, boxY, "Armure", currentInventory.armor, mouseX, mouseY); break;
            case ARTEFACT: hoveredItem = drawSingleSlot(boxX, boxY, "Artéfact", currentInventory.artifact, mouseX, mouseY); break;
            case SORT:     hoveredItem = drawSpellGrid(boxX, boxY, mouseX, mouseY); break;
        }

        batch.begin();
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "[Tab] onglet    [E] fermer", boxX + PADDING, boxY + PADDING + 5f);
        batch.end();

        if (hoveredItem != null) {
            Tooltip.draw(shapes, batch, font, mouseX, mouseY, hoveredItem.name + " (" + hoveredItem.rarity.name() + ")");
        }
    }

    /** Dessine les 4 boutons d'onglets, gère le clic. Retourne rien : cliquer change directement currentTab. */
    private void drawTabHeader(float boxX, float boxY, float mouseX, float mouseY, boolean clicked) {
        Tab[] tabs = Tab.values();
        float tabW = (BOX_W - PADDING * 2) / tabs.length;
        float tabY = boxY + BOX_H - PADDING - TAB_H;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < tabs.length; i++) {
            float tx = boxX + PADDING + i * tabW;
            Rectangle rect = new Rectangle(tx, tabY, tabW - 3f, TAB_H);
            boolean hovered = rect.contains(mouseX, mouseY);
            shapes.setColor(tabs[i] == currentTab ? TAB_BG_SEL : (hovered ? SLOT_BG_HOV : TAB_BG));
            shapes.rect(rect.x, rect.y, rect.width, rect.height);
            if (hovered && clicked) currentTab = tabs[i];
        }
        shapes.end();

        batch.begin();
        for (int i = 0; i < tabs.length; i++) {
            float tx = boxX + PADDING + i * tabW;
            font.setColor(tabs[i] == currentTab ? Color.YELLOW : Color.LIGHT_GRAY);
            font.draw(batch, tabs[i].name(), tx + 4f, tabY + 17f);
        }
        batch.end();
    }

    private Item drawSingleSlot(float boxX, float boxY, String label, Item item, float mouseX, float mouseY) {
        float slotX = boxX + (BOX_W - SLOT_W) / 2f;
        float slotY = boxY + BOX_H - PADDING - TAB_H - 15f - SLOT_H;
        return drawSlot(slotX, slotY, SLOT_W, SLOT_H, label, item, mouseX, mouseY);
    }

    /** Onglet SORT : grille 2x2 des 4 sorts équipés (touches 1-4 dans la hotbar). */
    private Item drawSpellGrid(float boxX, float boxY, float mouseX, float mouseY) {
        float gap = 10f;
        float slotW = (BOX_W - PADDING * 2 - gap) / 2f;
        float top = boxY + BOX_H - PADDING - TAB_H - 15f;

        Item hovered = null;
        for (int i = 0; i < InventoryComponent.SPELL_SLOTS; i++) {
            float col = i % 2;
            float row = i / 2;
            float sx = boxX + PADDING + col * (slotW + gap);
            float sy = top - SMALL_SLOT - row * (SMALL_SLOT + gap);
            Item found = drawSlot(sx, sy, slotW, SMALL_SLOT, (i + 1) + ". Sort", currentInventory.spells[i], mouseX, mouseY);
            if (found != null) hovered = found;
        }
        return hovered;
    }

    /** Dessine un slot (fond + bordure de rareté + icône + texte) et retourne l'item si survolé. */
    private Item drawSlot(float x, float y, float w, float h, String label, Item item, float mouseX, float mouseY) {
        Rectangle rect = new Rectangle(x, y, w, h);
        boolean hovered = rect.contains(mouseX, mouseY);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(hovered ? SLOT_BG_HOV : SLOT_BG);
        shapes.rect(x, y, w, h);
        if (item != null) {
            ItemIcon.draw(shapes, item, x, y + 10f, w, h - 10f);
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(slotBorderColor(item));
        shapes.rect(x, y, w, h);
        shapes.end();

        batch.begin();
        font.setColor(Color.GRAY);
        font.draw(batch, label, x + 5, y + h - 5);

        if (item instanceof Weapon) {
            com.badlogic.gdx.graphics.g2d.TextureRegion region = WeaponSpriteManager.getRegion(((Weapon) item).type);
            float rw = region.getRegionWidth();
            float rh = region.getRegionHeight();
            float scale = Math.min(30f / rw, 30f / rh);
            batch.draw(region, x + (w - rw * scale) / 2f, y + (h - rh * scale) / 2f + 5f, rw * scale, rh * scale);
        }

        if (item != null) {
            font.setColor(RarityColors.get(item.rarity));
            font.draw(batch, item.name, x + 5, y + h - 18);
        } else {
            font.setColor(Color.DARK_GRAY);
            font.draw(batch, "Vide", x + 5, y + h - 18);
        }
        batch.end();

        return (hovered && item != null) ? item : null;
    }

    private Color slotBorderColor(Item item) {
        return item == null ? Color.DARK_GRAY : RarityColors.get(item.rarity);
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
