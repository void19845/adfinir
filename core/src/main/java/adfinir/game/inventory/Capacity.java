package adfinir.game.inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Capacité active modulaire (inspirée de Noita).
 * Sockets : taille = rarity.bonusPropertyCount (COMMON=0, RARE=1, EPIC=2,
 * LEGENDARY=3, MYTHICAL=4), accueillent des CapacityEffectMod.
 */
public class Capacity extends Item {
    public CapacityEffect mainEffect;
    public final List<CapacityModifier> modifiers = new ArrayList<>();

    /** Sockets de modificateurs (CapacityEffectMod uniquement). */
    private final ItemModifier[] sockets;

    public Capacity(String name, Rarity rarity, CapacityEffect mainEffect) {
        super(name, rarity);
        this.mainEffect = mainEffect;
        this.sockets = new ItemModifier[rarity.bonusPropertyCount];
    }

    public void addModifier(CapacityModifier modifier) {
        // On limite le nombre de modificateurs selon la rareté
        if (modifiers.size() < rarity.bonusPropertyCount * 2) { // Exemple: Legendary = 6 mods
            modifiers.add(modifier);
        }
    }

    // ------------------------------------------------------------------
    // Sockets
    // ------------------------------------------------------------------

    public int getSocketCount() {
        return sockets.length;
    }

    public ItemModifier getSocket(int index) {
        if (index < 0 || index >= sockets.length) return null;
        return sockets[index];
    }

    public ItemModifier setSocket(int index, ItemModifier mod) {
        if (index < 0 || index >= sockets.length) return null;
        ItemModifier old = sockets[index];
        sockets[index] = mod;
        return old;
    }

    /** Effet actif = dernier CapacityEffectMod socketé avec effectOverride non nul, sinon mainEffect. */
    public CapacityEffect getActiveEffect() {
        CapacityEffect active = mainEffect;
        for (ItemModifier mod : sockets) {
            if (mod instanceof CapacityEffectMod) {
                CapacityEffect override = ((CapacityEffectMod) mod).effectOverride;
                if (override != null) active = override;
            }
        }
        return active;
    }

    /** Modifiers actifs = modifiers de base + extraModifier de chaque CapacityEffectMod socketé. */
    public List<CapacityModifier> getActiveModifiers() {
        List<CapacityModifier> active = new ArrayList<>(modifiers);
        for (ItemModifier mod : sockets) {
            if (mod instanceof CapacityEffectMod) {
                CapacityModifier extra = ((CapacityEffectMod) mod).extraModifier;
                if (extra != null) active.add(extra);
            }
        }
        return active;
    }

    @Override
    public String getDescription() {
        return String.format("%s (%s) - Base: %s. Modificateurs: %s",
            name, rarity.name(), mainEffect.name, modifiers.stream()
                .map(m -> m.type.name()).reduce("", (a, b) -> a + " " + b));
    }
}
