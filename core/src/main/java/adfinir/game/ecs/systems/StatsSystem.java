package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.PlayerInputComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;

/**
 * Système Ashley qui tourne chaque frame pour :
 *  1. Régénérer la stamina automatiquement.
 *  2. Synchroniser la vitesse de déplacement (SPD) depuis les stats vers PlayerInputComponent.
 *     → Ainsi, équiper des bottes de vitesse met à jour le mouvement immédiatement.
 */
public class StatsSystem extends IteratingSystem {

    private final ComponentMapper<PlayerStatsComponent> sm =
        ComponentMapper.getFor(PlayerStatsComponent.class);
    private final ComponentMapper<PlayerInputComponent> pm =
        ComponentMapper.getFor(PlayerInputComponent.class);

    public StatsSystem() {
        // Priorité 0 : tourne avant PlayerInputSystem (priorité 1)
        super(Family.all(PlayerStatsComponent.class, PlayerInputComponent.class).get(), 0);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        PlayerStatsComponent stats = sm.get(entity);
        PlayerInputComponent input = pm.get(entity);

        // 1. Régénération stamina
        stats.updateStamina(deltaTime);

        // 2. Synchronise la vitesse de mouvement avec la stat SPD (base + bonus items)
        input.speed = stats.spd();
    }
}
