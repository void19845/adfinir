package adfinir.game.enemy;

import adfinir.game.ecs.components.EnemyStatsComponent;
import com.badlogic.gdx.utils.Pool;

public class EnemyGenerator {
    private final Pool<EnemyStatsComponent> enemyPool;

    public EnemyGenerator(Pool<EnemyStatsComponent> enemyPool) {
        this.enemyPool = enemyPool;
    }

    public EnemyStatsComponent generateEnemy() {
        EnemyStatsComponent enemy = enemyPool.obtain();
        int rarityRoll = MathUtils.random(100);

        if (rarityRoll < 5) { // 5% chance for large enemies
            enemy.type = EnemyStatsComponent.EnemyType.LARGE;
            enemy.maxHp *= 2;
            enemy.def *= 1.5f;
        } else if (rarityRoll < 30) { // 25% chance for medium enemies
            enemy.type = EnemyStatsComponent.EnemyType.MEDIUM;
        } else { // 70% chance for small enemies
            enemy.type = EnemyStatsComponent.EnemyType.SMALL;
            enemy.maxHp *= 0.5f;
            enemy.def *= 0.75f;
        }

        enemy.currentHp = enemy.maxHp;
        return enemy;
    }
}
