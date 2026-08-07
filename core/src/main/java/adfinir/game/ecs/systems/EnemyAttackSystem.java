package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.EnemyStatsComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import adfinir.game.ecs.components.TransformComponent;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;

/**
 * Applique les dégâts de contact des ennemis sur le joueur quand ils sont
 * à portée. Dégâts et portée viennent de EnemyStatsComponent, déjà mis à
 * l'échelle par le threatFactor de l'étage via applyThreatFactor().
 */
public class EnemyAttackSystem extends IteratingSystem {
    private final ComponentMapper<EnemyStatsComponent> statsMapper = ComponentMapper.getFor(EnemyStatsComponent.class);
    private final ComponentMapper<TransformComponent> transformMapper = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<PlayerStatsComponent> playerStatsMapper = ComponentMapper.getFor(PlayerStatsComponent.class);

    private final Entity player;

    public EnemyAttackSystem(Entity player) {
        super(Family.all(EnemyStatsComponent.class, TransformComponent.class).get(), 5);
        this.player = player;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        EnemyStatsComponent stats = statsMapper.get(entity);
        if (stats.isDead) return;

        if (stats.attackTimer > 0f) {
            stats.attackTimer -= deltaTime;
            return;
        }

        TransformComponent pos = transformMapper.get(entity);
        TransformComponent playerPos = transformMapper.get(player);
        PlayerStatsComponent playerStats = playerStatsMapper.get(player);
        if (playerPos == null || playerStats == null) return;

        float dx = playerPos.x - pos.x;
        float dy = playerPos.y - pos.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist <= stats.attackRange) {
            playerStats.takeDamage(stats.attackDamage);
            stats.attackTimer = stats.attackCooldown;
        }
    }
}
