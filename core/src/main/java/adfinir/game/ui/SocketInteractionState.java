package adfinir.game.ui;

import adfinir.game.inventory.Item;
import adfinir.game.inventory.ItemModifier;

/**
 * État partagé "mod tenu en main" entre InventoryOverlay et LootBarOverlay,
 * nécessaire car un transfert de socket peut avoir sa source ou sa cible dans
 * l'un ou l'autre des deux overlays (ex: clic sur un mod de la loot bar, puis
 * clic sur un socket de l'inventaire pour l'implanter).
 *
 * heldMod != null  => un mod est actuellement sélectionné ("tenu en main"),
 * surligné en jaune côté UI ; sa provenance est soit un socket d'item équipé
 * (heldFromItem/heldFromSocketIndex), soit un slot de la loot bar
 * (heldFromLootBarIndex).
 *
 * hoverPreviewMod est repositionné à null puis réévalué CHAQUE frame par
 * LootBarOverlay (survol souris, sans clic) : InventoryOverlay l'utilise pour
 * surligner en vert les sockets compatibles pendant le survol, en plus du cas
 * où un mod est réellement tenu en main.
 */
public class SocketInteractionState {

    public ItemModifier heldMod;
    public Item heldFromItem;
    public int heldFromSocketIndex = -1;
    public int heldFromLootBarIndex = -1;

    /** Mod survolé (pas cliqué) dans la loot bar cette frame — réinitialisé à chaque frame. */
    public ItemModifier hoverPreviewMod;

    public boolean isHolding() {
        return heldMod != null;
    }

    /** Mod à utiliser pour le surlignage vert des sockets compatibles : tenu en priorité, sinon survolé. */
    public ItemModifier compatibilityPreviewMod() {
        return heldMod != null ? heldMod : hoverPreviewMod;
    }

    public void holdFromSocket(ItemModifier mod, Item source, int socketIndex) {
        heldMod = mod;
        heldFromItem = source;
        heldFromSocketIndex = socketIndex;
        heldFromLootBarIndex = -1;
    }

    public void holdFromLootBar(ItemModifier mod, int lootBarIndex) {
        heldMod = mod;
        heldFromLootBarIndex = lootBarIndex;
        heldFromItem = null;
        heldFromSocketIndex = -1;
    }

    public void clear() {
        heldMod = null;
        heldFromItem = null;
        heldFromSocketIndex = -1;
        heldFromLootBarIndex = -1;
    }
}
