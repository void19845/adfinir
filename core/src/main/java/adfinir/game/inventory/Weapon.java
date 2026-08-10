package adfinir.game.inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Arme modulaire avec un système de combos.
 */
public class Weapon extends Item {
    public final WeaponType type;
    public final List<WeaponAttack> comboSlots = new ArrayList<>();

    public Weapon(String name, Rarity rarity, WeaponType type) {
        super(name, rarity);
        this.type = type;
    }

    public void addAttack(WeaponAttack attack) {
        comboSlots.add(attack);
    }

    /**
     * Calcule les dégâts d'une attaque en tenant compte de la rareté, du type d'arme
     * et de la stat du porteur (ATK pour une attaque physique, MAG si l'attaque a un élément).
     */
    public float getModifiedDamage(int attackIndex, float atk, float mag) {
        if (attackIndex < 0 || attackIndex >= comboSlots.size()) return 0;
        WeaponAttack attack = comboSlots.get(attackIndex);
        float statContribution = (attack.element != null) ? mag : atk;
        return (attack.getRandomDamage() + statContribution) * rarity.statMultiplier * type.damageMod;
    }

    public float getModifiedCooldown(int attackIndex) {
        if (attackIndex < 0 || attackIndex >= comboSlots.size()) return 0;
        return comboSlots.get(attackIndex).cooldown * type.cooldownMod;
    }

    @Override
    public String getDescription() {
        return String.format("%s (%s) - %s. Combos: %d",
            name, rarity.name(), type.description, comboSlots.size());
    }
}
