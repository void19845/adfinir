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

    /**
     * Équipe l'item du slot {@code index} de la barre de loot. L'ancien équipement
     * du même type repart dans ce même slot de la barre (échange).
     * Ne fait rien si le slot est vide.
     *
     * @return true si l'arme a changé (le CombatComponent doit être resynchronisé).
     */
    public boolean equipFromBar(LootBarComponent bar, int index) {
        if (bar == null || index < 0 || index >= LootBarComponent.CAPACITY) return false;
        Item incoming = bar.slots[index];
        if (incoming == null) return false;

        boolean weaponChanged = false;
        if (incoming instanceof Weapon) {
            bar.slots[index] = this.weapon;
            this.weapon = (Weapon) incoming;
            weaponChanged = true;
        } else if (incoming instanceof Capacity) {
            bar.slots[index] = this.capacity;
            this.capacity = (Capacity) incoming;
        } else if (incoming instanceof Armor) {
            bar.slots[index] = this.armor;
            this.armor = (Armor) incoming;
        } else if (incoming instanceof Artifact) {
            bar.slots[index] = this.artifact;
            this.artifact = (Artifact) incoming;
        } else {
            return false;
        }

        bar.selectedIndex = index;
        return weaponChanged;
    }

    /**
     * Comme {@link #equipFromBar}, mais resynchronise aussi CombatComponent.weapon
     * (si l'arme a changé) et recalcule la StatSheet. Point d'entrée unique utilisé
     * par la sélection clavier (1-8) et le clic souris sur la barre.
     */
    public void equipFromBarAndSync(LootBarComponent bar, int index, CombatComponent combat, StatSheet stats) {
        boolean weaponChanged = equipFromBar(bar, index);
        if (weaponChanged && combat != null) {
            combat.weapon = this.weapon;
        }
        if (stats != null) {
            updateStats(stats);
        }
    }
}
