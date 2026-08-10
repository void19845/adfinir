package adfinir.game.ecs.systems;

import adfinir.game.combat.AttackGeometry;
import adfinir.game.combat.DamageResolver;
import adfinir.game.ecs.components.CombatComponent;
import adfinir.game.ecs.components.EnemyStatsComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.inventory.Weapon;
import adfinir.game.inventory.WeaponAttack;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;

public class CombatSystem extends IteratingSystem {
    private final ComponentMapper<CombatComponent> combatMapper = ComponentMapper.getFor(CombatComponent.class);
    private final ComponentMapper<TransformComponent> transformMapper = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<PlayerStatsComponent> statsMapper = ComponentMapper.getFor(PlayerStatsComponent.class);
    private final ComponentMapper<EnemyStatsComponent> enemyStatsMapper = ComponentMapper.getFor(EnemyStatsComponent.class);

    private final Entity player;
    private final Family enemyFamily;

    public CombatSystem(Entity player, Family enemyFamily) {
        super(Family.all(CombatComponent.class, TransformComponent.class).get(), 10);
        this.player = player;
        this.enemyFamily = enemyFamily;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        CombatComponent combat = combatMapper.get(entity);
        Weapon weapon = combat.weapon;

        // Décompte des cooldowns (arme + compétence)
        if (combat.timer > 0) {
            combat.timer -= deltaTime;
        }
        for (int i = 0; i < combat.spellTimers.length; i++) {
            if (combat.spellTimers[i] > 0) {
                combat.spellTimers[i] -= deltaTime;
            }
        }
        if (combat.capacityBursting) {
            combat.capacityBurstTimer -= deltaTime;
            if (combat.capacityBurstTimer <= 0f) {
                combat.capacityBursting = false;
            }
        }

        if (weapon == null || !combat.isAttacking) return;

        WeaponAttack currentAttack = weapon.comboSlots.get(combat.activeComboIndex);
        boolean windowActive = combat.timer >= currentAttack.cooldown - currentAttack.duration;

        if (windowActive) {
            resolveHit(entity, combat, weapon, currentAttack);
        } else {
            combat.isAttacking = false;
        }
    }

    /** Teste et applique les dégâts de la fenêtre active courante contre les cibles adverses. */
    private void resolveHit(Entity attacker, CombatComponent combat, Weapon weapon, WeaponAttack currentAttack) {
        TransformComponent origin = transformMapper.get(attacker);
        float range = AttackGeometry.computeRange(weapon, combat.activeComboIndex);

        boolean attackerIsPlayer = statsMapper.has(attacker);

        if (attackerIsPlayer) {
            PlayerStatsComponent attackerStats = statsMapper.get(attacker);
            ImmutableArray<Entity> enemies = getEngine().getEntitiesFor(enemyFamily);
            for (Entity target : enemies) {
                EnemyStatsComponent targetStats = enemyStatsMapper.get(target);
                if (targetStats.isDead || combat.hitEntities.contains(target)) continue;
                if (hits(origin, combat, weapon, range, target)) {
                    float damage = weapon.getModifiedDamage(combat.activeComboIndex, attackerStats.atk(), attackerStats.mag());
                    DamageResolver.applyDamage(target, damage);
                    combat.hitEntities.add(target);
                }
            }
        } else {
            if (player == null || combat.hitEntities.contains(player)) return;
            PlayerStatsComponent targetStats = statsMapper.get(player);
            if (targetStats == null || targetStats.isDead) return;
            EnemyStatsComponent attackerStats = enemyStatsMapper.get(attacker);
            if (hits(origin, combat, weapon, range, player)) {
                float damage = weapon.getModifiedDamage(combat.activeComboIndex, attackerStats.atk, 0f);
                DamageResolver.applyDamage(player, damage);
                combat.hitEntities.add(player);
            }
        }
    }

    private boolean hits(TransformComponent origin, CombatComponent combat, Weapon weapon, float range, Entity target) {
        TransformComponent targetPos = transformMapper.get(target);
        return AttackGeometry.overlaps(weapon.type.shape, origin.x, origin.y, combat.attackDirX, combat.attackDirY,
            range, targetPos.x, targetPos.y, AttackGeometry.HIT_RADIUS);
    }
}
