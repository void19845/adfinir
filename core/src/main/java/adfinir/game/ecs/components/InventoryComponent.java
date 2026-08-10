package adfinir.game.ecs.components;

import adfinir.game.inventory.*;
import adfinir.game.player.StatSheet;
import com.badlogic.ashley.core.Component;

/**
 * Composant gérant l'équipement du joueur.
 * Gère : Weapon, Armor, Artifact (1 slot chacun) et 4 slots de sorts (Capacity).
 */
public class InventoryComponent implements Component {
    public static final int SPELL_SLOTS = 4;

    public Weapon weapon;
    public Capacity[] spells = new Capacity[SPELL_SLOTS];
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

    public void equipSpell(int slot, Capacity newCapacity) {
        if (slot < 0 || slot >= SPELL_SLOTS) return;
        spells[slot] = newCapacity;
    }

    /** Équipe un sort dans le premier slot libre, ou remplace le slot 0 si tout est occupé. */
    public void equipSpellAuto(Capacity newCapacity) {
        for (int i = 0; i < SPELL_SLOTS; i++) {
            if (spells[i] == null) {
                spells[i] = newCapacity;
                return;
            }
        }
        spells[0] = newCapacity;
    }

    public void equipArmor(Armor newArmor) {
        this.armor = newArmor;
    }

    public void equipArtifact(Artifact newArtifact) {
        this.artifact = newArtifact;
    }
}
