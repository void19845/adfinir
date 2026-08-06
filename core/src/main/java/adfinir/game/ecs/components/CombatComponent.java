package adfinir.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool;

public class CombatComponent implements Component, Pool.Poolable {

    public float attackRange     = 20f;
    public float attackCooldown  = 0.4f;
    public float currentCooldown = 0f;
    public float knockbackForce  = 130f;

    /** Mis à true par PlayerInputSystem quand la touche d'attaque est pressée. */
    public boolean wantsToAttack      = false;

    // ── Attaque puissante (touche R) ──────────────────────────────────────
    public float   powerRange         = 38f;
    public float   powerCooldown      = 1.5f;
    public float   powerCurrentCD     = 0f;
    public boolean wantsPowerAttack   = false;

    @Override
    public void reset() {
        attackRange = 20f; attackCooldown = 0.4f;
        currentCooldown = 0f; knockbackForce = 130f;
        wantsToAttack = false;
        powerRange = 38f; powerCooldown = 1.5f;
        powerCurrentCD = 0f; wantsPowerAttack = false;
    }
}
