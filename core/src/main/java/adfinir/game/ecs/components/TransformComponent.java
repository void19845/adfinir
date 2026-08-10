package adfinir.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool;

public class TransformComponent implements Component, Pool.Poolable {
    public float x = 0f;
    public float y = 0f;
    public float rotation = 0f;

    @Override
    public void reset() {
        x = 0f;
        y = 0f;
        rotation = 0f;
    }
}
