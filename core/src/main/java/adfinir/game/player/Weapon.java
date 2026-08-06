package adfinir.game.player;

/**
 * Représente une arme équipée par le joueur.
 */
public class Weapon {
    public String name;
    public float damage;
    public float range;       // Largeur de la hitbox d'attaque
    public float cooldown;    // Temps entre deux attaques
    public float duration;    // Durée de l'attaque

    public Weapon(String name, float damage, float range, float cooldown, float duration) {
        this.name = name;
        this.damage = damage;
        this.range = range;
        this.cooldown = cooldown;
        this.duration = duration;
    }

    public static Weapon createSword() {
        return new Weapon("Épée courte", 15f, 30f, 0.4f, 0.15f);
    }

    public static Weapon createAxe() {
        return new Weapon("Hache lourde", 30f, 40f, 0.8f, 0.25f);
    }
}
