package adfinir.game.ui;

import adfinir.game.inventory.*;
import adfinir.game.ecs.components.CombatComponent;
import adfinir.game.ecs.components.InventoryComponent;
import adfinir.game.ecs.components.LootBarComponent;
import adfinir.game.input.GameAction;
import adfinir.game.input.KeyBindings;
import adfinir.game.player.StatSheet;
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
 * Overlay plein écran affichant l'inventaire et l'équipement du joueur, en
 * onglets (Arme / Capacité / Armure / Artéfact) : un seul emplacement visible
 * à la fois, en grand, avec sa case-icône en creux (sprite d'arme ou icône
 * vectorielle ItemIcon) et un panneau de détails à droite :
 *  - Armure / Artéfact  : bonus de stats (+X PV Max, +X DEF, ...)
 *  - Arme               : liste des combos (dégâts, cooldown)
 *  - Capacité           : effet principal + modificateurs
 *
 * Sockets (onglets Arme / Capacité uniquement) : petites cases en creux sous
 * l'emplacement actif. Flux d'interaction (voir SocketInteractionState) :
 *  - clic sur un socket occupé, rien tenu en main  -> le tient en main (jaune)
 *  - clic sur un socket vide compatible, tenant     -> implante
 *  - clic sur un socket occupé compatible, tenant   -> échange
 *  - clic droit ou [R] sur un socket                -> extrait vers la loot bar
 *  - [Échap] (géré par GameScreen)                  -> annule le mod tenu
 * Survol d'un mod dans la LootBarOverlay -> sockets compatibles surlignés vert.
 *
 * Glisser-déposer : un item standard glissé depuis la LootBarOverlay et
 * relâché sur la case-icône de l'onglet correspondant s'équipe (voir
 * tryDropOnActiveSlot(), appelé par LootBarOverlay au relâchement du clic).
 */
public class InventoryOverlay implements Disposable {

    private final SpriteBatch   batch;
    private final BitmapFont    font;
    private final ShapeRenderer shapes;

    private boolean visible = false;
    private InventoryComponent currentInventory;
    private LootBarComponent bar;
    private CombatComponent combat;
    private StatSheet stats;
    private SocketInteractionState interaction;

    /** Onglet actif : 0=Arme, 1=Capacité, 2=Armure, 3=Artéfact. */
    private int activeTab = 0;

    // --- Mise en page ---
    private static final float MARGIN     = 24f;
    private static final float PADDING    = 16f;
    private static final float SLOT_W     = 260f;
    private static final float SLOT_H     = 128f;
    private static final float TAB_H      = 30f;
    private static final float FOOTER_H   = 20f;
    private static final float ICON_BOX   = 92f;  // case-icône (en creux)
    private static final int   TAB_COUNT  = 4;

    private static final float DETAIL_W        = 320f;
    private static final float DETAIL_LINE_H   = 15f;
    private static final float DETAIL_SCALE    = 1.0f;
    private static final int   DETAIL_MAX_LINES = 9;

    // --- Sockets (bande basse de l'emplacement, onglets Arme/Capacité) ---
    private static final float SOCKET_SIZE = 22f;
    private static final float SOCKET_GAP  = 6f;

    private static final String[] TAB_LABELS = { "Arme", "Capacité", "Armure", "Artéfact" };

    private static final float BOX_W =
        PADDING + SLOT_W + PADDING + DETAIL_W + PADDING;

    private static final float BOX_H =
        PADDING            // bord haut
            + TAB_H
            + PADDING       // séparation onglets / emplacement
            + SLOT_H
            + PADDING       // séparation emplacement / pied
            + FOOTER_H
            + PADDING;      // bord bas

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

    /** Annule le mod tenu en main (appelé par GameScreen sur [Échap] quand un mod est tenu). */
    public void cancelHeld() {
        if (interaction != null) interaction.clear();
    }

    public boolean isHoldingMod() {
        return interaction != null && interaction.isHolding();
    }

    public void update(InventoryComponent inventory, LootBarComponent bar, CombatComponent combat,
                       StatSheet stats, SocketInteractionState interaction) {
        this.currentInventory = inventory;
        this.bar = bar;
        this.combat = combat;
        this.stats = stats;
        this.interaction = interaction;
    }

    // ------------------------------------------------------------------
    // Géométrie partagée (input / rendu / drop) — l'emplacement actif a une
    // position fixe, seul son contenu change selon l'onglet.
    // ------------------------------------------------------------------

    private float boxX() { return MARGIN; }
    private float boxY() { return (Gdx.graphics.getHeight() - BOX_H) / 2f; }
    private float slotX() { return boxX() + PADDING; }
    private float tabY()  { return boxY() + BOX_H - PADDING - TAB_H; }
    private float slotY() { return tabY() - PADDING - SLOT_H; }
    private float iconX() { return slotX() + 10f; }
    private float iconY() { return slotY() + (SLOT_H - ICON_BOX) / 2f; }
    private float tabW()  { return (SLOT_W + PADDING + DETAIL_W) / TAB_COUNT; }

    private Item activeItem() {
        if (currentInventory == null) return null;
        switch (activeTab) {
            case 0:  return currentInventory.weapon;
            case 1:  return currentInventory.capacity;
            case 2:  return currentInventory.armor;
            case 3:  return currentInventory.artifact;
            default: return null;
        }
    }

    private static boolean matchesTab(Item item, int tab) {
        switch (tab) {
            case 0:  return item instanceof Weapon;
            case 1:  return item instanceof Capacity;
            case 2:  return item instanceof Armor;
            case 3:  return item instanceof Artifact;
            default: return false;
        }
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    public void handleInput() {
        if (!visible || currentInventory == null) return;

        boolean leftClick  = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        boolean rightClick = Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT);
        boolean rKey       = KeyBindings.isJustPressed(GameAction.EXTRACT_SOCKET);
        if (!leftClick && !rightClick && !rKey) return;

        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();

        // 1) Clic sur un onglet -> change l'emplacement affiché
        if (leftClick) {
            float ty = tabY();
            float tw = tabW();
            for (int t = 0; t < TAB_COUNT; t++) {
                float tx = slotX() + t * tw;
                if (mx >= tx && mx <= tx + tw && my >= ty && my <= ty + TAB_H) {
                    activeTab = t;
                    return;
                }
            }
        }

        // 2) Clic/R/clic-droit sur un socket (onglets Arme=0 / Capacité=1 uniquement)
        if (activeTab == 0 || activeTab == 1) {
            Item hostItem = activeItem();
            if (hostItem != null) {
                float baseX = slotX() + 8;
                float baseY = slotY() + 8;
                int count = socketCountOf(hostItem);
                for (int s = 0; s < count; s++) {
                    float sx = baseX + s * (SOCKET_SIZE + SOCKET_GAP);
                    if (mx >= sx && mx <= sx + SOCKET_SIZE && my >= baseY && my <= baseY + SOCKET_SIZE) {
                        if (rightClick || rKey) {
                            extractSocket(hostItem, s);
                        } else {
                            clickSocket(hostItem, s);
                        }
                        return;
                    }
                }
            }
        }
    }

    /**
     * Tente d'équiper un item standard glissé depuis la loot bar (voir
     * LootBarOverlay) : n'aboutit que si le panneau est ouvert, que l'item
     * correspond à l'onglet actif, et que le relâchement tombe sur la
     * case-icône de l'emplacement actif. Appelé par LootBarOverlay au
     * relâchement du clic gauche.
     */
    public boolean tryDropOnActiveSlot(Item item, int fromLootBarIndex, float mx, float my) {
        if (!visible || currentInventory == null || bar == null) return false;
        if (item == null || !matchesTab(item, activeTab)) return false;

        float ix = iconX(), iy = iconY();
        if (mx < ix || mx > ix + ICON_BOX || my < iy || my > iy + ICON_BOX) return false;

        currentInventory.equipFromBarAndSync(bar, fromLootBarIndex, combat, stats);
        return true;
    }

    private void clickSocket(Item hostItem, int socketIndex) {
        if (interaction == null) return;
        ItemModifier occupant = socketOf(hostItem, socketIndex);

        if (!interaction.isHolding()) {
            if (occupant != null) interaction.holdFromSocket(occupant, hostItem, socketIndex);
            return; // clic sur socket vide sans rien tenir : no-op
        }

        boolean ok;
        if (interaction.heldFromLootBarIndex >= 0) {
            ok = currentInventory.implantFromLootBarAndSync(hostItem, socketIndex, bar, interaction.heldFromLootBarIndex, stats);
        } else if (interaction.heldFromItem != null) {
            ok = currentInventory.swapSocketsAndSync(interaction.heldFromItem, interaction.heldFromSocketIndex, hostItem, socketIndex, stats);
        } else {
            ok = false;
        }
        if (ok) interaction.clear();
    }

    private void extractSocket(Item hostItem, int socketIndex) {
        if (bar == null) return;
        currentInventory.extractToLootBarAndSync(hostItem, socketIndex, bar, stats);
        if (interaction != null && interaction.heldFromItem == hostItem && interaction.heldFromSocketIndex == socketIndex) {
            interaction.clear();
        }
    }

    private ItemModifier socketOf(Item item, int index) {
        if (item instanceof Weapon) return ((Weapon) item).getSocket(index);
        if (item instanceof Capacity) return ((Capacity) item).getSocket(index);
        return null;
    }

    private int socketCountOf(Item item) {
        if (item instanceof Weapon) return ((Weapon) item).getSocketCount();
        if (item instanceof Capacity) return ((Capacity) item).getSocketCount();
        return 0;
    }

    // ------------------------------------------------------------------
    // Rendu
    // ------------------------------------------------------------------

    public void draw() {
        if (!visible || currentInventory == null) return;

        float boxX = boxX(), boxY = boxY(), slotX = slotX(), slotY = slotY();
        float detailX = slotX + SLOT_W + PADDING;
        float tabY = tabY(), tw = tabW();
        Item item = activeItem();

        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();
        boolean draggingCompatible = interaction != null && interaction.isDraggingItem()
            && matchesTab(interaction.dragItem, activeTab);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        UiTheme.panel(shapes, boxX, boxY, BOX_W, BOX_H);

        // Onglets
        for (int t = 0; t < TAB_COUNT; t++) {
            float tx = slotX + t * tw;
            boolean hovered = mx >= tx && mx <= tx + tw && my >= tabY && my <= tabY + TAB_H;
            Color tabBg = t == activeTab ? UiTheme.SLOT_BG_SELECT : (hovered ? UiTheme.SLOT_BG_HOVER : UiTheme.SLOT_BG);
            UiTheme.slotSunken(shapes, tx, tabY, tw - 2f, TAB_H, tabBg);
        }

        // Emplacement actif + panneau de détails
        UiTheme.slotSunken(shapes, slotX, slotY, SLOT_W, SLOT_H, UiTheme.SLOT_BG);
        UiTheme.slotSunken(shapes, detailX, slotY, DETAIL_W, SLOT_H, UiTheme.SLOT_BG_EMPTY);

        float ix = iconX(), iy = iconY();
        Color iconBg = draggingCompatible
            ? (mx >= ix && mx <= ix + ICON_BOX && my >= iy && my <= iy + ICON_BOX ? UiTheme.SLOT_BG_SELECT : UiTheme.SLOT_BG_HOVER)
            : UiTheme.SLOT_BG_EMPTY;
        UiTheme.slotSunken(shapes, ix, iy, ICON_BOX, ICON_BOX, iconBg);
        if (item != null && !(item instanceof Weapon)) {
            ItemIcon.draw(shapes, item, ix, iy, ICON_BOX, ICON_BOX);
        }
        shapes.end();

        // Cadres
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (int t = 0; t < TAB_COUNT; t++) {
            float tx = slotX + t * tw;
            shapes.setColor(t == activeTab ? UiTheme.PANEL_ACCENT : UiTheme.TEXT_DIM);
            shapes.rect(tx, tabY, tw - 2f, TAB_H);
        }
        shapes.setColor(item != null ? ItemDetails.rarityColor(item.rarity) : UiTheme.TEXT_DIM);
        shapes.rect(slotX, slotY, SLOT_W, SLOT_H);
        shapes.setColor(UiTheme.TEXT_DIM);
        shapes.rect(detailX, slotY, DETAIL_W, SLOT_H);
        shapes.setColor(draggingCompatible ? Color.GREEN : (item != null ? ItemDetails.rarityColor(item.rarity) : UiTheme.TEXT_DIM));
        shapes.rect(ix, iy, ICON_BOX, ICON_BOX);
        shapes.end();

        // Sockets (avant batch.begin() : ShapeRenderer/SpriteBatch ne s'interleavent pas)
        if ((activeTab == 0 || activeTab == 1) && item != null) {
            drawSocketShapes(item, slotX, slotY);
        }

        batch.begin();

        for (int t = 0; t < TAB_COUNT; t++) {
            float tx = slotX + t * tw;
            font.setColor(t == activeTab ? UiTheme.TEXT_TITLE : UiTheme.TEXT_BODY);
            font.draw(batch, TAB_LABELS[t], tx, tabY + TAB_H - 8f, tw - 2f, Align.center, false);
        }

        drawSlotContent(batch, slotX, slotY, item);
        drawDetailPanel(batch, detailX, slotY, item);

        // Pied de page
        font.setColor(UiTheme.TEXT_DIM);
        String footer = isHoldingMod()
            ? "[clic] implanter/échanger   —   [Échap] annuler"
            : "[E] fermer   —   glisser un objet depuis la barre pour l'équiper";
        font.draw(batch, footer, slotX, boxY + PADDING + FOOTER_H - 4f);

        batch.end();
    }

    /** Fonds + contours des sockets de l'emplacement actif. Doit être appelé AVANT batch.begin(). */
    private void drawSocketShapes(Item hostItem, float slotX, float slotY) {
        int count = socketCountOf(hostItem);
        if (count == 0) return;

        ItemModifier previewMod = interaction != null ? interaction.compatibilityPreviewMod() : null;
        boolean previewCompatible = previewMod != null && previewMod.isCompatibleWith(hostItem);

        float baseX = slotX + 8;
        float baseY = slotY + 8;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int s = 0; s < count; s++) {
            ItemModifier occupant = socketOf(hostItem, s);
            float sx = baseX + s * (SOCKET_SIZE + SOCKET_GAP);
            Color bg = occupant == null ? UiTheme.SLOT_BG_EMPTY
                : (occupant instanceof WeaponComboMod
                    ? new Color(0.45f, 0.16f, 0.16f, 1f)   // rouge sourd : combo offensif
                    : new Color(0.16f, 0.24f, 0.45f, 1f)); // bleu sourd : effet magique
            UiTheme.slotSunken(shapes, sx, baseY, SOCKET_SIZE, SOCKET_SIZE, bg);
            if (occupant != null) {
                ItemIcon.draw(shapes, occupant, sx, baseY, SOCKET_SIZE, SOCKET_SIZE);
            }
        }
        shapes.end();

        for (int s = 0; s < count; s++) {
            float sx = baseX + s * (SOCKET_SIZE + SOCKET_GAP);
            ItemModifier occupant = socketOf(hostItem, s);
            boolean isHeldSource = interaction != null && interaction.heldFromItem == hostItem
                && interaction.heldFromSocketIndex == s;

            Color border;
            if (isHeldSource) border = Color.YELLOW;
            else if (previewCompatible) border = Color.GREEN;
            else if (occupant != null) border = ItemDetails.rarityColor(occupant.rarity);
            else border = UiTheme.TEXT_DIM;

            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(border);
            if (occupant == null) {
                drawDashedRect(sx, baseY, SOCKET_SIZE, SOCKET_SIZE);
            } else {
                shapes.rect(sx, baseY, SOCKET_SIZE, SOCKET_SIZE);
            }
            shapes.end();
        }
    }

    /** Contour pointillé (ShapeRenderer ne supporte pas les traits en tirets nativement). */
    private void drawDashedRect(float x, float y, float w, float h) {
        float dash = 3f;
        for (float dx = 0; dx < w; dx += dash * 2) {
            shapes.line(x + dx, y, x + Math.min(dx + dash, w), y);
            shapes.line(x + dx, y + h, x + Math.min(dx + dash, w), y + h);
        }
        for (float dy = 0; dy < h; dy += dash * 2) {
            shapes.line(x, y + dy, x, y + Math.min(dy + dash, h));
            shapes.line(x + w, y + dy, x + w, y + Math.min(dy + dash, h));
        }
    }

    private void drawSlotContent(SpriteBatch batch, float x, float y, Item item) {
        float iconZoneW = ICON_BOX + 20f;
        float textX = x + iconZoneW;
        float textZoneW = SLOT_W - iconZoneW - 10f;

        // Sprite d'arme (seul type avec un vrai sprite ; les autres ont déjà leur ItemIcon dessiné en dessous)
        if (item instanceof Weapon) {
            float bx = iconX(), by = iconY();
            TextureRegion region = WeaponSpriteManager.getRegion(((Weapon) item).type);
            float w = region.getRegionWidth();
            float h = region.getRegionHeight();
            float scale = Math.min((ICON_BOX - 10f) / w, (ICON_BOX - 10f) / h);
            float finalW = w * scale;
            float finalH = h * scale;
            batch.draw(region, bx + (ICON_BOX - finalW) / 2f, by + (ICON_BOX - finalH) / 2f, finalW, finalH);
        }

        if (item != null) {
            // Rangée 1 (tout en haut) : étiquette de rareté, alignée à droite
            font.setColor(ItemDetails.rarityColor(item.rarity));
            font.draw(batch, rarityLabel(item.rarity), textX, y + SLOT_H - 12, textZoneW, Align.right, false);

            // Rangée 2 : nom de l'objet, en dessous, sans chevaucher la rareté
            font.getData().setScale(1.15f);
            font.draw(batch, item.name, textX, y + SLOT_H - 32, textZoneW, Align.left, true);
            font.getData().setScale(1f);

            // Rangée 3 : description courte
            font.setColor(UiTheme.TEXT_BODY);
            String desc = item.getDescription();
            int maxChars = 46;
            if (desc.length() > maxChars) desc = desc.substring(0, maxChars - 3) + "...";
            font.draw(batch, desc, textX, y + SLOT_H - 56, textZoneW, Align.left, true);
            // La bande basse de l'emplacement (sockets, onglets Arme/Capacité) est dessinée
            // séparément par drawSocketShapes() — rien à ajouter ici, pas de chevauchement.
        } else {
            font.getData().setScale(1.1f);
            font.setColor(UiTheme.TEXT_DIM);
            font.draw(batch, "-- emplacement vide --", textX, y + SLOT_H - 16);
            font.getData().setScale(1f);
            font.draw(batch, "Glisse un objet compatible depuis la barre.", textX, y + SLOT_H - 40, textZoneW, Align.left, true);
        }
    }

    private static String rarityLabel(Rarity r) {
        switch (r) {
            case LEGENDARY: return "LÉGENDAIRE";
            case MYTHICAL:  return "MYTHIQUE";
            case EPIC:      return "ÉPIQUE";
            case RARE:      return "RARE";
            case COMMON:
            default:        return "COMMUN";
        }
    }

    /**
     * Panneau de détails à droite de l'emplacement : stats (Armure/Artéfact),
     * combos + sockets (Arme) ou effet + modificateurs + sockets (Capacité).
     * Les lignes de section ("Combos :", "Sockets :"...) ressortent en accent.
     */
    private void drawDetailPanel(SpriteBatch batch, float x, float slotY, Item item) {
        float textX = x + 12;
        float textW = DETAIL_W - 24;
        float y = slotY + SLOT_H - 16;

        if (item == null) {
            font.setColor(UiTheme.TEXT_DIM);
            font.draw(batch, "Rien d'équipé dans cet emplacement.", textX, y, textW, Align.left, true);
            return;
        }

        List<String> lines = ItemDetails.buildDetailLines(item);
        if (lines.isEmpty()) return;

        font.getData().setScale(DETAIL_SCALE);

        int shown = Math.min(lines.size(), DETAIL_MAX_LINES);
        for (int i = 0; i < shown; i++) {
            String line = lines.get(i);
            font.setColor(line.endsWith(":") ? UiTheme.PANEL_ACCENT : UiTheme.TEXT_BODY);
            font.draw(batch, line, textX, y - i * DETAIL_LINE_H, textW, Align.left, true);
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
