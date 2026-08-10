package adfinir.game.player;

/**
 * Définit la forme visuelle et logique de la zone d'attaque.
 */
public enum AttackShape {
    CONE,           // Attaque en cône / wedge
    ARC,            // Attaque en arc de cercle
    RECTANGLE,      // Attaque longue et étroite, collée au joueur
    CIRCLE,         // Disque plein centré sur le joueur (rayon fixe)
    FIXED_DISTANCE, // Hitbox statique spawnée à distance fixe devant le joueur
    PROJECTILE      // Spawn une entité projectile qui voyage (voir ProjectileComponent)
}
