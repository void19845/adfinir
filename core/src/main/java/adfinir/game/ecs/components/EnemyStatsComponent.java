package adfinir.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Pool;

/**
 * Composant gérant les statistiques d'un ennemi.
 */
public class EnemyStatsComponent implements Component, Pool.Poolable {
    public float currentHp;
    public float maxHp = 50f;
    public float def = 2f;
    public boolean isDead = false;

    // Attaque de contact — appliquée par EnemyAttackSystem
    public float attackDamage = 5f;
    public float attackRange = 14f;
    public float attackCooldown = 1.0f;
    public float attackTimer = 0f;

    public EnemyStatsComponent() {
        reset();
    }

    @Override
    public void reset() {
        // Vie aléatoire entre 30 et 70 par défaut
        maxHp = MathUtils.random(30f, 70f);
        currentHp = maxHp;
        isDead = false;
        attackTimer = 0f;
    }

    /** Inflige des dégâts en tenant compte de la DEF. Retourne les dégâts réels subis. */
    public float takeDamage(float rawDamage) {
        float dmg = Math.max(1f, rawDamage - def);
        currentHp = Math.max(0f, currentHp - dmg);
        if (currentHp <= 0f) isDead = true;
        return dmg;
    }

    /**
     * Applique le facteur de menace de l'étage : augmente HP, DEF et dégâts d'attaque.
     * À appeler juste après la création du composant (avant l'ajout à l'entité).
     */
    public void applyThreatFactor(float threatFactor) {
        maxHp = maxHp * threatFactor;
        currentHp = maxHp;
        def = def * threatFactor;
        attackDamage = attackDamage * threatFactor;
    }
}
