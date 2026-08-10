package adfinir.game.inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Arme modulaire avec un système de combos.
 * Sockets : taille = rarity.bonusPropertyCount (COMMON=1, RARE=1, EPIC=2,
 * LEGENDARY=3, MYTHICAL=4), accueillent des WeaponComboMod.
 *
 * Le comportement d'une arme est déterminé uniquement par son type (stats :
 * range/damage/cooldown mods) et ses sockets : le combo entier vient des
 * WeaponComboMod implantés, il n'y a plus d'attaque "de base" figée sur
 * l'arme elle-même. ItemGenerator garantit qu'au moins un socket est rempli
 * à la création pour qu'une arme ait toujours au moins une attaque.
 */
public class Weapon extends Item {
    public final WeaponType type;

    /** Sockets de modificateurs (WeaponComboMod uniquement). */
    private final ItemModifier[] sockets;

    public Weapon(String name, Rarity rarity, WeaponType type) {
        super(name, rarity);
        this.type = type;
        this.sockets = new ItemModifier[rarity.bonusPropertyCount];
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

    /** Place mod dans le socket index et retourne l'ancien occupant (peut être null). */
    public ItemModifier setSocket(int index, ItemModifier mod) {
        if (index < 0 || index >= sockets.length) return null;
        ItemModifier old = sockets[index];
        sockets[index] = mod;
        return old;
    }

    /**
     * Combo effectif = concaténation des attaques de chaque WeaponComboMod
     * implanté, dans l'ordre des sockets. Recalculé à la volée (pas de
     * cache) : CombatComponent/CombatSystem l'appellent directement, donc
     * tout changement de socket est répercuté en temps réel en combat.
     */
    public List<WeaponAttack> getActiveCombo() {
        List<WeaponAttack> active = new ArrayList<>();
        for (ItemModifier mod : sockets) {
            if (mod instanceof WeaponComboMod) {
                active.addAll(((WeaponComboMod) mod).comboAttacks);
            }
        }
        return active;
    }

    /**
     * Calcule les dégâts d'une attaque en tenant compte de la rareté et du type d'arme.
     */
    public float getModifiedDamage(int attackIndex) {
        List<WeaponAttack> active = getActiveCombo();
        if (attackIndex < 0 || attackIndex >= active.size()) return 0;
        WeaponAttack attack = active.get(attackIndex);
        return attack.getRandomDamage() * rarity.statMultiplier * type.damageMod;
    }

    public float getModifiedCooldown(int attackIndex) {
        List<WeaponAttack> active = getActiveCombo();
        if (attackIndex < 0 || attackIndex >= active.size()) return 0;
        return active.get(attackIndex).cooldown * type.cooldownMod;
    }

    @Override
    public String getDescription() {
        return String.format("%s (%s) - %s. Combos: %d",
            name, rarity.name(), type.description, getActiveCombo().size());
    }
}
