package adfinir.game.ecs.components;

import adfinir.game.inventory.Weapon;
import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

import java.util.HashSet;
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

    // --- Compétences (Capacity) — 4 slots indépendants, façon hotbar ---
    public final float[] spellTimers = new float[InventoryComponent.SPELL_SLOTS]; // Cooldown par slot
    public boolean capacityBursting = false;  // True pendant le court flash visuel du burst
    public float capacityBurstTimer = 0f;     // Temps restant du flash visuel

    public boolean canAttack() {
        return timer <= 0f && weapon != null;
    }

    public boolean canUseCapacity(int slot) {
        return slot >= 0 && slot < spellTimers.length && spellTimers[slot] <= 0f;
    }

    public void triggerAttack(float dirX, float dirY) {
        if (weapon == null) return;
        this.attackDirX = dirX;
        this.attackDirY = dirY;
        isAttacking = true;

        // L'attaque qui va effectivement se jouer est celle pointée par comboIndex AVANT incrément
        activeComboIndex = comboIndex;

        // Utilise le cooldown de l'attaque actuelle du combo
        timer = weapon.getModifiedCooldown(activeComboIndex);

        // Passe à l'attaque suivante pour le prochain coup
        comboIndex = (comboIndex + 1) % weapon.comboSlots.size();
        hitEntities.clear();
    }
}
