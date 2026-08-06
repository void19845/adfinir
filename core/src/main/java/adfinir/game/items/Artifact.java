package adfinir.game.items;


public class Artifact {

    public enum Slot {
        FLOWER("Fleur"), FEATHER("Plume"), CLOCK("Sablier"),
        CUP("Calice"), CROWN("Diadème");

        public final String label;
        Slot(String label) { this.label = label; }
    }

    public enum MainStat {
        HP("HP"), ATK("ATK"), DEF("DEF"),
        CRIT_RATE("Taux crit"), CRIT_DMG("Dég. crit"),
        ELEM_MASTERY("Maîtrise");

        public final String label;
        MainStat(String label) { this.label = label; }
    }

    // ── Champs principaux ─────────────────────────────────────────────────
    public String        name      = "Artéfact";
    public Item.Rarity   rarity    = Item.Rarity.COMMON;
    public Slot          slot      = Slot.FLOWER;
    public MainStat      mainStat  = MainStat.HP;
    public float         mainValue = 0f;

    // ── Sous-stats (bonus appliqués en permanence) ────────────────────────
    public float atkBonus      = 0f;
    public float defBonus      = 0f;
    public float critRateBonus = 0f;  // valeur absolue, ex: 0.05 = +5%
    public float critDmgBonus  = 0f;  // valeur absolue, ex: 0.10 = +10%

    public Artifact() {}

    public Artifact(String name, Item.Rarity rarity, Slot slot,
                    MainStat mainStat, float mainValue,
                    float atkBonus, float defBonus,
                    float critRateBonus, float critDmgBonus) {
        this.name = name; this.rarity = rarity;
        this.slot = slot; this.mainStat = mainStat; this.mainValue = mainValue;
        this.atkBonus = atkBonus; this.defBonus = defBonus;
        this.critRateBonus = critRateBonus; this.critDmgBonus = critDmgBonus;
    }

    /** Résumé court pour l'affichage dans l'inventaire. */
    public String summaryLine() {
        StringBuilder sb = new StringBuilder();
        sb.append(mainStat.label).append("+").append((int) mainValue);
        if (atkBonus      > 0) sb.append("  ATK+").append((int) atkBonus);
        if (defBonus      > 0) sb.append("  DEF+").append((int) defBonus);
        if (critRateBonus > 0) sb.append("  CR+").append((int)(critRateBonus * 100)).append("%");
        if (critDmgBonus  > 0) sb.append("  CD+").append((int)(critDmgBonus  * 100)).append("%");
        return sb.toString();
    }

    // ── Artéfacts par défaut (utilisés si items.json ne charge pas) ───────
    public static final Artifact ADVENTURER_FLOWER = new Artifact(
        "Fleur de l'aventurier", Item.Rarity.COMMON, Slot.FLOWER,
        MainStat.HP, 200f, 3f, 0f, 0f, 0f
    );
    public static final Artifact HERO_FEATHER = new Artifact(
        "Plume du héros", Item.Rarity.UNCOMMON, Slot.FEATHER,
        MainStat.ATK, 20f, 0f, 5f, 0.02f, 0f
    );
    public static final Artifact SWIFT_CLOCK = new Artifact(
        "Sablier rapide", Item.Rarity.RARE, Slot.CLOCK,
        MainStat.CRIT_RATE, 0.08f, 5f, 0f, 0f, 0.10f
    );
}
