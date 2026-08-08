package adfinir.game.inventory;

/**
 * Modificateur de capacité : porte un CapacityEffect alternatif (nullable,
 * remplace l'effet actif si présent) et/ou un CapacityModifier additionnel
 * (nullable, ajouté aux modifiers actifs), implantable dans un socket de
 * Capacity. Compatible uniquement avec Capacity (voir getCompatibleHostType()).
 */
public class CapacityEffectMod extends ItemModifier {

    public final CapacityEffect effectOverride; // nullable
    public final CapacityModifier extraModifier; // nullable

    public CapacityEffectMod(String modifierId, String name, Rarity rarity,
                             CapacityEffect effectOverride, CapacityModifier extraModifier) {
        super(modifierId, name, rarity);
        this.effectOverride = effectOverride;
        this.extraModifier = extraModifier;
    }

    @Override
    public Class<? extends Item> getCompatibleHostType() {
        return Capacity.class;
    }

    @Override
    public String getDescription() {
        String effectPart = effectOverride != null ? effectOverride.name : "aucun";
        String modPart = extraModifier != null ? extraModifier.type.name() : "aucun";
        return String.format("%s (%s) - Effet: %s, Modificateur: %s",
            name, rarity.name(), effectPart, modPart);
    }
}
