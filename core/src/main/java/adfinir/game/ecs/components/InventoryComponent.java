package adfinir.game.ecs.components;

import adfinir.game.inventory.*;
import adfinir.game.player.StatSheet;
import com.badlogic.ashley.core.Component;

/**
 * Composant gérant l'équipement du joueur.
 * Gère 4 slots : Weapon, Capacity, Armor, Artifact.
 */
public class InventoryComponent implements Component {
    public Weapon weapon;
    public Capacity capacity;
    public Armor armor;
    public Artifact artifact;

    /**
     * Recalcule les bonus de stats sur la StatSheet du joueur en fonction de l'équipement actuel.
     */
    public void updateStats(StatSheet stats) {
        stats.clearBonus();

        // Armor bonus
        if (armor != null) {
            stats.applyBonusMap(armor.getStatBonuses());
        }

        // Artifact bonus
        if (artifact != null) {
            stats.applyBonusMap(artifact.getStatBonuses());
        }

        // Note: Weapons and Capacities primarily affect combat logic,
        // but could also provide stat bonuses here.
    }

    public void equipWeapon(Weapon newWeapon) {
        this.weapon = newWeapon;
    }

    public void equipCapacity(Capacity newCapacity) {
        this.capacity = newCapacity;
    }

    public void equipArmor(Armor newArmor) {
        this.armor = newArmor;
    }

    public void equipArtifact(Artifact newArtifact) {
        this.artifact = newArtifact;
    }
}
