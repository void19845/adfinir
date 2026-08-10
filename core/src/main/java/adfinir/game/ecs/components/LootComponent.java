package adfinir.game.ecs.components;

import adfinir.game.inventory.Item;
import adfinir.game.inventory.Rarity;
import com.badlogic.ashley.core.Component;

/**
 * Marque une entité comme un objet au sol ramassable, laissé par un ennemi vaincu.
 */
public class LootComponent implements Component {
    public Item item;
    public Rarity rarity;
}
