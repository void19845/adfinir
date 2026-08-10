package adfinir.game.combat;

import adfinir.game.ecs.components.EnemyStatsComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;

/**
 * Point d'entrée unique pour appliquer des dégâts à une entité, qu'elle soit
 * le joueur ou un ennemi. Réutilisé par CombatSystem (coups d'armes) et
 * CapacityBurst (compétences) sans coupler ces systèmes entre eux.
 */
public class DamageResolver {

    /** Applique des dégâts bruts à la cible (mitigés par sa DEF). Retourne les dégâts réels subis. */
    public static float applyDamage(Entity target, float rawDamage) {
        PlayerStatsComponent playerStats = target.getComponent(PlayerStatsComponent.class);
        if (playerStats != null) {
            float real = playerStats.takeDamage(rawDamage);
            Gdx.app.log("DamageResolver", "Joueur touché : " + real + " dégâts. HP: " + playerStats.currentHp);
            return real;
        }

        EnemyStatsComponent enemyStats = target.getComponent(EnemyStatsComponent.class);
        if (enemyStats != null) {
            float real = enemyStats.takeDamage(rawDamage);
            Gdx.app.log("DamageResolver", "Ennemi touché : " + real + " dégâts. HP: " + enemyStats.currentHp);
            return real;
        }

        return 0f;
    }
}
