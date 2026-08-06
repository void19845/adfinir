package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.CombatComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import adfinir.game.ecs.components.TransformComponent;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;

public class CombatSystem extends IteratingSystem {
    private final ComponentMapper<CombatComponent> combatMapper = ComponentMapper.getFor(CombatComponent.class);
    private final ComponentMapper<TransformComponent> transformMapper = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<PlayerStatsComponent> statsMapper = ComponentMapper.getFor(PlayerStatsComponent.class);

    public CombatSystem() {
        // On s'intéresse aux entités qui peuvent combattre
        super(Family.all(CombatComponent.class, TransformComponent.class).get(), 10);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        CombatComponent combat = combatMapper.get(entity);

        // Mise à jour du cooldown
        if (combat.timer > 0) {
            combat.timer -= deltaTime;
        }

        // Gestion de l'état d'attaque
        if (combat.isAttacking) {
            // On réduit la durée de l'attaque
            // Note: En production, on utiliserait un timer dédié pour la durée de la hitbox
            // Ici on simplifie : l'attaque est active pendant un court instant.

            // On pourrait ici déclencher la détection de collision
            // Pour l'instant, on log juste l'attaque car il n'y a pas d'ennemis
            if (combat.timer >= combat.attackCooldown - 0.1f) {
                Gdx.app.log("CombatSystem", "Entity " + entity + " performs a melee attack!");
            }

            // L'attaque s'arrête après un certain temps (simplification)
            if (combat.timer < combat.attackCooldown - 0.2f) {
                combat.isAttacking = false;
            }
        }
    }

    /**
     * Applique des dégâts à une cible.
     * Cette méthode sera appelée par le CombatSystem lors d'une collision d'attaque.
     */
    public void applyDamage(Entity attacker, Entity target, float damage) {
        PlayerStatsComponent targetStats = statsMapper.get(target);
        if (targetStats != null) {
            float realDamage = targetStats.takeDamage(damage);
            Gdx.app.log("CombatSystem", "Target " + target + " took " + realDamage + " damage. HP: " + targetStats.currentHp);
        }
    }
}
