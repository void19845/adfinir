package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.EnemyAIComponent;
import adfinir.game.ecs.components.VelocityComponent;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;

/**
 * Système gérant le mouvement simple des ennemis (marche aléatoire).
 */
public class EnemyMovementSystem extends IteratingSystem {
    private final ComponentMapper<EnemyAIComponent> aiMapper = ComponentMapper.getFor(EnemyAIComponent.class);
    private final ComponentMapper<VelocityComponent> velMapper = ComponentMapper.getFor(VelocityComponent.class);

    public EnemyMovementSystem() {
        super(Family.all(EnemyAIComponent.class, VelocityComponent.class).get(), 0);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        EnemyAIComponent ai = aiMapper.get(entity);
        VelocityComponent vel = velMapper.get(entity);

        ai.changeDirectionTimer -= deltaTime;

        if (ai.changeDirectionTimer <= 0f) {
            // Choisir une direction aléatoire
            float angle = MathUtils.random(0, 360);
            float rad = (float) Math.toRadians(angle);

            vel.vx = (float) Math.cos(rad) * ai.speed;
            vel.vy = (float) Math.sin(rad) * ai.speed;

            // Reset timer
            ai.changeDirectionTimer = ai.moveDuration;
        }
    }
}
