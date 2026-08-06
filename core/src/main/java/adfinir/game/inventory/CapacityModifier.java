package adfinir.game.inventory;

/**
 * Modificateur qui altère le comportement de l'effet principal.
 */
public class CapacityModifier {
    public enum ModType {
        BOUNCE,       // Rebond sur les murs
        DUPLICATE,    // Duplication du projectile
        ARC,          // Trajectoire en arc
        EXPLOSION,    // Explosion à l'impact
        RICOCHET,     // Ricochet entre ennemis
        SPEED_UP      // Augmentation de vitesse
    }

    public final ModType type;
    public final float intensity;

    public CapacityModifier(ModType type, float intensity) {
        this.type = type;
        this.intensity = intensity;
    }
}
