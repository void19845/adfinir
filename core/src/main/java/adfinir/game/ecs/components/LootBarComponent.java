package adfinir.game.ecs.components;

import adfinir.game.inventory.Item;
import com.badlogic.ashley.core.Component;

/**
 * Barre d'objets ramassés en attente d'équipement (8 cercles), distincte des
 * 4 slots d'équipement actif gérés par InventoryComponent.
 *
 * Le ramassage (LootPickupSystem) remplit cette barre. L'équipement effectif
 * se fait ensuite via InventoryComponent.equipFromBar(...) (touche 1-8 ou clic),
 * qui échange l'item sélectionné avec l'équipement actuel du même type.
 */
public class LootBarComponent implements Component {
    public static final int CAPACITY = 8;

    public final Item[] slots = new Item[CAPACITY];

    /** Cercle actuellement en surbrillance (par défaut le premier). Cible du swap [F]. */
    public int selectedIndex = 0;

    public boolean isFull() {
        for (Item i : slots) if (i == null) return false;
        return true;
    }

    public int firstEmptySlot() {
        for (int i = 0; i < CAPACITY; i++) if (slots[i] == null) return i;
        return -1;
    }
}
