package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.CombatComponent;
import adfinir.game.ecs.components.EnemyStatsComponent;
import adfinir.game.ecs.components.ProjectileComponent;
import adfinir.game.ecs.components.RenderComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.ecs.components.VelocityComponent;
import adfinir.game.inventory.WeaponAttack;
import adfinir.game.player.AttackShape;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;

import java.util.List;

/**
 * Détecte les coups de mêlée pendant la fenêtre active d'une attaque
 * (CombatComponent.isAttacking) et applique les dégâts une seule fois par
 * swing (CombatComponent.hasHit). Géométrie testée en miroir de ce que
 * RenderSystem dessine (même formules), mais indépendante : le rendu
 * existant n'est pas touché. Une attaque de forme PROJECTILE fait apparaître
 * un projectile (ProjectileSystem) au lieu de tester une géométrie
 * instantanée.
 *
 * Note : conserve la même convention d'indexation que CombatSystem/RenderSystem
 * (combat.comboIndex déjà incrémenté par triggerAttack au moment du test) —
 * comportement préexistant, non modifié ici.
 */
public class MeleeHitSystem extends IteratingSystem {

    private static final float CONE_ANGLE_DEG = 60f;
    /** Package-visible : réutilisée par RenderSystem pour dessiner exactement la même hitbox. */
    static final float FIXED_DISTANCE_HIT_RADIUS = 10f;
    private static final float ENEMY_POINT_TOLERANCE = 6f;

    private final ComponentMapper<CombatComponent>     cm  = ComponentMapper.getFor(CombatComponent.class);
    private final ComponentMapper<TransformComponent>  tm  = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<EnemyStatsComponent> esm = ComponentMapper.getFor(EnemyStatsComponent.class);

    public MeleeHitSystem() {
        super(Family.all(CombatComponent.class, TransformComponent.class).get(), 3);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        CombatComponent combat = cm.get(entity);
        if (combat.weapon == null || !combat.isAttacking || combat.hasHit) return;

        List<WeaponAttack> activeCombo = combat.weapon.getActiveCombo();
        if (combat.comboIndex < 0 || combat.comboIndex >= activeCombo.size()) return;
        WeaponAttack attack = activeCombo.get(combat.comboIndex);

        TransformComponent pos = tm.get(entity);
        float range = attack.areaOfEffect * combat.weapon.type.rangeMod;
        float damage = combat.weapon.getModifiedDamage(combat.comboIndex);

        if (attack.shape == AttackShape.PROJECTILE) {
            spawnWeaponProjectile(entity, pos, combat, attack, damage);
            combat.hasHit = true; // la responsabilité du hit passe au projectile
            return;
        }

        boolean hitSomeone = false;
        for (Entity enemy : getEngine().getEntitiesFor(Family.all(EnemyStatsComponent.class, TransformComponent.class).get())) {
            EnemyStatsComponent stats = esm.get(enemy);
            if (stats.isDead) continue;
            TransformComponent ePos = tm.get(enemy);
            if (isInside(attack.shape, pos, combat.attackDirX, combat.attackDirY, range, ePos.x, ePos.y)) {
                getEngine().getSystem(CombatSystem.class).applyDamage(entity, enemy, damage);
                hitSomeone = true;
            }
        }
        if (hitSomeone) combat.hasHit = true;
    }

    private boolean isInside(AttackShape shape, TransformComponent origin, float dirX, float dirY,
                             float range, float px, float py) {
        float dx = px - origin.x;
        float dy = py - origin.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        switch (shape) {
            case CONE: {
                if (dist > range / 2f) return false;
                if (dist < 0.001f) return true; // au contact, angle indéfini -> touché
                float angleTo  = (float) Math.toDegrees(Math.atan2(dy, dx));
                float angleDir = (float) Math.toDegrees(Math.atan2(dirY, dirX));
                return Math.abs(normalizeAngle(angleTo - angleDir)) <= CONE_ANGLE_DEG / 2f;
            }
            case CIRCLE:
                return dist <= range + ENEMY_POINT_TOLERANCE;
            case FIXED_DISTANCE: {
                float cx = origin.x + dirX * range;
                float cy = origin.y + dirY * range;
                float ddx = px - cx;
                float ddy = py - cy;
                return Math.sqrt(ddx * ddx + ddy * ddy) <= FIXED_DISTANCE_HIT_RADIUS;
            }
            case RECTANGLE:
                return inLongRect(origin, dirX, dirY, range, 15f, px, py);
            case ARC:
                // Miroir exact de RenderSystem : arcWidth (=range*1.5) en extension le long de
                // l'axe dominant, arcDepth (=range) en largeur perpendiculaire.
                return inLongRect(origin, dirX, dirY, range * 1.5f, range / 2f, px, py);
            default:
                return false;
        }
    }

    /** Rectangle orienté selon l'axe dominant de dirX/dirY — miroir exact de RenderSystem. */
    private boolean inLongRect(TransformComponent origin, float dirX, float dirY,
                               float length, float halfWidth, float px, float py) {
        if (Math.abs(dirX) > Math.abs(dirY)) {
            float minX = origin.x + (dirX > 0 ? 0 : -length);
            float maxX = origin.x + (dirX > 0 ? length : 0);
            return px >= minX && px <= maxX && py >= origin.y - halfWidth && py <= origin.y + halfWidth;
        } else {
            float minY = origin.y + (dirY > 0 ? 0 : -length);
            float maxY = origin.y + (dirY > 0 ? length : 0);
            return py >= minY && py <= maxY && px >= origin.x - halfWidth && px <= origin.x + halfWidth;
        }
    }

    private float normalizeAngle(float angle) {
        while (angle > 180f)  angle -= 360f;
        while (angle < -180f) angle += 360f;
        return angle;
    }

    /** Attaque d'arme de forme PROJECTILE : même ProjectileSystem que la capacité, sans modifiers (arme "brute"). */
    private void spawnWeaponProjectile(Entity owner, TransformComponent pos, CombatComponent combat,
                                       WeaponAttack attack, float damage) {
        Entity projectile = new Entity();

        TransformComponent transform = new TransformComponent();
        transform.x = pos.x;
        transform.y = pos.y;

        VelocityComponent vel = new VelocityComponent();
        vel.vx = combat.attackDirX * attack.projectileSpeed;
        vel.vy = combat.attackDirY * attack.projectileSpeed;

        RenderComponent render = new RenderComponent();
        render.width  = attack.areaOfEffect;
        render.height = attack.areaOfEffect;

        ProjectileComponent proj = new ProjectileComponent();
        proj.owner = owner;
        proj.damage = damage;
        proj.hitRadius = attack.areaOfEffect;

        projectile.add(transform);
        projectile.add(vel);
        projectile.add(render);
        projectile.add(proj);

        getEngine().addEntity(projectile);
    }
}
