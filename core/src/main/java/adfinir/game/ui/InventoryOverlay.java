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
 * Layout dérivé de constantes (BOX_W / BOX_H) pour éviter tout chevauchement.
 * Chaque ligne d'équipement = case-icône en creux (sprite d'arme ou icône
 * vectorielle ItemIcon) + nom/description à droite, pour reconnaître un
 * objet au premier coup d'œil sans lire le texte. Un panneau de détails est
 * affiché à droite de chaque ligne :
 *  - Armure / Artéfact  : bonus de stats (+X PV Max, +X DEF, ...)
 *  - Arme               : liste des combos (dégâts, cooldown)
 *  - Capacité           : effet principal + modificateurs
 *
 * Sockets (arme / capacité) : cliquer sur la ligne Arme ou Capacité la
 * sélectionne (cadre accent) et affiche ses sockets en petites cases en
 * creux dans la bande basse de la ligne. Flux d'interaction (voir
 * SocketInteractionState) :
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
    private static final float MARGIN     = 24f;
    private static final float PADDING    = 16f;  // marge intérieure + espace vertical entre blocs
    private static final float SLOT_W     = 220f;
    private static final float SLOT_H     = 76f;
    private static final float SLOT_GAP   = 10f;  // espace entre deux slots consécutifs
    private static final float HEADER_H   = 30f;  // hauteur réservée au titre
    private static final float FOOTER_H   = 20f;  // hauteur réservée au texte de fermeture
    private static final float ICON_BOX   = 56f;  // case-icône (en creux) à gauche de chaque ligne
    private static final int   SLOT_COUNT = 4;

    private static final float DETAIL_W        = 300f; // largeur du panneau de détails
    private static final float DETAIL_LINE_H   = 14f;
    private static final float DETAIL_SCALE    = 1.0f;
    private static final int   DETAIL_MAX_LINES = 6;   // sécurité anti-débordement du slot

    // --- Sockets ---
    private static final float SOCKET_SIZE = 20f;
    private static final float SOCKET_GAP  = 5f;

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

        float firstSlotTop = boxY + BOX_H - PADDING - HEADER_H - PADDING;
        float headerBarY   = firstSlotTop + PADDING;
        float footerBarY   = boxY + PADDING;

        Item[] items = {
            currentInventory.weapon,
            currentInventory.capacity,
            currentInventory.armor,
            currentInventory.artifact
        };
        String[] labels = { "ARME", "CAPACITÉ", "ARMURE", "ARTÉFACT" };

        // --- Fonds (panneau, bandeau titre, bandeau pied, lignes de slot, panneau détail) ---
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        UiTheme.panel(shapes, boxX, boxY, BOX_W, BOX_H);

        shapes.setColor(UiTheme.HEADER_BG);
        shapes.rect(boxX + UiTheme.BEVEL, headerBarY, BOX_W - UiTheme.BEVEL * 2, HEADER_H + PADDING - UiTheme.BEVEL);
        shapes.rect(boxX + UiTheme.BEVEL, boxY + UiTheme.BEVEL, BOX_W - UiTheme.BEVEL * 2, footerBarY - boxY - UiTheme.BEVEL + FOOTER_H);

        for (int i = 0; i < SLOT_COUNT; i++) {
            float slotY = firstSlotTop - i * (SLOT_H + SLOT_GAP) - SLOT_H;
            boolean selectable = (i == 0 || i == 1);
            boolean hovered = selectable && mx >= slotX && mx <= slotX + SLOT_W && my >= slotY && my <= slotY + SLOT_H;
            Color rowBg = i == selectedSlotIndex ? UiTheme.SLOT_BG_SELECT
                : (hovered ? UiTheme.SLOT_BG_HOVER : UiTheme.SLOT_BG);
            UiTheme.slotSunken(shapes, slotX, slotY, SLOT_W, SLOT_H, rowBg);
            UiTheme.slotSunken(shapes, detailX, slotY, DETAIL_W, SLOT_H, UiTheme.SLOT_BG_EMPTY);

            // Case-icône en creux à gauche de la ligne
            float iconX = slotX + 8f;
            float iconY = slotY + (SLOT_H - ICON_BOX) / 2f;
            UiTheme.slotSunken(shapes, iconX, iconY, ICON_BOX, ICON_BOX, UiTheme.SLOT_BG_EMPTY);
            Item item = items[i];
            if (item != null && !(item instanceof Weapon)) {
                ItemIcon.draw(shapes, item, iconX, iconY, ICON_BOX, ICON_BOX);
            }
        }
        shapes.end();

        // --- Cadres (rareté / sélection) ---
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(UiTheme.PANEL_ACCENT);
        shapes.rect(boxX + UiTheme.BEVEL, headerBarY, BOX_W - UiTheme.BEVEL * 2, HEADER_H + PADDING - UiTheme.BEVEL);

        for (int i = 0; i < SLOT_COUNT; i++) {
            float slotY = firstSlotTop - i * (SLOT_H + SLOT_GAP) - SLOT_H;
            Item item = items[i];
            shapes.setColor(i == selectedSlotIndex ? UiTheme.PANEL_ACCENT
                : (item != null ? ItemDetails.rarityColor(item.rarity) : UiTheme.TEXT_DIM));
            shapes.rect(slotX, slotY, SLOT_W, SLOT_H);

            float iconX = slotX + 8f;
            float iconY = slotY + (SLOT_H - ICON_BOX) / 2f;
            shapes.rect(iconX, iconY, ICON_BOX, ICON_BOX);
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

        // Titre
        font.getData().setScale(1.2f);
        font.setColor(UiTheme.TEXT_TITLE);
        font.draw(batch, "INVENTAIRE", slotX, boxY + BOX_H - PADDING - 4f);
        font.getData().setScale(1f);

        for (int i = 0; i < SLOT_COUNT; i++) {
            float slotY = firstSlotTop - i * (SLOT_H + SLOT_GAP) - SLOT_H;
            drawSlotContent(batch, slotX, slotY, labels[i], items[i], i == 0 || i == 1, i == selectedSlotIndex);
            drawDetailPanel(batch, detailX, slotY, items[i]);
        }

        // Pied de page
        font.setColor(UiTheme.TEXT_DIM);
        String footer = isHoldingMod()
            ? "[clic] implanter/échanger   —   [Échap] annuler"
            : "[E] fermer   —   [clic] sélectionner Arme/Capacité pour voir ses sockets";
        font.draw(batch, footer, slotX, boxY + PADDING + FOOTER_H - 4f);

        batch.end();
    }

    /** Fonds + contours des sockets du slot sélectionné. Doit être appelé AVANT batch.begin(). */
    private void drawSocketShapes(Item hostItem, float slotX, float slotY) {
        int count = socketCountOf(hostItem);
        if (count == 0) return;

        ItemModifier previewMod = interaction != null ? interaction.compatibilityPreviewMod() : null;
        boolean previewCompatible = previewMod != null && previewMod.isCompatibleWith(hostItem);

        float baseX = slotX + 6;
        float baseY = slotY + 5;

        // Fonds en creux (mods occupant un socket, teinte selon le type)
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

    private void drawSlotContent(SpriteBatch batch, float x, float y, String label, Item item,
                                 boolean selectable, boolean selected) {
        float iconZoneW = ICON_BOX + 16f;
        float textX = x + iconZoneW;
        float textZoneW = SLOT_W - iconZoneW - 8f;

        // Sprite d'arme (seul type avec un vrai sprite ; les autres ont déjà leur ItemIcon dessiné en dessous)
        if (item instanceof Weapon) {
            float boxX = x + 8f;
            float boxY = y + (SLOT_H - ICON_BOX) / 2f;
            TextureRegion region = WeaponSpriteManager.getRegion(((Weapon) item).type);
            float w = region.getRegionWidth();
            float h = region.getRegionHeight();
            float scale = Math.min((ICON_BOX - 8f) / w, (ICON_BOX - 8f) / h);
            float finalW = w * scale;
            float finalH = h * scale;
            batch.draw(region, boxX + (ICON_BOX - finalW) / 2f, boxY + (ICON_BOX - finalH) / 2f, finalW, finalH);
        }

        font.setColor(UiTheme.TEXT_DIM);
        font.draw(batch, label, textX, y + SLOT_H - 8);

        if (item != null) {
            // Étiquette de rareté, alignée à droite de la ligne
            font.setColor(ItemDetails.rarityColor(item.rarity));
            font.draw(batch, rarityLabel(item.rarity), textX, y + SLOT_H - 8, textZoneW, Align.right, false);

            // Nom, contraint et tronqué dans la zone de texte
            font.getData().setScale(1.05f);
            font.draw(batch, item.name, textX, y + SLOT_H - 26, textZoneW, Align.left, true);
            font.getData().setScale(1f);

            // Description courte, une seule ligne
            font.setColor(UiTheme.TEXT_BODY);
            String desc = item.getDescription();
            int maxChars = 30;
            if (desc.length() > maxChars) desc = desc.substring(0, maxChars - 3) + "...";
            font.draw(batch, desc, textX, y + SLOT_H - 44, textZoneW, Align.left, true);

            if (selectable) {
                font.setColor(selected ? UiTheme.PANEL_ACCENT : UiTheme.TEXT_DIM);
                font.draw(batch, selected ? "▾ sockets" : "▸ sockets", textX, y + 12);
            }
        } else {
            font.setColor(UiTheme.TEXT_DIM);
            font.draw(batch, "-- vide --", textX, y + SLOT_H - 28);
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
     * Panneau de détails à droite de la ligne : stats (Armure/Artéfact),
     * combos + sockets (Arme) ou effet + modificateurs + sockets (Capacité).
     * Les lignes de section ("Combos :", "Sockets :"...) ressortent en accent.
     */
    private void drawDetailPanel(SpriteBatch batch, float x, float slotY, Item item) {
        float textX = x + 10;
        float textW = DETAIL_W - 20;
        float y = slotY + SLOT_H - 12;

        if (item == null) {
            font.setColor(UiTheme.TEXT_DIM);
            font.draw(batch, "Rien d'équipé ici.", textX, y);
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
