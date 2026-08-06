package adfinir.game.ecs.components;

import com.badlogic.ashley.core.Component;

public class RenderComponent implements Component {

    public enum EntityType { PLAYER_1, PLAYER_2, MOB }

    public EntityType type  = EntityType.MOB;
    public float      width = 16f;
    public float      height = 16f;
}
