package adfinir.game.ecs.systems;

import adfinir.game.combat.DamageResolver;
import adfinir.game.ecs.components.DashComponent;
import adfinir.game.ecs.components.EnemyStatsComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.ecs.components.VelocityComponent;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;

/**
 * Fait avancer la ruée du Dash (touche Tab, voir PlayerInputSystem) : impose
 * une vitesse élevée dans la direction visée pendant une courte durée — la
 * vraie intégration de position/collision reste déléguée à MovementSystem
 * (exécuté après celui-ci), donc un mur arrête bien le dash comme un
 * déplacement normal. Inflige des dégâts à chaque ennemi traversé, une seule
 * fois par ennemi et par ruée (DashComponent.hitEntities).
 */
public class DashSystem extends IteratingSystem {

    public static final float SPEED        = 340f;  // px/s pendant la ruée (très supérieur à la vitesse normale)
    public static final float DURATION     = 0.16f; // secondes de ruée active
    public static final float COOLDOWN     = 2.5f;
    public static final float STAMINA_COST = 25f;

    private static final float DAMAGE_MULT = 0.6f; // fraction de l'ATK du joueur, par ennemi traversé
    private static final float HIT_RADIUS  = 14f;

    private final ComponentMapper<DashComponent>        dm  = ComponentMapper.getFor(DashComponent.class);
    private final ComponentMapper<TransformComponent>   tm  = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<VelocityComponent>    vm  = ComponentMapper.getFor(VelocityComponent.class);
    private final ComponentMapper<PlayerStatsComponent>  sm  = ComponentMapper.getFor(PlayerStatsComponent.class);
    private final ComponentMapper<EnemyStatsComponent>   esm = ComponentMapper.getFor(EnemyStatsComponent.class);

    private final Family enemyFamily;

    public DashSystem(Family enemyFamily) {
        super(Family.all(DashComponent.class, TransformComponent.class, VelocityComponent.class).get(), 1);
        this.enemyFamily = enemyFamily;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        DashComponent dash = dm.get(entity);

        if (dash.timer > 0f) {
            dash.timer -= deltaTime;
        }

        if (!dash.dashing) return;

        VelocityComponent vel = vm.get(entity);
        vel.vx = dash.dirX * SPEED;
        vel.vy = dash.dirY * SPEED;

        dash.dashTimeLeft -= deltaTime;
        if (dash.dashTimeLeft <= 0f) {
            dash.dashing = false;
        }

        applyDashDamage(entity, dash);
    }

    private void applyDashDamage(Entity entity, DashComponent dash) {
        TransformComponent pos = tm.get(entity);
        PlayerStatsComponent stats = sm.get(entity);
        float damage = stats != null ? stats.atk() * DAMAGE_MULT : 0f;
        if (damage <= 0f) return;

        ImmutableArray<Entity> enemies = getEngine().getEntitiesFor(enemyFamily);
        for (Entity enemy : enemies) {
            EnemyStatsComponent enemyStats = esm.get(enemy);
            if (enemyStats.isDead || dash.hitEntities.contains(enemy)) continue;
            TransformComponent ePos = tm.get(enemy);
            float dx = ePos.x - pos.x;
            float dy = ePos.y - pos.y;
            if (dx * dx + dy * dy <= HIT_RADIUS * HIT_RADIUS) {
                DamageResolver.applyDamage(enemy, damage);
                dash.hitEntities.add(enemy);
            }
        }
    }
}
