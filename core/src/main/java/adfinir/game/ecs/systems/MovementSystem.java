package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.ecs.components.VelocityComponent;
import adfinir.game.dungeon.DungeonMap;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;

public class MovementSystem extends IteratingSystem {

    private final ComponentMapper<TransformComponent> tm = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<VelocityComponent>  vm = ComponentMapper.getFor(VelocityComponent.class);

    private final DungeonMap map;

    public MovementSystem(DungeonMap map) {
        super(Family.all(TransformComponent.class, VelocityComponent.class).get(), 2);
        this.map = map;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent pos = tm.get(entity);
        VelocityComponent  vel = vm.get(entity);

        float nextX = pos.x + vel.vx * deltaTime;
        float nextY = pos.y + vel.vy * deltaTime;

        // Hitbox : 12x12 pixels centrée sur (pos.x, pos.y)
        // On teste les 4 coins pour éviter le clipping entre deux tiles.
        float half = 5f; // demi-taille de la hitbox (légèrement < taille visuelle)

        // --- Axe X : tester les 4 coins sur nextX avec pos.y actuel ---
        boolean blockedX = map.isSolid(nextX - half, pos.y - half)
                        || map.isSolid(nextX + half, pos.y - half)
                        || map.isSolid(nextX - half, pos.y + half)
                        || map.isSolid(nextX + half, pos.y + half);
        if (!blockedX) pos.x = nextX;

        // --- Axe Y : tester les 4 coins sur nextY avec pos.x déjà mis à jour ---
        boolean blockedY = map.isSolid(pos.x - half, nextY - half)
                        || map.isSolid(pos.x + half, nextY - half)
                        || map.isSolid(pos.x - half, nextY + half)
                        || map.isSolid(pos.x + half, nextY + half);
        if (!blockedY) pos.y = nextY;
    }
}
