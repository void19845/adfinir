package adfinir.game.inventory;

/**
 * Modificateur implantable dans un socket d'arme ou de capacité.
 * Hérite d'Item pour pouvoir transiter par LootBarComponent (barre de 8 slots)
 * exactement comme n'importe quel autre loot.
 *
 * modifierId est l'identifiant stable utilisé par ItemGenerator.createModifierById()
 * pour la reconstruction à la sauvegarde (même principe que Artifact.passiveEffectId
 * / ItemGenerator.createArtifactById : source de vérité unique id -> définition).
 */
public abstract class ItemModifier extends Item {

    public final String modifierId;

    protected ItemModifier(String modifierId, String name, Rarity rarity) {
        super(name, rarity);
        this.modifierId = modifierId;
    }

    /** Type d'item hôte compatible (Weapon.class ou Capacity.class). Règle de compatibilité des sockets. */
    public abstract Class<? extends Item> getCompatibleHostType();

    public boolean isCompatibleWith(Item host) {
        return host != null && getCompatibleHostType().isInstance(host);
    }
}
