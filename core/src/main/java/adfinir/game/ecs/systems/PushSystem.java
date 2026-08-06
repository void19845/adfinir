package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.MobAIComponent;
import adfinir.game.ecs.components.PlayerInputComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.world.WorldMap;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;

/**
 * Résout les collisions physiques entre le joueur et les mobs.
 * Le joueur pousse les mobs, et subit un léger refoulement en retour.
 */
public class PushSystem extends EntitySystem {

    private static final float PUSH_RADIUS  = 12f;  // distance de contact
    private static final float MOB_PUSH     = 2.5f;  // force appliquée au mob
    private static final float PLAYER_PUSH  = 0.6f;  // refoulement léger sur le joueur

    private final ComponentMapper<TransformComponent> tm  = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<MobAIComponent>     aim = ComponentMapper.getFor(MobAIComponent.class);

    private final WorldMap worldMap;

    private ImmutableArray<Entity> players;
    private ImmutableArray<Entity> mobs;

    public PushSystem(WorldMap worldMap) {
        super(6); // priorité après MobAISystem (3) et avant RenderSystem (10)
        this.worldMap = worldMap;
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        players = engine.getEntitiesFor(
            Family.all(TransformComponent.class, PlayerInputComponent.class).get()
        );
        mobs = engine.getEntitiesFor(
            Family.all(TransformComponent.class, MobAIComponent.class).get()
        );
    }

    @Override
    public void update(float deltaTime) {
        for (Entity player : players) {
            TransformComponent pPos = tm.get(player);

            for (Entity mob : mobs) {
                MobAIComponent ai = aim.get(mob);
                if (ai == null || ai.state == MobAIComponent.State.DEAD) continue;

                TransformComponent mPos = tm.get(mob);

                float dx = mPos.x - pPos.x;
                float dy = mPos.y - pPos.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist >= PUSH_RADIUS || dist < 0.01f) continue;

                float overlap = PUSH_RADIUS - dist;
                float nx = dx / dist;
                float ny = dy / dist;

                // Pousser le mob (si pas en terrain solide)
                float newMX = mPos.x + nx * overlap * MOB_PUSH;
                float newMY = mPos.y + ny * overlap * MOB_PUSH;
                if (!worldMap.isSolid(newMX, mPos.y)) mPos.x = newMX;
                if (!worldMap.isSolid(mPos.x, newMY)) mPos.y = newMY;

                // Léger refoulement du joueur
                float newPX = pPos.x - nx * overlap * PLAYER_PUSH;
                float newPY = pPos.y - ny * overlap * PLAYER_PUSH;
                if (!worldMap.isSolid(newPX, pPos.y)) pPos.x = newPX;
                if (!worldMap.isSolid(pPos.x, newPY)) pPos.y = newPY;
            }
        }
    }
}
