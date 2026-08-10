package adfinir.game.inventory;

import adfinir.game.player.AttackShape;

/**
 * Définit une attaque spécifique dans une séquence de combo d'arme.
 * La forme (shape) est portée par l'attaque elle-même, pas par l'arme :
 * une même arme peut ainsi avoir des attaques de formes différentes selon
 * les WeaponComboMod socketés.
 */
public class WeaponAttack {
    public final String name;
    public final float minDamage;
    public final float maxDamage;
    public final float cooldown;
    public final float duration;
    public final float knockback;
    public final String element; // null si physique
    public final float areaOfEffect; // réinterprété selon shape (rayon/longueur/distance fixe)
    public final AttackShape shape;
    /** Vitesse du projectile en pixels/seconde. Ignoré si shape != PROJECTILE. */
    public final float projectileSpeed;

    public WeaponAttack(String name, float minDamage, float maxDamage, float cooldown, float duration,
                        float knockback, String element, float areaOfEffect, AttackShape shape) {
        this(name, minDamage, maxDamage, cooldown, duration, knockback, element, areaOfEffect, shape, 0f);
    }

    public WeaponAttack(String name, float minDamage, float maxDamage, float cooldown, float duration,
                        float knockback, String element, float areaOfEffect, AttackShape shape,
                        float projectileSpeed) {
        this.name = name;
        this.minDamage = minDamage;
        this.maxDamage = maxDamage;
        this.cooldown = cooldown;
        this.duration = duration;
        this.knockback = knockback;
        this.element = element;
        this.areaOfEffect = areaOfEffect;
        this.shape = shape;
        this.projectileSpeed = projectileSpeed;
    }

    public float getRandomDamage() {
        return minDamage + (float) Math.random() * (maxDamage - minDamage);
    }
}
