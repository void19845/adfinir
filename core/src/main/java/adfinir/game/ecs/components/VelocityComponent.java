package adfinir.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool;

public class VelocityComponent implements Component, Pool.Poolable {
    public float vx = 0f;
    public float vy = 0f;

    @Override
    public void reset() {
        vx = 0f;
        vy = 0f;
    }
}
