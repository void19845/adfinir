package adfinir.game.inventory;

/**
 * Définit une attaque spécifique dans une séquence de combo d'arme.
 */
public class WeaponAttack {
    public final String name;
    public final float minDamage;
    public final float maxDamage;
    public final float cooldown;
    public final float duration;
    public final float knockback;
    public final String element; // null si physique
    public final float areaOfEffect;

    public WeaponAttack(String name, float minDamage, float maxDamage, float cooldown, float duration, float knockback, String element, float areaOfEffect) {
        this.name = name;
        this.minDamage = minDamage;
        this.maxDamage = maxDamage;
        this.cooldown = cooldown;
        this.duration = duration;
        this.knockback = knockback;
        this.element = element;
        this.areaOfEffect = areaOfEffect;
    }

    public float getRandomDamage() {
        return minDamage + (float) Math.random() * (maxDamage - minDamage);
    }
}
