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

    /** Bordure des mods (ItemModifier) dans la loot bar — spec §2.3. */
    public static final Color MOD_BORDER_COLOR = new Color(1f, 0.84f, 0f, 1f); // doré
    /** Bordure des items standard (Weapon/Capacity/Armor/Artifact) dans la loot bar. */
    public static final Color STANDARD_BORDER_COLOR = new Color(0.75f, 0.75f, 0.78f, 1f); // argenté

    public static List<String> buildDetailLines(Item item) {
        List<String> lines = new ArrayList<>();
        if (item == null) return lines;

        if (item instanceof ItemModifier) {
            lines.addAll(buildModifierLines((ItemModifier) item));
            return lines;
        }

        if (item instanceof Weapon) {
            Weapon w = (Weapon) item;
            List<WeaponAttack> combo = w.getActiveCombo();
            lines.add("Combos :");
            int shown = Math.min(combo.size(), 3);
            for (int i = 0; i < shown; i++) {
                WeaponAttack a = combo.get(i);
                lines.add(String.format("%d. %s  %.0f-%.0f dgt (cd %.2fs)",
                    i + 1, a.name, a.minDamage, a.maxDamage, a.cooldown));
            }
            if (combo.size() > shown) {
                lines.add("... +" + (combo.size() - shown) + " autre(s)");
            }
            appendSocketLines(lines, w.getSocketCount(), w::getSocket);

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
            appendSocketLines(lines, c.getSocketCount(), c::getSocket);

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

    /** Section "Sockets" commune à Weapon et Capacity : "[1] VIDE" ou "[1] <résumé du mod>". */
    private static void appendSocketLines(List<String> lines, int socketCount, java.util.function.IntFunction<ItemModifier> getSocket) {
        if (socketCount == 0) return;
        lines.add("Sockets :");
        for (int i = 0; i < socketCount; i++) {
            ItemModifier mod = getSocket.apply(i);
            lines.add(mod == null
                ? String.format("[%d] VIDE", i + 1)
                : String.format("[%d] %s", i + 1, modifierSummary(mod)));
        }
    }

    /** Résumé une ligne d'un mod, utilisé dans la liste des sockets d'un item hôte. */
    private static String modifierSummary(ItemModifier mod) {
        if (mod instanceof WeaponComboMod) {
            WeaponComboMod wcm = (WeaponComboMod) mod;
            return mod.name + " (+" + wcm.comboAttacks.size() + " attaque(s))";
        }
        return mod.name;
    }

    /**
     * Détails d'un ItemModifier lui-même (survol dans la loot bar) :
     * "Modificateur d'arme — Combo : ..." ou "Modificateur de capacité — Effet : ...".
     */
    private static List<String> buildModifierLines(ItemModifier mod) {
        List<String> lines = new ArrayList<>();

        if (mod instanceof WeaponComboMod) {
            WeaponComboMod wcm = (WeaponComboMod) mod;
            lines.add("Modificateur d'arme — Combo :");
            for (WeaponAttack a : wcm.comboAttacks) {
                lines.add(String.format("%s  %.0f-%.0f dgt (cd %.2fs)", a.name, a.minDamage, a.maxDamage, a.cooldown));
            }
        } else if (mod instanceof CapacityEffectMod) {
            CapacityEffectMod cem = (CapacityEffectMod) mod;
            lines.add("Modificateur de capacité — Effet :");
            if (cem.effectOverride != null) {
                lines.add(String.format("%s (%s) Dgt %.0f",
                    cem.effectOverride.name, cem.effectOverride.type, cem.effectOverride.baseDamage));
            }
            if (cem.extraModifier != null) {
                lines.add(String.format("+ %s x%.1f", cem.extraModifier.type.name(), cem.extraModifier.intensity));
            }
        }

        Map<StatType, Float> bonuses = mod.getStatBonuses();
        for (StatType t : StatType.values()) {
            Float v = bonuses.get(t);
            if (v != null && v != 0f) {
                lines.add(String.format("+%.1f %s", v, statLabel(t)));
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
