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

public class EnemyMovementSystem extends IteratingSystem {
    private final ComponentMapper<EnemyAIComponent> aiMapper = ComponentMapper.getFor(EnemyAIComponent.class);
    private final ComponentMapper<VelocityComponent> velMapper = ComponentMapper.getFor(VelocityComponent.class);
    private final ComponentMapper<TransformComponent> transformMapper = ComponentMapper.getFor(TransformComponent.class);

    private static final int MAX_RANDOM_TRIES = 8;
    // Distance de "sondage" devant l'ennemi pour valider la direction (en pixels)
    private static final float LOOKAHEAD = 20f;

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

        float dx = playerPos.x - pos.x;
        float dy = playerPos.y - pos.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < ai.detectionRange) {
            ai.state = EnemyAIComponent.State.PURSUING;
        } else if (dist > ai.detectionRange * 1.2f) {
            ai.state = EnemyAIComponent.State.IDLE;
        }

        if (ai.state == EnemyAIComponent.State.PURSUING) {
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
                vel.vx = 0;
                vel.vy = 0;
            }
        } else {
            // Marche aléatoire (IDLE) — ne choisit plus une direction menant dans un mur
            ai.changeDirectionTimer -= deltaTime;
            if (ai.changeDirectionTimer <= 0f) {
                boolean found = false;

                for (int i = 0; i < MAX_RANDOM_TRIES; i++) {
                    float angle = MathUtils.random(0, 360);
                    float rad = (float) Math.toRadians(angle);
                    float dirX = (float) Math.cos(rad);
                    float dirY = (float) Math.sin(rad);

                    if (isWalkable(pos.x + dirX * LOOKAHEAD, pos.y + dirY * LOOKAHEAD)) {
                        vel.vx = dirX * ai.speed;
                        vel.vy = dirY * ai.speed;
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    // Aucune direction libre trouvée (coin/impasse) : on reste immobile
                    // et on retente vite plutôt que d'attendre moveDuration entier
                    vel.vx = 0;
                    vel.vy = 0;
                    ai.changeDirectionTimer = 0.2f;
                    return;
                }

                ai.changeDirectionTimer = ai.moveDuration;
            }
        }
    }

    /** Vérifie que la case pixel (x,y) est dans les limites et n'est pas un mur. */
    private boolean isWalkable(float x, float y) {
        int col = (int) (x / DungeonMap.TILE_SIZE);
        int row = (int) (y / DungeonMap.TILE_SIZE);

        if (col < 0 || col >= map.cols || row < 0 || row >= map.rows) return false;
        return map.getTile(col, row) != DungeonMap.TILE_WALL;
    }
}
