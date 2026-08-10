package adfinir.game.ui;

import adfinir.game.ecs.components.CombatComponent;
import adfinir.game.ecs.components.InventoryComponent;
import adfinir.game.ecs.components.LootBarComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import adfinir.game.inventory.Item;
import adfinir.game.inventory.ItemModifier;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

import java.util.List;

/**
 * Barre d'objets ramassés (8 cercles) affichée en bas de l'écran.
 *
 * - Cercle en surbrillance (contour jaune) = sélection actuelle. Cible du swap
 *   au sol [F] (voir LootPickupSystem) et valeur par défaut = le premier cercle.
 * - Sélection : touches [1]-[8], ou clic gauche sur un cercle.
 *   - Item standard (Weapon/Capacity/Armor/Artifact) : équipe immédiatement
 *     (échange avec l'équipement actuel du même type, qui revient dans ce même
 *     cercle) — logique centralisée dans InventoryComponent.equipFromBarAndSync().
 *   - ItemModifier : ne s'équipe jamais directement (les 4 slots n'acceptent
 *     pas les mods). Le clic le "tient en main" (SocketInteractionState) en vue
 *     de son implantation dans un socket via InventoryOverlay.
 * - Survol souris : infobulle avec le nom et les détails de l'item (stats /
 *   combos / capacité / sockets), réutilise ItemDetails (partagé avec
 *   InventoryOverlay). Le survol d'un ItemModifier alimente aussi
 *   SocketInteractionState.hoverPreviewMod, pour que InventoryOverlay puisse
 *   surligner en vert les sockets compatibles pendant le survol.
 */
public class LootBarOverlay implements Disposable {

    private static final int   CIRCLE_COUNT   = LootBarComponent.CAPACITY; // 8
    private static final float RADIUS         = 18f;
    private static final float SPACING        = 46f;
    private static final float BOTTOM_MARGIN  = 40f;
    private static final float DETAIL_SCALE    = 1.0f;

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
     * Capte la sélection clavier [1]-[8] et le clic souris sur un cercle.
     * À appeler une fois par frame (indépendamment de draw()).
     */
    public void handleInput() {
        if (bar == null || inventory == null || playerStats == null) return;

        for (int i = 0; i < CIRCLE_COUNT; i++) {
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

    private float barStartX() {
        float totalWidth = (CIRCLE_COUNT - 1) * SPACING;
        return screenW / 2f - totalWidth / 2f;
    }

    private float circleY() {
        return BOTTOM_MARGIN;
    }

    /** Index du cercle sous la souris, ou -1. Coordonnées écran → origine haut-gauche à convertir. */
    private int hoveredIndex() {
        float mx = Gdx.input.getX();
        float my = screenH - Gdx.input.getY();
        float startX = barStartX();
        float cy = circleY();
        for (int i = 0; i < CIRCLE_COUNT; i++) {
            float cx = startX + i * SPACING;
            float dx = mx - cx;
            float dy = my - cy;
            if (dx * dx + dy * dy <= RADIUS * RADIUS) return i;
        }
        return -1;
    }

    public void draw() {
        if (bar == null) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        float startX = barStartX();
        float cy = circleY();
        int hovered = hoveredIndex();

        // Survol d'un mod : alimente l'aperçu de compatibilité consommé par InventoryOverlay.
        if (interaction != null) {
            Item hoveredItem = hovered >= 0 ? bar.slots[hovered] : null;
            interaction.hoverPreviewMod = (hoveredItem instanceof ItemModifier) ? (ItemModifier) hoveredItem : null;
        }

        // Disques : fond + couleur de rareté (ou gris si vide)
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < CIRCLE_COUNT; i++) {
            float cx = startX + i * SPACING;
            Item item = bar.slots[i];

            shapes.setColor(0f, 0f, 0f, 0.75f);
            shapes.circle(cx, cy, RADIUS + 3f, 24);

            shapes.setColor(item != null ? ItemDetails.rarityColor(item.rarity) : new Color(0.25f, 0.25f, 0.25f, 1f));
            shapes.circle(cx, cy, RADIUS, 24);
        }
        shapes.end();

        // Contours : jaune pour le slot sélectionné/tenu, doré/argenté selon le type d'item, gris si vide
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < CIRCLE_COUNT; i++) {
            float cx = startX + i * SPACING;
            Item item = bar.slots[i];
            boolean isHeldSource = interaction != null && interaction.heldFromLootBarIndex == i;
            boolean selected = (i == bar.selectedIndex) || isHeldSource;

            Color borderColor;
            if (selected) {
                borderColor = Color.YELLOW;
            } else if (item == null) {
                borderColor = Color.GRAY;
            } else if (item instanceof ItemModifier) {
                borderColor = ItemDetails.MOD_BORDER_COLOR;
            } else {
                borderColor = ItemDetails.STANDARD_BORDER_COLOR;
            }
            shapes.setColor(borderColor);
            shapes.circle(cx, cy, RADIUS + (selected ? 4f : 0f), 24);
        }
        shapes.end();

        batch.begin();

        // Numéro de raccourci (1-8) au-dessus de chaque cercle + symbole ◈ sur les mods
        font.getData().setScale(DETAIL_SCALE);
        for (int i = 0; i < CIRCLE_COUNT; i++) {
            float cx = startX + i * SPACING;
            font.setColor(Color.WHITE);
            font.draw(batch, String.valueOf(i + 1), cx - 4, cy + RADIUS + 16f);

            if (bar.slots[i] instanceof ItemModifier) {
                font.setColor(ItemDetails.MOD_BORDER_COLOR);
                font.draw(batch, "\u25C8", cx - 4, cy + 5); // ◈ overlay sur l'icône
            }
        }

        if (hovered >= 0 && bar.slots[hovered] != null) {
            // La barre est en bas de l'écran : l'infobulle s'ouvre vers le haut, ancrée par le bas.
            drawTooltip(startX + hovered * SPACING, cy + RADIUS + 8f, bar.slots[hovered]);
        }

        batch.end();
    }

    /** bottomY = bas de l'infobulle (juste au-dessus du cercle) ; le bloc entier s'étend vers le haut. */
    private void drawTooltip(float anchorX, float bottomY, Item item) {
        List<String> lines = ItemDetails.buildDetailLines(item);
        float titleH = 14f;
        float y = bottomY + titleH + lines.size() * 12f;

        font.setColor(Color.YELLOW);
        font.draw(batch, item.name, anchorX - 80, y, 160, Align.center, true);

        font.setColor(Color.LIGHT_GRAY);
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
