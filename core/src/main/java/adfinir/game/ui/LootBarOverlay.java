package adfinir.game.ui;

import adfinir.game.ecs.components.CombatComponent;
import adfinir.game.ecs.components.InventoryComponent;
import adfinir.game.ecs.components.LootBarComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import adfinir.game.inventory.Item;
import adfinir.game.inventory.ItemModifier;
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
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

import java.util.List;

/**
 * Barre d'objets ramassés (8 cases carrées, style hotbar) affichée en bas de
 * l'écran.
 *
 * - Case en surbrillance jaune = sélection actuelle. Cible du swap au sol
 *   [F] (voir LootPickupSystem) et valeur par défaut = la première case.
 * - Sélection : touches [1]-[8], ou clic gauche sur une case.
 *   - Item standard (Weapon/Capacity/Armor/Artifact) : équipe immédiatement
 *     (échange avec l'équipement actuel du même type, qui revient dans cette
 *     même case) — logique centralisée dans InventoryComponent.equipFromBarAndSync().
 *   - ItemModifier : ne s'équipe jamais directement (les 4 slots n'acceptent
 *     pas les mods). Le clic le "tient en main" (SocketInteractionState) en vue
 *     de son implantation dans un socket via InventoryOverlay.
 * - Chaque case affiche l'icône réelle de l'item (sprite d'arme ou ItemIcon
 *   vectorielle) pour qu'on reconnaisse l'objet sans avoir à lire son nom.
 * - Survol souris : infobulle avec le nom et les détails de l'item (stats /
 *   combos / capacité / sockets), réutilise ItemDetails (partagé avec
 *   InventoryOverlay). Le survol d'un ItemModifier alimente aussi
 *   SocketInteractionState.hoverPreviewMod, pour que InventoryOverlay puisse
 *   surligner en vert les sockets compatibles pendant le survol.
 */
public class LootBarOverlay implements Disposable {

    private static final int   SLOT_COUNT    = LootBarComponent.CAPACITY; // 8
    private static final float SLOT_SIZE     = 44f;
    private static final float SLOT_GAP      = 4f;
    private static final float SPACING       = SLOT_SIZE + SLOT_GAP;
    private static final float BOTTOM_MARGIN = 14f;
    private static final float DETAIL_SCALE  = 1.0f;

    private final SpriteBatch   batch;
    private final BitmapFont    font;
    private final ShapeRenderer shapes;

    private int screenW, screenH;

    private LootBarComponent      bar;
    private InventoryComponent    inventory;
    private CombatComponent       combat;
    private PlayerStatsComponent  playerStats;
    private SocketInteractionState interaction;

    public LootBarOverlay() {
        batch  = new SpriteBatch();
        font   = new BitmapFont();
        font.getData().setScale(DETAIL_SCALE);
        shapes = new ShapeRenderer();
    }

    /** À rafraîchir chaque frame avec les références courantes du joueur. */
    public void update(LootBarComponent bar, InventoryComponent inventory,
                       CombatComponent combat, PlayerStatsComponent playerStats,
                       SocketInteractionState interaction) {
        this.bar = bar;
        this.inventory = inventory;
        this.combat = combat;
        this.playerStats = playerStats;
        this.interaction = interaction;
    }

    /**
     * Capte la sélection clavier [1]-[8] et le clic souris sur une case.
     * À appeler une fois par frame (indépendamment de draw()).
     */
    public void handleInput() {
        if (bar == null || inventory == null || playerStats == null) return;

        for (int i = 0; i < SLOT_COUNT; i++) {
            int key = Input.Keys.NUM_1 + i;
            if (Gdx.input.isKeyJustPressed(key)) {
                select(i);
                break;
            }
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            int hovered = hoveredIndex();
            if (hovered >= 0) select(hovered);
        }
    }

    /** Équipe (item standard) ou tient en main (ItemModifier) le slot cliqué/sélectionné au clavier. */
    private void select(int index) {
        Item item = bar.slots[index];
        if (item == null) return;

        if (item instanceof ItemModifier) {
            bar.selectedIndex = index;
            if (interaction != null) interaction.holdFromLootBar((ItemModifier) item, index);
            return;
        }
        inventory.equipFromBarAndSync(bar, index, combat, playerStats.stats);
    }

    /** Largeur totale de la barre — exposée pour que StatsOverlay aligne ses barres HP/Stamina dessus. */
    public static float totalWidth() {
        return (SLOT_COUNT - 1) * SPACING + SLOT_SIZE;
    }

    /** X de départ de la barre pour une largeur d'écran donnée — voir totalWidth(). */
    public static float startX(int screenW) {
        return screenW / 2f - totalWidth() / 2f;
    }

    /** Y du bas de la barre — voir topY(). */
    public static float bottomY() {
        return BOTTOM_MARGIN;
    }

    /** Y du haut de la barre — point d'ancrage pour tout ce qui se dessine juste au-dessus. */
    public static float topY() {
        return BOTTOM_MARGIN + SLOT_SIZE;
    }

    private float barStartX() {
        return startX(screenW);
    }

    private float slotY() {
        return bottomY();
    }

    /** Index de la case sous la souris, ou -1. Coordonnées écran → origine haut-gauche à convertir. */
    private int hoveredIndex() {
        float mx = Gdx.input.getX();
        float my = screenH - Gdx.input.getY();
        float startX = barStartX();
        float sy = slotY();
        for (int i = 0; i < SLOT_COUNT; i++) {
            float sx = startX + i * SPACING;
            if (mx >= sx && mx <= sx + SLOT_SIZE && my >= sy && my <= sy + SLOT_SIZE) return i;
        }
        return -1;
    }

    public void draw() {
        if (bar == null) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        float startX = barStartX();
        float sy = slotY();
        int hovered = hoveredIndex();

        // Survol d'un mod : alimente l'aperçu de compatibilité consommé par InventoryOverlay.
        if (interaction != null) {
            Item hoveredItem = hovered >= 0 ? bar.slots[hovered] : null;
            interaction.hoverPreviewMod = (hoveredItem instanceof ItemModifier) ? (ItemModifier) hoveredItem : null;
        }

        // Cases en creux (fond ardoise + bevel) + icône vectorielle des items non-armes
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < SLOT_COUNT; i++) {
            float sx = startX + i * SPACING;
            Item item = bar.slots[i];
            boolean selected = (i == bar.selectedIndex)
                || (interaction != null && interaction.heldFromLootBarIndex == i);
            Color bg = selected ? UiTheme.SLOT_BG_SELECT
                : (i == hovered ? UiTheme.SLOT_BG_HOVER : (item == null ? UiTheme.SLOT_BG_EMPTY : UiTheme.SLOT_BG));
            UiTheme.slotSunken(shapes, sx, sy, SLOT_SIZE, SLOT_SIZE, bg);

            if (item != null && !(item instanceof Weapon)) {
                float pad = 6f;
                ItemIcon.draw(shapes, item, sx + pad, sy + pad, SLOT_SIZE - pad * 2, SLOT_SIZE - pad * 2);
            }
        }
        shapes.end();

        // Cadres : jaune pour le slot sélectionné/tenu, sinon rareté (gris terne si vide)
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < SLOT_COUNT; i++) {
            float sx = startX + i * SPACING;
            Item item = bar.slots[i];
            boolean isHeldSource = interaction != null && interaction.heldFromLootBarIndex == i;
            boolean selected = (i == bar.selectedIndex) || isHeldSource;

            Color borderColor = selected ? Color.YELLOW
                : (item != null ? ItemDetails.rarityColor(item.rarity) : UiTheme.TEXT_DIM);
            shapes.setColor(borderColor);
            shapes.rect(sx, sy, SLOT_SIZE, SLOT_SIZE);
        }
        shapes.end();

        batch.begin();

        // Sprite d'arme (seul type avec un vrai sprite ; le reste a déjà son ItemIcon dessiné en dessous)
        for (int i = 0; i < SLOT_COUNT; i++) {
            Item item = bar.slots[i];
            if (!(item instanceof Weapon)) continue;
            float sx = startX + i * SPACING;
            float pad = 5f;
            float maxDim = SLOT_SIZE - pad * 2;
            TextureRegion region = WeaponSpriteManager.getRegion(((Weapon) item).type);
            float w = region.getRegionWidth();
            float h = region.getRegionHeight();
            float scale = Math.min(maxDim / w, maxDim / h);
            float finalW = w * scale;
            float finalH = h * scale;
            batch.draw(region, sx + (SLOT_SIZE - finalW) / 2f, sy + (SLOT_SIZE - finalH) / 2f, finalW, finalH);
        }

        // Numéro de raccourci (1-8), petit badge en haut à gauche de chaque case
        font.getData().setScale(DETAIL_SCALE);
        for (int i = 0; i < SLOT_COUNT; i++) {
            float sx = startX + i * SPACING;
            font.setColor(bar.slots[i] != null ? UiTheme.TEXT_BODY : UiTheme.TEXT_DIM);
            font.draw(batch, String.valueOf(i + 1), sx + 3f, sy + SLOT_SIZE - 3f);
        }

        if (hovered >= 0 && bar.slots[hovered] != null) {
            // La barre est en bas de l'écran : l'infobulle s'ouvre vers le haut, ancrée par le bas.
            drawTooltip(startX + hovered * SPACING + SLOT_SIZE / 2f, sy + SLOT_SIZE + 8f, bar.slots[hovered]);
        }

        batch.end();
    }

    /** bottomY = bas de l'infobulle (juste au-dessus de la case) ; le bloc entier s'étend vers le haut. */
    private void drawTooltip(float anchorX, float bottomY, Item item) {
        List<String> lines = ItemDetails.buildDetailLines(item);
        float titleH = 14f;
        float y = bottomY + titleH + lines.size() * 12f;

        font.setColor(ItemDetails.rarityColor(item.rarity));
        font.draw(batch, item.name, anchorX - 80, y, 160, Align.center, true);

        font.setColor(UiTheme.TEXT_BODY);
        y -= titleH;
        for (String line : lines) {
            font.draw(batch, line, anchorX - 100, y, 200, Align.center, true);
            y -= 12f;
        }
    }

    public void resize(int w, int h) {
        screenW = w;
        screenH = h;
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
