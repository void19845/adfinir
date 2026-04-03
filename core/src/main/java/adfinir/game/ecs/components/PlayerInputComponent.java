package adfinir.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Pool;

public class PlayerInputComponent implements Component, Pool.Poolable {
    public float speed = 80f;

    // Touches configurables
    public int upKey     = Input.Keys.UP;
    public int downKey   = Input.Keys.DOWN;
    public int leftKey   = Input.Keys.LEFT;
    public int rightKey  = Input.Keys.RIGHT;
    public int attackKey = Input.Keys.SPACE;

    @Override
    public void reset() {
        speed = 80f;
        upKey = Input.Keys.UP; downKey = Input.Keys.DOWN;
        leftKey = Input.Keys.LEFT; rightKey = Input.Keys.RIGHT;
        attackKey = Input.Keys.SPACE;
    }
}
