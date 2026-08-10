package adfinir.game.ecs.components;

import com.badlogic.ashley.core.Component;

/**
 * État de cast de la capacité active (clic droit). Ne duplique pas la
 * référence Capacity (contrairement à CombatComponent.weapon) : CapacitySystem
 * lit directement InventoryComponent.capacity, qui reste la seule source de
 * vérité sur l'équipement.
 */
public class CapacityComponent implements Component {
    public float timer = 0f;       // cooldown restant avant le prochain cast possible
    public boolean triggered = false; // intention de cast posée par PlayerInputSystem, consommée par CapacitySystem
    public float dirX = 1f;
    public float dirY = 0f;

    public boolean canCast() {
        return timer <= 0f;
    }

    public void trigger(float dirX, float dirY, float cooldown) {
        this.triggered = true;
        this.dirX = dirX;
        this.dirY = dirY;
        this.timer = cooldown;
    }
}
