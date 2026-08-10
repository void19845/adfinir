package adfinir.game.ui;

import adfinir.game.inventory.*;
import adfinir.game.ecs.components.InventoryComponent;
import adfinir.game.ecs.components.LootBarComponent;
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
 *
 * Sockets (arme / capacité) : cliquer sur la ligne Arme ou Capacité la
 * sélectionne (contour cyan) et affiche ses sockets en petits carrés dans la
 * bande basse du slot. Flux d'interaction (voir SocketInteractionState) :
 *  - clic sur un socket occupé, rien tenu en main  -> le tient en main (jaune)
 *  - clic sur un socket vide compatible, tenant     -> implante
 *  - clic sur un socket occupé compatible, tenant   -> échange
 *  - clic droit ou [R] sur un socket                -> extrait vers la loot bar
 *  - [Échap] (géré par GameScreen)                  -> annule le mod tenu
 * Survol d'un mod dans la LootBarOverlay -> sockets compatibles surlignés vert.
 */
public class InventoryOverlay implements Disposable {

    private final SpriteBatch   batch;
    private final BitmapFont    font;
    private final ShapeRenderer shapes;

    private boolean visible = false;
    private InventoryComponent currentInventory;
    private LootBarComponent bar;
    private StatSheet stats;
    private SocketInteractionState interaction;

    /** Slot actuellement sélectionné pour l'affichage des sockets (0=Arme, 1=Capacité), -1 = aucun. */
    private int selectedSlotIndex = -1;

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
    private static final float DETAIL_SCALE    = 1.0f;
    private static final int   DETAIL_MAX_LINES = 5;   // sécurité anti-débordement du slot

    // --- Sockets ---
    private static final float SOCKET_SIZE = 16f;
    private static final float SOCKET_GAP  = 4f;

    // --- Palette (cohérente avec ShopOverlay) ---
    private static final Color BG          = new Color(0.06f, 0.06f, 0.09f, 0.94f);
    private static final Color BORDER      = new Color(0.35f, 0.75f, 0.85f, 0.9f);   // cyan doux
    private static final Color ROW_BG      = new Color(0.14f, 0.14f, 0.19f, 1f);
    private static final Color ROW_SELECTED= new Color(0.16f, 0.22f, 0.27f, 1f);
    private static final Color ROW_HOVER   = new Color(0.20f, 0.19f, 0.26f, 1f);
    private static final Color DETAIL_BG   = new Color(0.09f, 0.09f, 0.12f, 1f);
    private static final Color RULE_COLOR  = new Color(0.35f, 0.75f, 0.85f, 0.5f);

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
        if (!visible) selectedSlotIndex = -1;
    }

    public boolean isVisible() { return visible; }

    /** Annule le mod tenu en main (appelé par GameScreen sur [Échap] quand un mod est tenu). */
    public void cancelHeld() {
        if (interaction != null) interaction.clear();
    }

    public boolean isHoldingMod() {
        return interaction != null && interaction.isHolding();
    }

    public void update(InventoryComponent inventory, LootBarComponent bar, StatSheet stats,
                       SocketInteractionState interaction) {
        this.currentInventory = inventory;
        this.bar = bar;
        this.stats = stats;
        this.interaction = interaction;
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    public void handleInput() {
        if (!visible || currentInventory == null) return;

        boolean leftClick  = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        boolean rightClick = Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT);
        boolean rKey       = Gdx.input.isKeyJustPressed(Input.Keys.R);
        if (!leftClick && !rightClick && !rKey) return;

        int screenH = Gdx.graphics.getHeight();
        float boxY = (screenH - BOX_H) / 2f;
        float slotX = MARGIN + PADDING;
        float firstSlotTop = boxY + BOX_H - PADDING - HEADER_H - PADDING;

        float mx = Gdx.input.getX();
        float my = screenH - Gdx.input.getY();

        // 1) Clic/R/clic-droit sur un socket du slot sélectionné (Arme=0 / Capacité=1 uniquement)
        if (selectedSlotIndex == 0 || selectedSlotIndex == 1) {
            Item hostItem = selectedSlotIndex == 0 ? currentInventory.weapon : currentInventory.capacity;
            if (hostItem != null) {
                float slotY = firstSlotTop - selectedSlotIndex * (SLOT_H + SLOT_GAP) - SLOT_H;
                int count = socketCountOf(hostItem);
                for (int s = 0; s < count; s++) {
                    float sx = slotX + 6 + s * (SOCKET_SIZE + SOCKET_GAP);
                    float sy = slotY + 4;
                    if (mx >= sx && mx <= sx + SOCKET_SIZE && my >= sy && my <= sy + SOCKET_SIZE) {
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

        // 2) Clic gauche sur une ligne de slot -> sélection (Arme/Capacité) ou désélection
        if (leftClick) {
            for (int i = 0; i < SLOT_COUNT; i++) {
                float slotY = firstSlotTop - i * (SLOT_H + SLOT_GAP) - SLOT_H;
                if (mx >= slotX && mx <= slotX + SLOT_W && my >= slotY && my <= slotY + SLOT_H) {
                    selectedSlotIndex = (i == 0 || i == 1) ? (selectedSlotIndex == i ? -1 : i) : -1;
                    return;
                }
            }
        }
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

        int screenH = Gdx.graphics.getHeight();

        float boxX = MARGIN;
        float boxY = (screenH - BOX_H) / 2f;
        float slotX = boxX + PADDING;
        float detailX = slotX + SLOT_W + PADDING;

        float mx = Gdx.input.getX();
        float my = screenH - Gdx.input.getY();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Fond du panneau
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(BG);
        shapes.rect(boxX, boxY, BOX_W, BOX_H);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(BORDER);
        shapes.rect(boxX, boxY, BOX_W, BOX_H);
        shapes.end();

        // Y du haut du premier slot (juste sous le titre)
        float firstSlotTop = boxY + BOX_H - PADDING - HEADER_H - PADDING;
        float headerRuleY  = firstSlotTop + PADDING / 2f;
        float footerRuleY  = boxY + PADDING + FOOTER_H + PADDING / 2f;

        // Fonds des slots (sélection / survol / normal) + panneau de détails
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < SLOT_COUNT; i++) {
            float slotY = firstSlotTop - i * (SLOT_H + SLOT_GAP) - SLOT_H;
            boolean hovered = (i == 0 || i == 1) && mx >= slotX && mx <= slotX + SLOT_W
                && my >= slotY && my <= slotY + SLOT_H;
            shapes.setColor(i == selectedSlotIndex ? ROW_SELECTED : (hovered ? ROW_HOVER : ROW_BG));
            shapes.rect(slotX, slotY, SLOT_W, SLOT_H);
            shapes.setColor(DETAIL_BG);
            shapes.rect(detailX, slotY, DETAIL_W, SLOT_H);
        }
        shapes.end();

        // Séparateurs (titre / footer) + cadres des 4 slots (rareté, cyan si sélectionné)
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(RULE_COLOR);
        shapes.line(slotX, headerRuleY, boxX + BOX_W - PADDING, headerRuleY);
        shapes.line(slotX, footerRuleY, boxX + BOX_W - PADDING, footerRuleY);

        Item[]   itemsForBorder  = {
            currentInventory.weapon, currentInventory.capacity,
            currentInventory.armor, currentInventory.artifact
        };
        for (int i = 0; i < SLOT_COUNT; i++) {
            float slotY = firstSlotTop - i * (SLOT_H + SLOT_GAP) - SLOT_H;
            Item item = itemsForBorder[i];
            shapes.setColor(i == selectedSlotIndex ? Color.CYAN
                : (item != null ? ItemDetails.rarityColor(item.rarity) : Color.DARK_GRAY));
            shapes.rect(slotX, slotY, SLOT_W, SLOT_H);
            shapes.setColor(Color.DARK_GRAY);
            shapes.rect(detailX, slotY, DETAIL_W, SLOT_H);
        }
        shapes.end();

        // Sockets du slot sélectionné (avant batch.begin() : ShapeRenderer/SpriteBatch ne s'interleavent pas)
        if (selectedSlotIndex == 0 || selectedSlotIndex == 1) {
            Item hostItem = selectedSlotIndex == 0 ? currentInventory.weapon : currentInventory.capacity;
            if (hostItem != null) {
                float slotY = firstSlotTop - selectedSlotIndex * (SLOT_H + SLOT_GAP) - SLOT_H;
                drawSocketShapes(hostItem, slotX, slotY);
            }
        }

        batch.begin();

        // Titre : positionné au-dessus des slots, ne les chevauche plus
        font.getData().setScale(1.15f);
        font.setColor(Color.YELLOW);
        font.draw(batch, "INVENTAIRE", slotX, boxY + BOX_H - PADDING);
        font.getData().setScale(1f);

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
        String footer = isHoldingMod()
            ? "[clic] implanter/échanger — [Échap] annuler"
            : "[E] fermer l'inventaire — [clic] sélectionner Arme/Capacité";
        font.draw(batch, footer, slotX, boxY + PADDING + FOOTER_H);

        batch.end();
    }

    /** Fonds + contours des sockets du slot sélectionné. Doit être appelé AVANT batch.begin(). */
    private void drawSocketShapes(Item hostItem, float slotX, float slotY) {
        int count = socketCountOf(hostItem);
        if (count == 0) return;

        ItemModifier previewMod = interaction != null ? interaction.compatibilityPreviewMod() : null;
        boolean previewCompatible = previewMod != null && previewMod.isCompatibleWith(hostItem);

        float baseX = slotX + 6;
        float baseY = slotY + 4;

        // Fonds (mods occupant un socket)
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int s = 0; s < count; s++) {
            ItemModifier occupant = socketOf(hostItem, s);
            if (occupant == null) continue;
            float sx = baseX + s * (SOCKET_SIZE + SOCKET_GAP);
            shapes.setColor(occupant instanceof WeaponComboMod
                ? new Color(0.75f, 0.2f, 0.2f, 0.85f)   // rouge : combo offensif
                : new Color(0.2f, 0.4f, 0.85f, 0.85f)); // bleu : effet magique
            shapes.rect(sx, baseY, SOCKET_SIZE, SOCKET_SIZE);
        }
        shapes.end();

        // Contours (jaune = tenu, vert = cible compatible en survol/tenue, sinon rareté/gris)
        for (int s = 0; s < count; s++) {
            float sx = baseX + s * (SOCKET_SIZE + SOCKET_GAP);
            ItemModifier occupant = socketOf(hostItem, s);
            boolean isHeldSource = interaction != null && interaction.heldFromItem == hostItem
                && interaction.heldFromSocketIndex == s;

            Color border;
            if (isHeldSource) border = Color.YELLOW;
            else if (previewCompatible) border = Color.GREEN;
            else if (occupant != null) border = ItemDetails.rarityColor(occupant.rarity);
            else border = Color.GRAY;

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
     * combos + sockets (Arme) ou effet + modificateurs + sockets (Capacité).
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
