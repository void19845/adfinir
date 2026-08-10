package adfinir.game.ecs.components;

import adfinir.game.inventory.Weapon;
import com.badlogic.ashley.core.Component;

/**
 * Composant gérant l'état du combat d'une entité.
 * Les statistiques d'attaque sont désormais déléguées à l'objet Weapon.
 */
public class CombatComponent implements Component {
    // Arme actuellement équipée
    public Weapon weapon;

    // Direction de l'attaque au moment du déclenchement (normalisé)
    public float attackDirX = 1f;
    public float attackDirY = 0f;

    // --- État actuel ---
    public float timer = 0f;            // Timer de cooldown
    public boolean isAttacking = false; // True si l'entité est en train de frapper
    public boolean hasHit = false;      // True si l'attaque a déjà touché quelqu'un
    public int comboIndex = 0;          // Indice de l'attaque actuelle dans le combo

    public boolean canAttack() {
        return timer <= 0f && weapon != null && !weapon.getActiveCombo().isEmpty();
    }

    public void triggerAttack(float dirX, float dirY) {
        if (weapon == null) return;
        java.util.List<adfinir.game.inventory.WeaponAttack> combo = weapon.getActiveCombo();
        if (combo.isEmpty()) return;

        this.attackDirX = dirX;
        this.attackDirY = dirY;
        isAttacking = true;

        // Utilise le cooldown de l'attaque actuelle du combo (base + sockets)
        timer = weapon.getModifiedCooldown(comboIndex);

        // Passe à l'attaque suivante pour le prochain coup
        comboIndex = (comboIndex + 1) % combo.size();
        hasHit = false; // Reset hit for new attack
    }
}
