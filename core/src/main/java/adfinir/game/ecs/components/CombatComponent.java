package adfinir.game.ecs.components;

import adfinir.game.player.Weapon;
import com.badlogic.ashley.core.Component;

/**
 * Composant gérant l'état du combat d'une entité.
 * Les statistiques d'attaque sont désormais déléguées à l'objet Weapon.
 */
public class CombatComponent implements Component {
    // Arme actuellement équipée
    public Weapon weapon;

    // --- État actuel ---
    public float timer = 0f;            // Timer de cooldown
    public boolean isAttacking = false; // True si l'entité est en train de frapper

    public boolean canAttack() {
        return timer <= 0f && weapon != null;
    }

    public void triggerAttack() {
        if (weapon == null) return;
        isAttacking = true;
        timer = weapon.cooldown;
    }
}
