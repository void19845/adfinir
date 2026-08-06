package adfinir.game.ecs.components;

import com.badlogic.ashley.core.Component;

/**
 * Composant gérant les capacités de combat d'une entité.
 */
public class CombatComponent implements Component {
    // --- Paramètres d'attaque ---
    public float attackDamage = 10f;
    public float attackRange = 24f;      // Rayon ou largeur de la hitbox d'attaque
    public float attackCooldown = 0.5f; // Temps entre deux attaques (secondes)

    // --- État actuel ---
    public float timer = 0f;            // Timer de cooldown
    public boolean isAttacking = false;  // True si l'entité est en train de frapper
    public float attackDuration = 0.15f; // Durée pendant laquelle la hitbox est active

    public boolean canAttack() {
        return timer <= 0f;
    }

    public void triggerAttack() {
        isAttacking = true;
        timer = attackCooldown;
    }
}
