package adfinir.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool;

public class StatsComponent implements Component, Pool.Poolable {

    public float atk      = 10f;
    public float def      = 5f;
    public float critRate = 0.10f;  // 10%
    public float critDmg  = 1.50f;  // x1.5

    public int level         = 1;
    public int xp            = 0;
    public int xpToNextLevel = 100;

    @Override
    public void reset() {
        atk = 10f; def = 5f; critRate = 0.10f; critDmg = 1.50f;
        level = 1; xp = 0; xpToNextLevel = 100;
    }

    /** Retourne vrai et incrémente le niveau si on a assez d'XP. */
    public boolean tryLevelUp() {
        if (xp >= xpToNextLevel) {
            xp -= xpToNextLevel;
            level++;
            xpToNextLevel = (int)(xpToNextLevel * 1.4f);
            atk += 2f;
            def += 1f;
            return true;
        }
        return false;
    }
}
