package adfinir.game.inventory;

/**
 * Rareté globale influant sur les stats et le nombre de propriétés bonus.
 */
public enum Rarity {
    COMMON(1.0f, 0),
    RARE(1.1f, 1),
    EPIC(1.25f, 2),
    LEGENDARY(1.5f, 3),
    MYTHICAL(2.0f, 4),;

    public final float statMultiplier;
    public final int bonusPropertyCount;

    Rarity(float statMultiplier, int bonusPropertyCount) {
        this.statMultiplier = statMultiplier;
        this.bonusPropertyCount = bonusPropertyCount;
    }
}
