package adfinir.game.ui;

import adfinir.game.ecs.components.LootBarComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import adfinir.game.inventory.Item;
import adfinir.game.inventory.ItemGenerator;
import adfinir.game.inventory.Weapon;
import adfinir.game.inventory.WeaponSpriteManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

import java.util.List;

/**
 * Boutique (touche P) : 5 objets aléatoires à acheter avec l'or.
 * Un achat rejoint la loot bar (premier slot libre) — même point d'entrée que
 * le ramassage au sol (LootPickupSystem) ; l'équipement se fait ensuite via
 * la touche 1-8 ou un clic sur la barre, comme pour tout autre loot.
 */
public class ShopOverlay implements Disposable {

    private static final int OFFER_COUNT = 5;
    private static final float BOX_W = 340f;
    private static final float BOX_H = 340f;
    private static final float ROW_H = 56f;
    private static final float ROW_GAP = 6f;
    private static final float PADDING = 15f;
    private static final float ICON_BOX = 42f;
    private static final float FLASH_DURATION = 1.2f;

    private static final Color GOLD_ACCENT = new Color(0.85f, 0.68f, 0.25f, 1f);

    private final SpriteBatch   batch;
    private final BitmapFont    font;
    private final ShapeRenderer shapes;

    private boolean visible = false;
    private final Item[] offers = new Item[OFFER_COUNT];
    private final int[] prices = new int[OFFER_COUNT];

    private float flashTimer = 0f;
    private String flashText = "";

    public ShopOverlay() {
        batch  = new SpriteBatch();
        font   = new BitmapFont();
        shapes = new ShapeRenderer();
    }

    public boolean isVisible() { return visible; }

    /** Ouvre/ferme la boutique. Régénère l'offre à chaque ouverture. */
    public void toggle() {
        visible = !visible;
        if (visible) rollOffers();
    }

    private void rollOffers() {
        for (int i = 0; i < OFFER_COUNT; i++) {
            Item item;
            switch (MathUtils.random(3)) {
                case 0:  item = ItemGenerator.generateWeapon(); break;
                case 1:  item = ItemGenerator.generateArmor(); break;
                case 2:  item = ItemGenerator.generateCapacity(); break;
                default: item = ItemGenerator.generateArtifact(); break;
            }
            offers[i] = item;
            prices[i] = ItemGenerator.priceFor(item.rarity);
        }
        flashTimer = 0f;
    }

    /** Gère les achats au clavier (touches 1-5) et le décompte du flash de retour. */
    public void update(float delta, LootBarComponent bar, PlayerStatsComponent stats) {
        if (!visible) return;
        if (flashTimer > 0f) flashTimer -= delta;

        int[] keys = { Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4, Input.Keys.NUM_5 };
        for (int i = 0; i < OFFER_COUNT; i++) {
            if (offers[i] != null && Gdx.input.isKeyJustPressed(keys[i])) {
                buy(i, bar, stats);
            }
        }
    }

    private void buy(int index, LootBarComponent bar, PlayerStatsComponent stats) {
        Item item = offers[index];
        int price = prices[index];

        int slot = bar.firstEmptySlot();
        if (slot == -1) {
            flashTimer = FLASH_DURATION;
            flashText = "Barre de loot pleine !";
            return;
        }
        if (!stats.spendGold(price)) {
            flashTimer = FLASH_DURATION;
            flashText = "Pas assez d'or !";
            return;
        }

        bar.slots[slot] = item;

        flashTimer = FLASH_DURATION;
        flashText = "Acheté : " + item.name + " !";
        offers[index] = null; // vendu : slot vide jusqu'à la prochaine ouverture
    }

    public void draw(int screenW, int screenH, LootBarComponent bar, PlayerStatsComponent stats) {
        if (!visible) return;

        float boxX = (screenW - BOX_W) / 2f;
        float boxY = (screenH - BOX_H) / 2f;
        float mouseX = Gdx.input.getX();
        float mouseY = screenH - Gdx.input.getY();
        boolean clicked = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        Item hoveredItem = null;
        float rowTop = boxY + BOX_H - PADDING - 40f;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        UiTheme.panel(shapes, boxX, boxY, BOX_W, BOX_H);
        for (int i = 0; i < OFFER_COUNT; i++) {
            float rowY = rowTop - i * (ROW_H + ROW_GAP);
            boolean hovered = mouseX >= boxX + PADDING && mouseX <= boxX + BOX_W - PADDING
                && mouseY >= rowY && mouseY <= rowY + ROW_H;
            Item item = offers[i];

            Color rowBg = item == null ? UiTheme.SLOT_BG_EMPTY : (hovered ? UiTheme.SLOT_BG_HOVER : UiTheme.SLOT_BG);
            UiTheme.slotSunken(shapes, boxX + PADDING, rowY, BOX_W - PADDING * 2, ROW_H, rowBg);

            float iconX = boxX + PADDING + 6f;
            float iconY = rowY + (ROW_H - ICON_BOX) / 2f;
            UiTheme.slotSunken(shapes, iconX, iconY, ICON_BOX, ICON_BOX, UiTheme.SLOT_BG_EMPTY);
            if (item != null && !(item instanceof Weapon)) {
                ItemIcon.draw(shapes, item, iconX, iconY, ICON_BOX, ICON_BOX);
            }

            if (hovered && item != null) {
                hoveredItem = item;
                if (clicked) buy(i, bar, stats);
            }
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(GOLD_ACCENT);
        shapes.rect(boxX, boxY, BOX_W, BOX_H);
        for (int i = 0; i < OFFER_COUNT; i++) {
            float rowY = rowTop - i * (ROW_H + ROW_GAP);
            Item item = offers[i];
            shapes.setColor(item == null ? UiTheme.TEXT_DIM : ItemDetails.rarityColor(item.rarity));
            shapes.rect(boxX + PADDING, rowY, BOX_W - PADDING * 2, ROW_H);
        }
        shapes.end();

        batch.begin();

        float textX = boxX + PADDING + 6f + ICON_BOX + 10f;
        float textW = BOX_W - PADDING * 2 - (textX - (boxX + PADDING)) - 8f;
        for (int i = 0; i < OFFER_COUNT; i++) {
            float rowY = rowTop - i * (ROW_H + ROW_GAP);
            Item item = offers[i];
            if (item instanceof Weapon) {
                float iconX = boxX + PADDING + 6f;
                float iconY = rowY + (ROW_H - ICON_BOX) / 2f;
                TextureRegion region = WeaponSpriteManager.getRegion(((Weapon) item).type);
                float w = region.getRegionWidth(), h = region.getRegionHeight();
                float scale = Math.min((ICON_BOX - 6f) / w, (ICON_BOX - 6f) / h);
                float fw = w * scale, fh = h * scale;
                batch.draw(region, iconX + (ICON_BOX - fw) / 2f, iconY + (ICON_BOX - fh) / 2f, fw, fh);
            }

            if (item != null) {
                font.setColor(ItemDetails.rarityColor(item.rarity));
                font.draw(batch, "[" + (i + 1) + "] " + item.name, textX, rowY + ROW_H - 12f, textW, Align.left, true);
                font.setColor(GOLD_ACCENT);
                font.draw(batch, prices[i] + " or", textX, rowY + 18f);
            } else {
                font.setColor(UiTheme.TEXT_DIM);
                font.draw(batch, "[" + (i + 1) + "] -- vendu --", textX, rowY + ROW_H - 12f);
            }
        }

        font.getData().setScale(1.15f);
        font.setColor(UiTheme.TEXT_TITLE);
        font.draw(batch, "BOUTIQUE", boxX + PADDING, boxY + BOX_H - PADDING - 2f);
        font.getData().setScale(1f);
        font.setColor(GOLD_ACCENT);
        font.draw(batch, stats.gold + " or", boxX + PADDING, boxY + BOX_H - PADDING - 20f);
        font.setColor(UiTheme.TEXT_DIM);
        font.draw(batch, "[P] fermer", boxX + PADDING, boxY + PADDING);
        if (flashTimer > 0f) {
            font.setColor(Color.GREEN);
            font.draw(batch, flashText, boxX + PADDING, boxY + PADDING + 16f);
        }
        batch.end();

        if (hoveredItem != null) {
            drawTooltip(boxX + BOX_W + 10f, boxY + BOX_H - PADDING, hoveredItem);
        }
    }

    private void drawTooltip(float x, float y, Item item) {
        batch.begin();
        font.setColor(ItemDetails.rarityColor(item.rarity));
        font.draw(batch, item.name, x, y);

        List<String> lines = ItemDetails.buildDetailLines(item);
        font.setColor(UiTheme.TEXT_BODY);
        float ly = y - 16f;
        for (String line : lines) {
            font.draw(batch, line, x, ly, 220, Align.left, true);
            ly -= 13f;
        }
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
