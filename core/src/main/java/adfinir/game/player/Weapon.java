package adfinir.game.player;

/**
 * Représente une arme équipée par le joueur.
 */
public class Weapon {
    public String name;
    public float damage;
    public AttackShape shape;
    public float range;       // Pour SQUARE: taille du carré. Pour ARC: rayon. Pour RECTANGLE: longueur.
    public float width;       // Largeur pour RECTANGLE
    public float cooldown;    // Temps entre deux attaques (secondes)
    public float duration;    // Durée de l'attaque

    public Weapon(String name, float damage, AttackShape shape, float range, float width, float cooldown, float duration) {
        this.name = name;
        this.damage = damage;
        this.shape = shape;
        this.range = range;
        this.width = width;
        this.cooldown = cooldown;
        this.duration = duration;
    }

    public static Weapon createSword() {
        return new Weapon("Épée courte", 15f, AttackShape.SQUARE, 30f, 30f, 0.4f, 0.15f);
    }

    public static Weapon createAxe() {
        return new Weapon("Hache lourde", 30f, AttackShape.ARC, 40f, 0f, 0.8f, 0.25f);
    }

    public static Weapon createLance() {
        return new Weapon("Lance", 20f, AttackShape.RECTANGLE, 60f, 15f, 0.6f, 0.2f);
    }
}
