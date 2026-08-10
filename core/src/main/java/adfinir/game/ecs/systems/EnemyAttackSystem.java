package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.EnemyProjectileComponent;
import adfinir.game.ecs.components.EnemyStatsComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import adfinir.game.ecs.components.RenderComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.ecs.components.VelocityComponent;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Color;

/**
 * Applique l'attaque des ennemis sur le joueur quand il est à portée :
 * dégâts de contact direct pour un ennemi normal, tir d'un
 * EnemyProjectileComponent (voir EnemyProjectileSystem) pour un ennemi
 * RANGED (EnemyStatsComponent.ranged). Dégâts/portée/cooldown viennent de
 * EnemyStatsComponent, déjà mis à l'échelle par le threatFactor de l'étage
 * via applyThreatFactor() puis par le profil d'EnemyType.
 */
public class EnemyAttackSystem extends IteratingSystem {
    private static final float PROJECTILE_SPEED = 130f;

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

        if (dist > stats.attackRange) return;

        if (stats.ranged) {
            fireProjectile(pos, dx, dy, dist, stats);
        } else {
            playerStats.takeDamage(stats.attackDamage);
        }
        stats.attackTimer = stats.attackCooldown;
    }

    private void fireProjectile(TransformComponent origin, float dx, float dy, float dist, EnemyStatsComponent stats) {
        if (dist <= 0.001f) return;
        float dirX = dx / dist;
        float dirY = dy / dist;

        Entity projectile = new Entity();

        TransformComponent transform = new TransformComponent();
        transform.x = origin.x;
        transform.y = origin.y;

        VelocityComponent vel = new VelocityComponent();
        vel.vx = dirX * PROJECTILE_SPEED;
        vel.vy = dirY * PROJECTILE_SPEED;

        RenderComponent render = new RenderComponent();
        render.width = 6f;
        render.height = 6f;
        render.color = new Color(0.3f, 0.65f, 1f, 1f);

        EnemyProjectileComponent proj = new EnemyProjectileComponent();
        proj.damage = stats.attackDamage;

        projectile.add(transform);
        projectile.add(vel);
        projectile.add(render);
        projectile.add(proj);

        getEngine().addEntity(projectile);
    }
}
