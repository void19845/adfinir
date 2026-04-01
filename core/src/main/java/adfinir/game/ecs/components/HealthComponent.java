package adfinir.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool;

public class HealthComponent {
    public float currentHealth = 100f ;
    public float maxHealth = 100f ;

    public void reset() {
        currentHealth = 100f ;
        maxHealth = 100f ;
    }
}
