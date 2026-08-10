package adfinir.game.combat;

import adfinir.game.ecs.components.EnemyStatsComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.inventory.Capacity;
import adfinir.game.inventory.CapacityModifier;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector2;

/**
 * Résolution V1 (simplifiée) des compétences (Capacity) : pas de vrais projectiles
 * physiques (rebond, duplication...) — un burst de dégâts instantané en zone (AoE)
 * autour d'un point de cast, avec les CapacityModifier approximés en bonus numériques
 * (dégâts/rayon/cooldown). Une vraie simulation de projectiles est un chantier futur.
 */
public class CapacityBurst {

    private static final ComponentMapper<TransformComponent> transformMapper = ComponentMapper.getFor(TransformComponent.class);
    private static final ComponentMapper<EnemyStatsComponent> enemyStatsMapper = ComponentMapper.getFor(EnemyStatsComponent.class);

    public static class BurstParams {
        public float damage;
        public float radius;
        public float cooldown;
        public float staminaCost;
    }

    /** Calcule les paramètres du burst à partir de la capacité équipée et de ses modificateurs. */
    public static BurstParams resolve(Capacity cap) {
        BurstParams p = new BurstParams();
        p.damage = cap.mainEffect.baseDamage * cap.rarity.statMultiplier;
        p.radius = cap.mainEffect.radius;
        p.cooldown = 3.0f;
        p.staminaCost = 30f;

        for (CapacityModifier m : cap.modifiers) {
            switch (m.type) {
                case EXPLOSION:
                case BOUNCE:
                case RICOCHET:
                case ARC:
                    // Approximation V1 : pas de vraie physique de rebond/duplication, juste plus de portée
                    p.radius *= (1f + 0.2f * m.intensity);
                    break;
                case DUPLICATE:
                    // Approximation V1 : simule plusieurs impacts par un bonus de dégâts
                    p.damage *= (1f + 0.25f * m.intensity);
                    break;
                case SPEED_UP:
                    p.cooldown *= (1f - 0.15f * m.intensity);
                    break;
            }
        }
        p.cooldown = Math.max(0.5f, p.cooldown);
        return p;
    }

    /** Applique les dégâts du burst à tous les ennemis vivants dans le rayon autour du point de cast. */
    public static void applyBurst(Engine engine, Family enemyFamily, float centerX, float centerY,
                                   float damage, float radius) {
        ImmutableArray<Entity> enemies = engine.getEntitiesFor(enemyFamily);
        for (Entity enemy : enemies) {
            EnemyStatsComponent stats = enemyStatsMapper.get(enemy);
            if (stats.isDead) continue;
            TransformComponent t = transformMapper.get(enemy);
            float dist = Vector2.dst(centerX, centerY, t.x, t.y);
            if (dist <= radius + AttackGeometry.HIT_RADIUS) {
                DamageResolver.applyDamage(enemy, damage);
            }
        }
    }
}
