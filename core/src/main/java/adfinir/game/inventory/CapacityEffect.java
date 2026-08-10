package adfinir.game.inventory;

/**
 * Effet de base d'une capacité (le projectile ou l'action principale).
 */
public class CapacityEffect {
    public final String name;
    public final float baseDamage;
    public final float speed;
    public final float radius;
    public final String type; // "FIRE", "ICE", "ELECTRIC", etc.

    public CapacityEffect(String name, float baseDamage, float speed, float radius, String type) {
        this.name = name;
        this.baseDamage = baseDamage;
        this.speed = speed;
        this.radius = radius;
        this.type = type;
    }
}
