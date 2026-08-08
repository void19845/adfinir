package adfinir.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Pool;

/**
 * Composant gérant les statistiques d'un ennemi.
 */
public class EnemyStatsComponent implements Component, Pool.Poolable {
    public enum EnemyType {
        SMALL,
        MEDIUM,
        LARGE
    }

    public float currentHp;
    public float maxHp = 50f;
    public float def = 2f;
    public boolean isDead = false;
    public EnemyType type;

    public EnemyStatsComponent() {
        reset();
    }

    @Override
    public void reset() {
        // Vie aléatoire entre 30 et 70 par défaut
        maxHp = MathUtils.random(30f, 70f);
        currentHp = maxHp;
        isDead = false;
        type = EnemyType.MEDIUM; // Default to medium if not specified
    }

    /** Inflige des dégâts en tenant compte de la DEF. Retourne les dégâts réels subis. */
    public float takeDamage(float rawDamage) {
        float dmg = Math.max(1f, rawDamage - def);
        currentHp = Math.max(0f, currentHp - dmg);
        if (currentHp <= 0f) isDead = true;
        return dmg;
    }
}
