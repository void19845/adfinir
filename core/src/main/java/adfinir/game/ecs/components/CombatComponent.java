package adfinir.game.ecs.components;

import adfinir.game.inventory.Weapon;
import adfinir.game.inventory.WeaponAttack;
import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public int comboIndex = 0;          // Indice de la PROCHAINE attaque du combo
    public int activeComboIndex = 0;    // Indice de l'attaque EN COURS (fenêtre active actuelle)

    // Entités déjà touchées pendant la fenêtre active courante (évite les hits multiples)
    public final Set<Entity> hitEntities = new HashSet<>();

    public boolean canAttack() {
        return timer <= 0f && weapon != null && !weapon.getActiveCombo().isEmpty();
    }

    public void triggerAttack(float dirX, float dirY) {
        if (weapon == null) return;
        List<WeaponAttack> combo = weapon.getActiveCombo();
        if (combo.isEmpty()) return;

        this.attackDirX = dirX;
        this.attackDirY = dirY;
        isAttacking = true;

        // L'attaque qui va effectivement se jouer est celle pointée par comboIndex AVANT incrément
        activeComboIndex = comboIndex;

        // Utilise le cooldown de l'attaque actuelle du combo (base + sockets)
        timer = weapon.getModifiedCooldown(activeComboIndex);

        // Passe à l'attaque suivante pour le prochain coup
        comboIndex = (comboIndex + 1) % combo.size();
        hitEntities.clear();
    }
}
