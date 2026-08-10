package adfinir.game.ecs.systems;

import adfinir.game.combat.DamageResolver;
import adfinir.game.dungeon.DungeonMap;
import adfinir.game.ecs.components.EnemyProjectileComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.ecs.components.VelocityComponent;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;

/**
 * Déplace les projectiles tirés par les ennemis RANGED (voir
 * EnemyAttackSystem) et les résout contre le joueur : mur ou expiration =
 * disparition silencieuse, joueur à portée = dégâts puis disparition.
 * Volontairement séparé de ProjectileSystem (capacités du joueur, qui cible
 * les ennemis) pour ne pas complexifier ce dernier avec une cible variable.
 */
public class EnemyProjectileSystem extends IteratingSystem {

    private final ComponentMapper<EnemyProjectileComponent> pm = ComponentMapper.getFor(EnemyProjectileComponent.class);
    private final ComponentMapper<TransformComponent>       tm = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<VelocityComponent>        vm = ComponentMapper.getFor(VelocityComponent.class);

    private final DungeonMap map;
    private final Entity player;

    public EnemyProjectileSystem(DungeonMap map, Entity player) {
        super(Family.all(EnemyProjectileComponent.class, TransformComponent.class, VelocityComponent.class).get(), 7);
        this.map = map;
        this.player = player;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        EnemyProjectileComponent proj = pm.get(entity);
        TransformComponent pos = tm.get(entity);
        VelocityComponent vel = vm.get(entity);

        proj.lifetime -= deltaTime;
        if (proj.lifetime <= 0f) {
            getEngine().removeEntity(entity);
            return;
        }

        float nextX = pos.x + vel.vx * deltaTime;
        float nextY = pos.y + vel.vy * deltaTime;
        if (map.isSolid(nextX, pos.y) || map.isSolid(pos.x, nextY)) {
            getEngine().removeEntity(entity);
            return;
        }
        pos.x = nextX;
        pos.y = nextY;

        if (player == null) return;
        PlayerStatsComponent playerStats = player.getComponent(PlayerStatsComponent.class);
        TransformComponent playerPos = tm.get(player);
        if (playerStats == null || playerPos == null || playerStats.isDead) return;

        float dx = playerPos.x - pos.x;
        float dy = playerPos.y - pos.y;
        if (dx * dx + dy * dy <= proj.hitRadius * proj.hitRadius) {
            DamageResolver.applyDamage(player, proj.damage);
            getEngine().removeEntity(entity);
        }
    }
}
