package adfinir.game.inventory;

/**
 * Type d'arme définissant les modificateurs de combat globaux (stats).
 * Ne définit plus la forme d'attaque : celle-ci est portée par chaque
 * WeaponAttack (voir AttackShape), fournie via les sockets de l'arme.
 */
public enum WeaponType {
    SPEAR(2.0f, 0.8f, 1.1f, "Portée augmentée"),
    SWORD(1.0f, 1.0f, 1.0f, "Équilibrée"),
    CLAYMORE(1.0f, 2.0f, 1.5f, "Puissante, lente, knockback");

    public final float rangeMod;
    public final float damageMod;
    public final float cooldownMod;
    public final String description;

    WeaponType(float rangeMod, float damageMod, float cooldownMod, String description) {
        this.rangeMod = rangeMod;
        this.damageMod = damageMod;
        this.cooldownMod = cooldownMod;
        this.description = description;
    }
}
