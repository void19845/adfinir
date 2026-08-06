package adfinir.game.inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Capacité active modulaire (inspirée de Noita).
 */
public class Capacity extends Item {
    public CapacityEffect mainEffect;
    public final List<CapacityModifier> modifiers = new ArrayList<>();

    public Capacity(String name, Rarity rarity, CapacityEffect mainEffect) {
        super(name, rarity);
        this.mainEffect = mainEffect;
    }

    public void addModifier(CapacityModifier modifier) {
        // On limite le nombre de modificateurs selon la rareté
        if (modifiers.size() < rarity.bonusPropertyCount * 2) { // Exemple: Legendary = 6 mods
            modifiers.add(modifier);
        }
    }

    @Override
    public String getDescription() {
        return String.format("%s (%s) - Base: %s. Modificateurs: %s",
            name, rarity.name(), mainEffect.name, modifiers.stream()
                .map(m -> m.type.name()).reduce("", (a, b) -> a + " " + b));
    }
}
