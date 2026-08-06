package adfinir.game.inventory;

/**
 * Type d'armure définissant la distribution des statistiques.
 */
public enum ArmorType {
    LIGHT("Légère", 1.2f, 0.8f, 1.0f),
    MEDIUM("Moyenne", 1.0f, 1.0f, 1.0f),
    HEAVY("Lourde", 0.7f, 1.5f, 0.8f);

    public final String name;
    public final float speedMod;
    public final float defenseMod;
    public final float hpMod;

    ArmorType(String name, float speedMod, float defenseMod, float hpMod) {
        this.name = name;
        this.speedMod = speedMod;
        this.defenseMod = defenseMod;
        this.hpMod = hpMod;
    }
}
