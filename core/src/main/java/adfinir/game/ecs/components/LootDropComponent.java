package adfinir.game.ecs.components;

import com.badlogic.ashley.core.Component;

/**
 * Données de loot d'un mob.
 * Le drop effectif est tiré depuis ItemDatabase (items.json).
 */
public class LootDropComponent implements Component {

    public int   xpReward   = 30;
    public float dropChance = 0.40f; // 40% de chance de dropper quelque chose

    public LootDropComponent(int xpReward, float dropChance) {
        this.xpReward   = xpReward;
        this.dropChance = dropChance;
    }

    /** Constructeur de compatibilité (ignore la table fixe, utilise ItemDatabase). */
    public LootDropComponent(int xpReward, Object items, Object chances) {
        this.xpReward   = xpReward;
        this.dropChance = 0.40f;
    }
}
