package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.EnemyStatsComponent;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import java.util.ArrayList;
import java.util.List;

/**
 * Système qui retire les entités marquées comme mortes du moteur.
 */
public class DeathSystem extends IteratingSystem {
    private final ComponentMapper<EnemyStatsComponent> enemyStatsMapper = ComponentMapper.getFor(EnemyStatsComponent.class);

    public DeathSystem() {
        super(Family.all(EnemyStatsComponent.class).get(), 100); // Tourne à la fin
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        EnemyStatsComponent stats = enemyStatsMapper.get(entity);
        if (stats != null && stats.isDead) {
            // On ne peut pas supprimer une entité directement pendant l'itération d'un IteratingSystem
            // sans risque, mais Ashley gère assez bien engine.removeEntity().
            // Cependant, la pratique recommandée est de marquer pour suppression ou d'utiliser
            // une liste temporaire si on rencontre des problèmes.
            getEngine().removeEntity(entity);
        }
    }
}
