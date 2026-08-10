package adfinir.game.inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Modificateur d'arme : ajoute une séquence d'attaques (combo) au combo de
 * base de l'arme hôte quand il est implanté dans un de ses sockets, plus
 * d'éventuels bonus de stats (hérités via Item.statBonuses).
 * Compatible uniquement avec Weapon (voir getCompatibleHostType()).
 */
public class WeaponComboMod extends ItemModifier {

    public final List<WeaponAttack> comboAttacks = new ArrayList<>();

    public WeaponComboMod(String modifierId, String name, Rarity rarity, List<WeaponAttack> comboAttacks) {
        super(modifierId, name, rarity);
        if (comboAttacks != null) this.comboAttacks.addAll(comboAttacks);
    }

    @Override
    public Class<? extends Item> getCompatibleHostType() {
        return Weapon.class;
    }

    @Override
    public String getDescription() {
        return String.format("%s (%s) - Combo: +%d attaque(s)",
            name, rarity.name(), comboAttacks.size());
    }
}
