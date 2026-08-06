package adfinir.game.inventory;

import adfinir.game.player.AttackShape;

/**
 * Type d'arme définissant les modificateurs de combat globaux.
 */
public enum WeaponType {
    SPEAR(1.2f, 0.8f, 1.1f, "Portée augmentée", AttackShape.RECTANGLE),
    SWORD(1.0f, 1.0f, 1.0f, "Équilibrée", AttackShape.CONE),
    CLAYMORE(1.0f, 1.3f, 1.5f, "Puissante, lente, knockback", AttackShape.ARC);

    public final float rangeMod;
    public final float damageMod;
    public final float cooldownMod;
    public final String description;
    public final AttackShape shape;

    WeaponType(float rangeMod, float damageMod, float cooldownMod, String description, AttackShape shape) {
        this.rangeMod = rangeMod;
        this.damageMod = damageMod;
        this.cooldownMod = cooldownMod;
        this.description = description;
        this.shape = shape;
    }
}
