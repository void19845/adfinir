package adfinir.game.player;

import java.util.EnumMap;
import java.util.Map;

/**
 * Feuille de stats du personnage.
 *
 * Les stats finales = stats de base + somme de tous les bonus d'items équipés.
 * Les stats de base sont fixes (définies à la création du personnage).
 * Les bonus sont recalculés à chaque changement d'équipement.
 */
public class StatSheet {

    // Stats de base (immuables, définies à la création)
    private final Map<StatType, Float> base   = new EnumMap<>(StatType.class);

    // Bonus totaux venant des items équipés (recalculé par EquipmentComponent)
    private final Map<StatType, Float> bonus  = new EnumMap<>(StatType.class);

    // ---------------------------------------------------------------
    // Constructeur — stats de base du personnage sans équipement
    // ---------------------------------------------------------------

    public StatSheet() {
        // Stats de base par défaut
        base.put(StatType.MAX_HP,       100f);
        base.put(StatType.ATK,           10f);
        base.put(StatType.MAG,            5f);
        base.put(StatType.DEF,            5f);
        base.put(StatType.SPD,           400f); // pixels/seconde
        base.put(StatType.MAX_STAMINA,  100f);
        base.put(StatType.STAMINA_REGEN, 20f); // stamina/seconde

        // Initialise tous les bonus à 0
        for (StatType t : StatType.values()) bonus.put(t, 0f);
    }

    // ---------------------------------------------------------------
    // Lecture des stats finales
    // ---------------------------------------------------------------

    /** Retourne la stat finale (base + bonus). */
    public float get(StatType type) {
        return base.getOrDefault(type, 0f) + bonus.getOrDefault(type, 0f);
    }

    /** Raccourcis lisibles. */
    public float maxHp()        { return get(StatType.MAX_HP); }
    public float atk()          { return get(StatType.ATK); }
    public float mag()          { return get(StatType.MAG); }
    public float def()          { return get(StatType.DEF); }
    public float spd()          { return get(StatType.SPD); }
    public float maxStamina()   { return get(StatType.MAX_STAMINA); }
    public float staminaRegen() { return get(StatType.STAMINA_REGEN); }

    // ---------------------------------------------------------------
    // Gestion des bonus d'items
    // ---------------------------------------------------------------

    /** Remet tous les bonus à zéro (appelé avant de recalculer l'équipement). */
    public void clearBonus() {
        for (StatType t : StatType.values()) bonus.put(t, 0f);
    }

    /** Ajoute un bonus à une stat (peut être négatif). */
    public void addBonus(StatType type, float value) {
        bonus.put(type, bonus.getOrDefault(type, 0f) + value);
    }

    /** Applique tous les bonus d'une map d'un coup (pratique pour les items). */
    public void applyBonusMap(Map<StatType, Float> bonusMap) {
        for (Map.Entry<StatType, Float> e : bonusMap.entrySet()) {
            addBonus(e.getKey(), e.getValue());
        }
    }

    // ---------------------------------------------------------------
    // Debug
    // ---------------------------------------------------------------

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("StatSheet{\n");
        for (StatType t : StatType.values()) {
            sb.append(String.format("  %-15s base=%.1f  bonus=%.1f  final=%.1f\n",
                t.name(), base.getOrDefault(t, 0f), bonus.getOrDefault(t, 0f), get(t)));
        }
        sb.append("}");
        return sb.toString();
    }
}
