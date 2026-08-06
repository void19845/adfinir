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

        // Gestion de la durée de l'attaque et détection de collision
        if (combat.isAttacking) {
            if (combat.timer < weapon.cooldown - weapon.duration) {
                combat.isAttacking = false;
            } else if (!combat.hasHit) {
                // Tentative de frapper d'autres entités
                checkHit(entity, combat, weapon);
            }
        }
    }

    private void checkHit(Entity attacker, CombatComponent combat, Weapon weapon) {
        TransformComponent attackerPos = transformMapper.get(attacker);

        // On parcourt toutes les entités du moteur pour voir lesquelles sont touchées
        for (Entity target : getEngine().getEntities()) {
            if (target == attacker) continue;

            TransformComponent targetPos = transformMapper.get(target);
            if (targetPos == null) continue;

            // Calcul de la distance
            float dx = targetPos.x - attackerPos.x;
            float dy = targetPos.y - attackerPos.y;
            float distSq = dx * dx + dy * dy;

            // Pour simplifier, on utilise un cercle basé sur le range de l'arme
            // On vérifie aussi si la cible est globalement dans la direction de l'attaque
            float range = weapon.range;
            if (distSq <= range * range) {
                // Vérification directionnelle simplifiée (produit scalaire)
                float dot = dx * combat.attackDirX + dy * combat.attackDirY;
                if (dot > 0) { // La cible est devant l'attaquant
                    applyDamage(attacker, target, weapon.damage);
                    combat.hasHit = true;
                    break; // On ne frappe qu'une cible par attaque pour l'instant
                }
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
