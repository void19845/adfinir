package adfinir.game.ui;

import adfinir.game.ecs.components.CombatComponent;
import adfinir.game.ecs.components.InventoryComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import adfinir.game.inventory.Armor;
import adfinir.game.inventory.Artifact;
import adfinir.game.inventory.Capacity;
import adfinir.game.inventory.Item;
import adfinir.game.inventory.ItemGenerator;
import adfinir.game.inventory.Weapon;
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
 * Boutique (touche P) : 5 objets aléatoires à acheter avec l'or.
 * Cliquable à la souris (ou touches 1-5), survol = tooltip, achat = équipement immédiat.
 */
public class ShopOverlay implements Disposable {

    private static final int OFFER_COUNT = 5;
    private static final float BOX_W = 300f;
    private static final float BOX_H = 320f;
    private static final float ROW_H = 52f;
    private static final float PADDING = 15f;
    private static final float FLASH_DURATION = 1.2f;

    private static final Color BG      = new Color(0.06f, 0.06f, 0.09f, 0.94f);
    private static final Color ROW_BG  = new Color(0.14f, 0.14f, 0.19f, 1f);
    private static final Color ROW_HOV = new Color(0.24f, 0.22f, 0.30f, 1f);
    private static final Color ROW_SOLD= new Color(0.08f, 0.08f, 0.08f, 1f);

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
            switch (com.badlogic.gdx.math.MathUtils.random(3)) {
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

    /** Gère les achats au clavier (touches 1-5) et le décompte du flash "Acheté !". */
    public void update(float delta, InventoryComponent inv, PlayerStatsComponent stats, CombatComponent combat) {
        if (!visible) return;
        if (flashTimer > 0f) flashTimer -= delta;

        int[] keys = { Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4, Input.Keys.NUM_5 };
        for (int i = 0; i < OFFER_COUNT; i++) {
            if (offers[i] != null && Gdx.input.isKeyJustPressed(keys[i])) {
                buy(i, inv, stats, combat);
            }
        }
    }

    private void buy(int index, InventoryComponent inv, PlayerStatsComponent stats, CombatComponent combat) {
        Item item = offers[index];
        int price = prices[index];
        if (!stats.spendGold(price)) {
            flashTimer = FLASH_DURATION;
            flashText = "Pas assez d'or !";
            return;
        }

        if (item instanceof Weapon) {
            inv.equipWeapon((Weapon) item);
            combat.weapon = (Weapon) item;
        } else if (item instanceof Armor) {
            inv.equipArmor((Armor) item);
        } else if (item instanceof Capacity) {
            inv.equipSpellAuto((Capacity) item);
        } else if (item instanceof Artifact) {
            inv.equipArtifact((Artifact) item);
        }
        inv.updateStats(stats.stats);

        flashTimer = FLASH_DURATION;
        flashText = "Acheté : " + item.name + " !";
        offers[index] = null; // vendu : slot vide jusqu'à la prochaine ouverture
    }

    public void draw(int screenW, int screenH, InventoryComponent inv, PlayerStatsComponent stats, CombatComponent combat) {
        if (!visible) return;

        float boxX = (screenW - BOX_W) / 2f;
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
        shapes.setColor(Color.GOLD);
        shapes.rect(boxX, boxY, BOX_W, BOX_H);
        shapes.end();

        Item hoveredItem = null;
        float rowTop = boxY + BOX_H - PADDING - 34f;

        for (int i = 0; i < OFFER_COUNT; i++) {
            float rowY = rowTop - i * (ROW_H + 6f);
            Rectangle rowRect = new Rectangle(boxX + PADDING, rowY, BOX_W - PADDING * 2, ROW_H);
            boolean hovered = rowRect.contains(mouseX, mouseY);
            Item item = offers[i];

            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(item == null ? ROW_SOLD : (hovered ? ROW_HOV : ROW_BG));
            shapes.rect(rowRect.x, rowRect.y, rowRect.width, rowRect.height);
            if (item != null) {
                ItemIcon.draw(shapes, item, rowRect.x, rowRect.y, 48f, rowRect.height);
            }
            shapes.end();

            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(item == null ? Color.DARK_GRAY : RarityColors.get(item.rarity));
            shapes.rect(rowRect.x, rowRect.y, rowRect.width, rowRect.height);
            shapes.end();

            batch.begin();
            if (item != null) {
                float textX = rowRect.x + 55f;
                if (item instanceof Weapon) {
                    com.badlogic.gdx.graphics.g2d.TextureRegion region =
                        adfinir.game.inventory.WeaponSpriteManager.getRegion(((Weapon) item).type);
                    float scale = Math.min(36f / region.getRegionWidth(), 36f / region.getRegionHeight());
                    batch.draw(region, rowRect.x + 6f, rowRect.y + (ROW_H - region.getRegionHeight() * scale) / 2f,
                        region.getRegionWidth() * scale, region.getRegionHeight() * scale);
                }
                font.setColor(RarityColors.get(item.rarity));
                font.draw(batch, "[" + (i + 1) + "] " + item.name, textX, rowRect.y + ROW_H - 12f);
                font.setColor(Color.GOLD);
                font.draw(batch, prices[i] + " or", textX, rowRect.y + 16f);
            } else {
                font.setColor(Color.DARK_GRAY);
                font.draw(batch, "[" + (i + 1) + "] -- vendu --", rowRect.x + 55f, rowRect.y + ROW_H - 12f);
            }
            batch.end();

            if (hovered && item != null) {
                hoveredItem = item;
                if (clicked) buy(i, inv, stats, combat);
            }
        }

        batch.begin();
        font.setColor(Color.YELLOW);
        font.draw(batch, "--- BOUTIQUE ---   Or : " + stats.gold, boxX + PADDING, boxY + BOX_H - PADDING);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "[P] fermer", boxX + PADDING, boxY + PADDING);
        if (flashTimer > 0f) {
            font.setColor(Color.GREEN);
            font.draw(batch, flashText, boxX + PADDING, boxY + PADDING + 16f);
        }
        batch.end();

        if (hoveredItem != null) {
            Tooltip.draw(shapes, batch, font, mouseX, mouseY, hoveredItem.name + " (" + hoveredItem.rarity.name() + ") - " + hoveredItem.getDescription());
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
