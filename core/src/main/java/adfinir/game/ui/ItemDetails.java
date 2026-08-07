package adfinir.game.ui;

import adfinir.game.inventory.*;
import adfinir.game.player.StatType;
import com.badlogic.gdx.graphics.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Construit les lignes de détail affichées pour un item (stats, combos, capacité).
 * Logique partagée entre InventoryOverlay (panneau détaillé) et LootBarOverlay (infobulle au survol).
 */
public final class ItemDetails {

    private ItemDetails() {}

    public static List<String> buildDetailLines(Item item) {
        List<String> lines = new ArrayList<>();
        if (item == null) return lines;

        if (item instanceof Weapon) {
            Weapon w = (Weapon) item;
            lines.add("Combos :");
            int shown = Math.min(w.comboSlots.size(), 3);
            for (int i = 0; i < shown; i++) {
                WeaponAttack a = w.comboSlots.get(i);
                lines.add(String.format("%d. %s  %.0f-%.0f dgt (cd %.2fs)",
                    i + 1, a.name, a.minDamage, a.maxDamage, a.cooldown));
            }
            if (w.comboSlots.size() > shown) {
                lines.add("... +" + (w.comboSlots.size() - shown) + " autre(s)");
            }

        } else if (item instanceof Capacity) {
            Capacity c = (Capacity) item;
            if (c.mainEffect != null) {
                lines.add(String.format("%s (%s)", c.mainEffect.name, c.mainEffect.type));
                lines.add(String.format("Dgt %.0f  Vit %.0f  Rayon %.0f",
                    c.mainEffect.baseDamage, c.mainEffect.speed, c.mainEffect.radius));
            }
            int shown = Math.min(c.modifiers.size(), 2);
            for (int i = 0; i < shown; i++) {
                CapacityModifier m = c.modifiers.get(i);
                lines.add(String.format("+ %s x%.1f", m.type.name(), m.intensity));
            }
            if (c.modifiers.size() > shown) {
                lines.add("... +" + (c.modifiers.size() - shown) + " autre(s)");
            }

        } else {
            // Armure / Artéfact : bonus de stats
            Map<StatType, Float> bonuses = item.getStatBonuses();
            for (StatType t : StatType.values()) {
                Float v = bonuses.get(t);
                if (v != null && v != 0f) {
                    lines.add(String.format("+%.1f %s", v, statLabel(t)));
                }
            }
        }

        return lines;
    }

    public static String statLabel(StatType t) {
        switch (t) {
            case MAX_HP:         return "PV Max";
            case ATK:             return "ATQ";
            case MAG:             return "MAG";
            case DEF:             return "DEF";
            case SPD:             return "Vitesse";
            case MAX_STAMINA:     return "Stamina Max";
            case STAMINA_REGEN:   return "Régén Stamina";
            default:              return t.name();
        }
    }

    public static Color rarityColor(Rarity rarity) {
        switch (rarity) {
            case LEGENDARY: return Color.ORANGE;
            case EPIC:      return Color.PURPLE;
            case RARE:      return Color.CYAN;
            case MYTHICAL:  return Color.MAGENTA;
            case COMMON:
            default:        return Color.WHITE;
        }
    }
}
