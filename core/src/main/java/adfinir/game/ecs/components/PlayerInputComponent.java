package adfinir.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool;

/**
 * Tag component : marque l'entité comme étant contrôlée par le joueur.
 */
public class PlayerInputComponent implements Component, Pool.Poolable {
    public float speed = 80f; // pixels/seconde

    // Direction du dernier mouvement non nul
    public float lastDirX = 1f;
    public float lastDirY = 0f;

    @Override
    public void reset() {
        speed = 80f;
        lastDirX = 1f;
        lastDirY = 0f;
    }
}
