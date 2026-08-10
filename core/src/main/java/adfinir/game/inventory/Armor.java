package adfinir.game.inventory;

import adfinir.game.player.StatType;

/**
 * Armure fournissant des bonus défensifs et utilitaires.
 */
public class Armor extends Item {
    public final ArmorType type;

    public Armor(String name, Rarity rarity, ArmorType type) {
        super(name, rarity);
        this.type = type;
        applyTypeStats();
    }

    private void applyTypeStats() {
        // Application des stats de base selon le type, amplifiées par la rareté
        float mult = rarity.statMultiplier;

        statBonuses.put(StatType.MAX_HP, 20f * type.hpMod * mult);
        statBonuses.put(StatType.DEF, 5f * type.defenseMod * mult);
        statBonuses.put(StatType.SPD, 10f * type.speedMod * mult);
        // On pourrait ajouter d'autres stats ici
    }

    @Override
    public String getDescription() {
        return String.format("%s (%s) - Type: %s. Bonus: %s",
            name, rarity.name(), type.name, statBonuses.toString());
    }
}
