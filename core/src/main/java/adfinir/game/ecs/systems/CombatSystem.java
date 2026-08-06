package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.CombatComponent;
import adfinir.game.ecs.components.EnemyStatsComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.player.Weapon;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;

public class CombatSystem extends IteratingSystem {
    private final ComponentMapper<CombatComponent> combatMapper = ComponentMapper.getFor(CombatComponent.class);
    private final ComponentMapper<TransformComponent> transformMapper = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<PlayerStatsComponent> statsMapper = ComponentMapper.getFor(PlayerStatsComponent.class);
    private final ComponentMapper<EnemyStatsComponent> enemyStatsMapper = ComponentMapper.getFor(EnemyStatsComponent.class);

    public CombatSystem() {
        super(Family.all(CombatComponent.class, TransformComponent.class).get(), 10);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        CombatComponent combat = combatMapper.get(entity);
        Weapon weapon = combat.weapon;

        if (weapon == null) return;

        // Mise à jour du cooldown
        if (combat.timer > 0) {
            combat.timer -= deltaTime;
        }

        // Gestion de la durée de l'attaque (hitbox active)
        if (combat.isAttacking) {
            // L'attaque est active tant que le timer est proche du cooldown max
            // On considère l'attaque active pendant la durée spécifiée par l'arme
            if (combat.timer < weapon.cooldown - weapon.duration) {
                combat.isAttacking = false;
            }
        }
    }

    public void applyDamage(Entity attacker, Entity target, float damage) {
        PlayerStatsComponent targetStats = statsMapper.get(target);
        if (targetStats != null) {
            float realDamage = targetStats.takeDamage(damage);
            Gdx.app.log("CombatSystem", "Target " + target + " took " + realDamage + " damage. HP: " + targetStats.currentHp);
            return;
        }

        EnemyStatsComponent enemyStats = enemyStatsMapper.get(target);
        if (enemyStats != null) {
            float realDamage = enemyStats.takeDamage(damage);
            Gdx.app.log("CombatSystem", "Enemy " + target + " took " + realDamage + " damage. HP: " + enemyStats.currentHp);
        }
    }
}
