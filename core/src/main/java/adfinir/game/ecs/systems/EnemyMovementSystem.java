package adfinir.game.ecs.systems;

import adfinir.game.dungeon.DungeonMap;
import adfinir.game.ecs.components.EnemyAIComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.ecs.components.VelocityComponent;
import adfinir.game.util.Pathfinding;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import java.util.List;

/**
 * Système gérant le mouvement intelligent des ennemis.
 */
public class EnemyMovementSystem extends IteratingSystem {
    private final ComponentMapper<EnemyAIComponent> aiMapper = ComponentMapper.getFor(EnemyAIComponent.class);
    private final ComponentMapper<VelocityComponent> velMapper = ComponentMapper.getFor(VelocityComponent.class);
    private final ComponentMapper<TransformComponent> transformMapper = ComponentMapper.getFor(TransformComponent.class);

    private final DungeonMap map;
    private Entity player;

    public EnemyMovementSystem(DungeonMap map, Entity player) {
        super(Family.all(EnemyAIComponent.class, VelocityComponent.class, TransformComponent.class).get(), 0);
        this.map = map;
        this.player = player;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        EnemyAIComponent ai = aiMapper.get(entity);
        VelocityComponent vel = velMapper.get(entity);
        TransformComponent pos = transformMapper.get(entity);

        if (player == null) return;
        TransformComponent playerPos = transformMapper.get(player);
        if (playerPos == null) return;

        // Calcul distance au joueur
        float dx = playerPos.x - pos.x;
        float dy = playerPos.y - pos.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        // Gestion des états de l'IA
        if (dist < ai.detectionRange) {
            ai.state = EnemyAIComponent.State.PURSUING;
        } else if (dist > ai.detectionRange * 1.2f) { // Hystérésis pour éviter le clignotement d'état
            ai.state = EnemyAIComponent.State.IDLE;
        }

        if (ai.state == EnemyAIComponent.State.PURSUING) {
            // Poursuite intelligente via BFS
            Vector2 nextStep = Pathfinding.findNextStep(map,
                new Vector2(pos.x, pos.y),
                new Vector2(playerPos.x, playerPos.y)
            );

            if (nextStep != null) {
                float dirX = nextStep.x - pos.x;
                float dirY = nextStep.y - pos.y;
                float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
                if (len > 0) {
                    vel.vx = (dirX / len) * ai.pursuitSpeed;
                    vel.vy = (dirY / len) * ai.pursuitSpeed;
                }
            } else {
                // Si aucun chemin, on s'arrête ou on tente un mouvement aléatoire
                vel.vx = 0;
                vel.vy = 0;
            }
        } else {
            // Marche aléatoire (IDLE)
            ai.changeDirectionTimer -= deltaTime;
            if (ai.changeDirectionTimer <= 0f) {
                float angle = MathUtils.random(0, 360);
                float rad = (float) Math.toRadians(angle);
                vel.vx = (float) Math.cos(rad) * ai.speed;
                vel.vy = (float) Math.sin(rad) * ai.speed;
                ai.changeDirectionTimer = ai.moveDuration;
            }
        }
    }
}
