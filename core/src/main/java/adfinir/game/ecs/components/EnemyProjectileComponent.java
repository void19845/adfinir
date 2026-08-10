package adfinir.game.ecs.components;

import com.badlogic.ashley.core.Component;

/**
 * Marque un projectile tiré par un ennemi de type RANGED (voir
 * EnemyAttackSystem / EnemyProjectileSystem). Volontairement minimal —
 * contrairement à ProjectileComponent (capacités du joueur), pas de rebond/
 * ricochet/explosion/arc : un tir ennemi va tout droit et touche le joueur
 * une seule fois.
 */
public class EnemyProjectileComponent implements Component {
    public float damage;
    public float hitRadius = 6f;
    public float lifetime = 3f;
}
