package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.ecs.components.VelocityComponent;
import adfinir.game.world.WorldMap;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;

public class MovementSystem extends IteratingSystem {

    private final ComponentMapper<TransformComponent> tm = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<VelocityComponent>  vm = ComponentMapper.getFor(VelocityComponent.class);

    private final WorldMap map;

    public MovementSystem(WorldMap map) {
        super(Family.all(TransformComponent.class, VelocityComponent.class).get(), 2);
        this.map = map;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent pos = tm.get(entity);
        VelocityComponent  vel = vm.get(entity);

        float nextX = pos.x + vel.vx * deltaTime;
        float nextY = pos.y + vel.vy * deltaTime;

        float half = 5f; // demi-hitbox (légèrement < taille visuelle)

        // Axe X
        boolean blockedX = map.isSolid(nextX - half, pos.y - half)
                        || map.isSolid(nextX + half, pos.y - half)
                        || map.isSolid(nextX - half, pos.y + half)
                        || map.isSolid(nextX + half, pos.y + half);
        if (!blockedX) pos.x = nextX;

        // Axe Y
        boolean blockedY = map.isSolid(pos.x - half, nextY - half)
                        || map.isSolid(pos.x + half, nextY - half)
                        || map.isSolid(pos.x - half, nextY + half)
                        || map.isSolid(pos.x + half, nextY + half);
        if (!blockedY) pos.y = nextY;

        // Clamp dans les limites du monde
        pos.x = Math.max(half, Math.min(map.getPixelWidth()  - half, pos.x));
        pos.y = Math.max(half, Math.min(map.getPixelHeight() - half, pos.y));
    }
}
