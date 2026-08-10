package adfinir.game.ecs.systems;

import adfinir.game.dungeon.DungeonMap;
import adfinir.game.ecs.components.EnemyStatsComponent;
import adfinir.game.ecs.components.ProjectileComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.ecs.components.VelocityComponent;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;

/**
 * Déplace les projectiles et résout leur comportement selon les charges
 * dérivées des modifiers au spawn (voir CapacitySystem) : rebond sur les
 * murs (BOUNCE), courbure de trajectoire (ARC), dégâts de zone à l'impact
 * (EXPLOSION), rebond entre ennemis (RICOCHET). Un ennemi est traité comme
 * un point (cohérent avec EnemyAttackSystem) ; approximation volontaire,
 * pas de vraie hitbox circulaire côté ennemi.
 */
public class ProjectileSystem extends IteratingSystem {

    private static final float ENEMY_HIT_RADIUS = 6f;

    private final ComponentMapper<ProjectileComponent> pm = ComponentMapper.getFor(ProjectileComponent.class);
    private final ComponentMapper<TransformComponent>  tm = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<VelocityComponent>   vm = ComponentMapper.getFor(VelocityComponent.class);
    private final ComponentMapper<EnemyStatsComponent>  esm = ComponentMapper.getFor(EnemyStatsComponent.class);

    private final DungeonMap map;

    public ProjectileSystem(DungeonMap map) {
        super(Family.all(ProjectileComponent.class, TransformComponent.class, VelocityComponent.class).get(), 7);
        this.map = map;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        ProjectileComponent proj = pm.get(entity);
        TransformComponent  pos  = tm.get(entity);
        VelocityComponent   vel  = vm.get(entity);

        proj.lifetime -= deltaTime;
        if (proj.lifetime <= 0f) {
            getEngine().removeEntity(entity); // expiration silencieuse, pas un impact
            return;
        }

        if (proj.arcDegreesPerSecond != 0f) {
            float rad = (float) Math.toRadians(proj.arcDegreesPerSecond * deltaTime);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            float nvx = vel.vx * cos - vel.vy * sin;
            float nvy = vel.vx * sin + vel.vy * cos;
            vel.vx = nvx;
            vel.vy = nvy;
        }

        float nextX = pos.x + vel.vx * deltaTime;
        float nextY = pos.y + vel.vy * deltaTime;
        boolean blockedX = map.isSolid(nextX, pos.y);
        boolean blockedY = map.isSolid(pos.x, nextY);

        if (blockedX || blockedY) {
            if (proj.bouncesLeft > 0) {
                if (blockedX) vel.vx = -vel.vx;
                if (blockedY) vel.vy = -vel.vy;
                proj.bouncesLeft--;
                return;
            }
            resolveImpact(entity, proj, pos.x, pos.y);
            return;
        }

        pos.x = nextX;
        pos.y = nextY;

        for (Entity enemy : getEngine().getEntitiesFor(Family.all(EnemyStatsComponent.class, TransformComponent.class).get())) {
            EnemyStatsComponent stats = esm.get(enemy);
            if (stats.isDead) continue;
            TransformComponent ePos = tm.get(enemy);
            float dx = ePos.x - pos.x;
            float dy = ePos.y - pos.y;
            if (Math.sqrt(dx * dx + dy * dy) <= proj.hitRadius + ENEMY_HIT_RADIUS) {
                onEnemyHit(entity, proj, enemy, pos.x, pos.y);
                return;
            }
        }
    }

    private void onEnemyHit(Entity projectileEntity, ProjectileComponent proj, Entity enemy, float x, float y) {
        CombatSystem combat = getEngine().getSystem(CombatSystem.class);

        if (proj.ricochetsLeft > 0) {
            Entity next = findNearestOtherEnemy(enemy, x, y);
            if (next != null) {
                combat.applyDamage(proj.owner, enemy, proj.damage);
                proj.ricochetsLeft--;
                redirectTowards(projectileEntity, next, x, y);
                return; // le projectile continue sa route, pas un impact final
            }
        }

        // Impact final : si une explosion est prévue, elle couvre déjà la cible touchée
        // (distance 0 <= explosionRadius) — éviter de compter le dégât deux fois.
        if (proj.explosionRadius <= 0f) {
            combat.applyDamage(proj.owner, enemy, proj.damage);
        }
        resolveImpact(projectileEntity, proj, x, y);
    }

    /** Fin de vie par impact (mur, ou ennemi sans ricochet restant) : explosion éventuelle puis suppression. */
    private void resolveImpact(Entity projectileEntity, ProjectileComponent proj, float x, float y) {
        if (proj.explosionRadius > 0f) {
            CombatSystem combat = getEngine().getSystem(CombatSystem.class);
            for (Entity enemy : getEngine().getEntitiesFor(Family.all(EnemyStatsComponent.class, TransformComponent.class).get())) {
                EnemyStatsComponent stats = esm.get(enemy);
                if (stats.isDead) continue;
                TransformComponent ePos = tm.get(enemy);
                float dx = ePos.x - x;
                float dy = ePos.y - y;
                if (Math.sqrt(dx * dx + dy * dy) <= proj.explosionRadius) {
                    combat.applyDamage(proj.owner, enemy, proj.damage);
                }
            }
        }
        getEngine().removeEntity(projectileEntity);
    }

    private void redirectTowards(Entity projectileEntity, Entity target, float x, float y) {
        TransformComponent tPos = tm.get(target);
        VelocityComponent  vel  = vm.get(projectileEntity);
        float dx = tPos.x - x;
        float dy = tPos.y - y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float speed = (float) Math.sqrt(vel.vx * vel.vx + vel.vy * vel.vy);
        if (len > 0.001f) {
            vel.vx = dx / len * speed;
            vel.vy = dy / len * speed;
        }
    }

    private Entity findNearestOtherEnemy(Entity exclude, float x, float y) {
        Entity nearest = null;
        float bestDistSq = Float.MAX_VALUE;
        for (Entity enemy : getEngine().getEntitiesFor(Family.all(EnemyStatsComponent.class, TransformComponent.class).get())) {
            if (enemy == exclude) continue;
            EnemyStatsComponent stats = esm.get(enemy);
            if (stats.isDead) continue;
            TransformComponent ePos = tm.get(enemy);
            float dx = ePos.x - x;
            float dy = ePos.y - y;
            float distSq = dx * dx + dy * dy;
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                nearest = enemy;
            }
        }
        return nearest;
    }
}
