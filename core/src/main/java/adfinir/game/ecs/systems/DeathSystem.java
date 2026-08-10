package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.EnemyStatsComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;

/**
 * Système qui retire les entités marquées comme mortes du moteur et verse
 * de l'or au joueur pour chaque ennemi vaincu.
 */
public class DeathSystem extends IteratingSystem {
    private final ComponentMapper<EnemyStatsComponent> enemyStatsMapper = ComponentMapper.getFor(EnemyStatsComponent.class);
    private final ComponentMapper<PlayerStatsComponent> playerStatsMapper = ComponentMapper.getFor(PlayerStatsComponent.class);

    private final Entity player;

    public DeathSystem(Entity player) {
        super(Family.all(EnemyStatsComponent.class).get(), 100); // Tourne à la fin
        this.player = player;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        EnemyStatsComponent stats = enemyStatsMapper.get(entity);
        if (stats != null && stats.isDead) {
            awardGold(stats);
            // On ne peut pas supprimer une entité directement pendant l'itération d'un IteratingSystem
            // sans risque, mais Ashley gère assez bien engine.removeEntity().
            // Cependant, la pratique recommandée est de marquer pour suppression ou d'utiliser
            // une liste temporaire si on rencontre des problèmes.
            getEngine().removeEntity(entity);
        }
    }

    private void awardGold(EnemyStatsComponent stats) {
        if (player == null) return;
        PlayerStatsComponent playerStats = playerStatsMapper.get(player);
        if (playerStats == null) return;
        int reward = Math.round(stats.maxHp * 0.5f);
        playerStats.addGold(reward);
    }
}
