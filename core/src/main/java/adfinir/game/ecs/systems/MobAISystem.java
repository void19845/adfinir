package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.*;
import adfinir.game.world.WorldMap;
import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;

import java.util.Random;

public class MobAISystem extends IteratingSystem {

    private static final Random RNG = new Random();

    private final ComponentMapper<TransformComponent> tm  = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<VelocityComponent>  vm  = ComponentMapper.getFor(VelocityComponent.class);
    private final ComponentMapper<MobAIComponent>     aim = ComponentMapper.getFor(MobAIComponent.class);
    private final ComponentMapper<HealthComponent>    hm  = ComponentMapper.getFor(HealthComponent.class);

    private final WorldMap map;
    private ImmutableArray<Entity> players;

    public MobAISystem(WorldMap map) {
        super(Family.all(TransformComponent.class, VelocityComponent.class, MobAIComponent.class).get(), 3);
        this.map = map;
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        players = engine.getEntitiesFor(
            Family.all(TransformComponent.class, PlayerInputComponent.class).get()
        );
    }

    @Override
    protected void processEntity(Entity mob, float dt) {
        TransformComponent pos = tm.get(mob);
        VelocityComponent  vel = vm.get(mob);
        MobAIComponent     ai  = aim.get(mob);
        HealthComponent    hp  = hm.get(mob);

        if (hp != null && hp.currentHealth <= 0) {
            ai.state = MobAIComponent.State.DEAD;
            vel.vx = 0; vel.vy = 0;
            return;
        }

        // Knockback
        if (ai.knockTimer > 0) {
            float nx = pos.x + ai.knockVx * dt;
            float ny = pos.y + ai.knockVy * dt;
            if (!map.isSolid(nx, pos.y)) pos.x = nx;
            if (!map.isSolid(pos.x, ny)) pos.y = ny;
            ai.knockTimer -= dt;
            vel.vx = 0; vel.vy = 0;
            return;
        }

        if (ai.currentCooldown > 0) ai.currentCooldown -= dt;

        // Joueur le plus proche
        Entity nearest    = null;
        float  nearestDst = Float.MAX_VALUE;
        for (Entity p : players) {
            TransformComponent pt = tm.get(p);
            if (pt == null) continue;
            float d = dst(pos.x, pos.y, pt.x, pt.y);
            if (d < nearestDst) { nearestDst = d; nearest = p; }
        }

        switch (ai.state) {
            case PATROL:
                doPatrol(pos, vel, ai, dt);
                if (nearest != null && nearestDst < ai.aggroRange)
                    ai.state = MobAIComponent.State.AGGRO;
                break;

            case AGGRO:
                if (nearest == null || nearestDst > ai.aggroRange * 1.6f) {
                    ai.state = MobAIComponent.State.PATROL;
                    break;
                }
                TransformComponent tgt = tm.get(nearest);
                moveToward(vel, pos, tgt.x, tgt.y, ai.speed);
                if (nearestDst < ai.attackRange)
                    ai.state = MobAIComponent.State.ATTACK;
                break;

            case ATTACK:
                if (nearest == null) { ai.state = MobAIComponent.State.PATROL; break; }
                tgt = tm.get(nearest);
                if (nearestDst > ai.attackRange * 1.5f) {
                    ai.state = MobAIComponent.State.AGGRO;
                    break;
                }
                vel.vx = 0; vel.vy = 0;
                if (ai.currentCooldown <= 0) {
                    attackPlayer(nearest, ai);
                    ai.currentCooldown = ai.attackCooldown;
                }
                break;

            case DEAD:
                vel.vx = 0; vel.vy = 0;
                break;
        }
    }

    private void attackPlayer(Entity player, MobAIComponent ai) {
        HealthComponent hp = player.getComponent(HealthComponent.class);
        if (hp == null) return;
        float def = 0f;
        StatsComponent stats = player.getComponent(StatsComponent.class);
        if (stats != null) def += stats.def;
        InventoryComponent inv = player.getComponent(InventoryComponent.class);
        if (inv != null) def += inv.getArmorDefBonus();
        float dmg = Math.max(1f, ai.attackDamage - def);
        hp.currentHealth = Math.max(0f, hp.currentHealth - dmg);
    }

    private void doPatrol(TransformComponent pos, VelocityComponent vel, MobAIComponent ai, float dt) {
        ai.patrolTimer -= dt;
        float dx = ai.patrolTargetX - pos.x;
        float dy = ai.patrolTargetY - pos.y;
        float d  = (float) Math.sqrt(dx * dx + dy * dy);
        if (d < 5f || ai.patrolTimer <= 0) {
            // Chercher une nouvelle cible non-solide
            for (int attempt = 0; attempt < 8; attempt++) {
                float tx = pos.x + (RNG.nextFloat() - 0.5f) * 80f;
                float ty = pos.y + (RNG.nextFloat() - 0.5f) * 80f;
                if (!map.isSolid(tx, ty)) {
                    ai.patrolTargetX = tx;
                    ai.patrolTargetY = ty;
                    break;
                }
            }
            ai.patrolTimer = 2f + RNG.nextFloat() * 2f;
        } else {
            moveToward(vel, pos, ai.patrolTargetX, ai.patrolTargetY, ai.speed * 0.5f);
        }
    }

    private void moveToward(VelocityComponent vel, TransformComponent pos,
                             float tx, float ty, float spd) {
        float dx = tx - pos.x, dy = ty - pos.y;
        float d  = (float) Math.sqrt(dx * dx + dy * dy);
        if (d > 0) { vel.vx = dx / d * spd; vel.vy = dy / d * spd; }
        else { vel.vx = 0; vel.vy = 0; }
    }

    private float dst(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
