package adfinir.game.inventory;

import adfinir.game.player.StatType;
import java.util.Map;
import java.util.HashMap;

/**
 * Classe de base pour tout objet d'équipement.
 */
public abstract class Item {
    public final String name;
    public final Rarity rarity;

    // Bonus de stats génériques (utilisé principalement par Armor et Artifact)
    protected final Map<StatType, Float> statBonuses = new HashMap<>();

    public Item(String name, Rarity rarity) {
        this.name = name;
        this.rarity = rarity;
    }

    public Map<StatType, Float> getStatBonuses() {
        return statBonuses;
    }

    public abstract String getDescription();
}
