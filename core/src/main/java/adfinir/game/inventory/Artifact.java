package adfinir.game.inventory;

import java.util.function.Consumer;

/**
 * Artefact conférant un bonus passif unique.
 */
public class Artifact extends Item {
    public final String passiveEffectId;
    public final Consumer<Object> effectLogic; // Simplifié: logique d'effet

    public Artifact(String name, Rarity rarity, String passiveEffectId, Consumer<Object> effectLogic) {
        super(name, rarity);
        this.passiveEffectId = passiveEffectId;
        this.effectLogic = effectLogic;
    }

    @Override
    public String getDescription() {
        return String.format("%s (%s) - Effet unique: %s",
            name, rarity.name(), passiveEffectId);
    }
}
