package adfinir.game.ecs.components;

import adfinir.game.inventory.CapacityModifier;
import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

import java.util.Collections;
import java.util.List;

/**
 * Marque une entité comme projectile (fireball de capacité, ou future flèche
 * d'arme). Le comportement (rebond, duplication au spawn, arc, explosion,
 * ricochet, vitesse) est entièrement piloté par la liste de modifiers issue
 * des sockets de la Capacity au moment du cast — voir ProjectileSystem.
 */
public class ProjectileComponent implements Component {
    /** Auteur du tir (évite de se blesser soi-même, extensible aux projectiles ennemis). */
    public Entity owner;

    public float damage;
    public float hitRadius = 6f;

    /** Modifiers actifs (peut être vide pour un projectile d'arme "brut"). */
    public List<CapacityModifier> modifiers = Collections.emptyList();

    public float lifetime = 2.5f; // secondes avant expiration silencieuse (pas d'explosion)

    // Charges consommables, dérivées des modifiers au spawn
    public int bouncesLeft = 0;     // BOUNCE
    public int ricochetsLeft = 0;   // RICOCHET
    public float explosionRadius = 0f; // EXPLOSION (0 = pas d'explosion)
    public float arcDegreesPerSecond = 0f; // ARC (0 = trajectoire droite)
}
