package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.CombatComponent;
import adfinir.game.ecs.components.EnemyStatsComponent;
import adfinir.game.ecs.components.RenderComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.inventory.Weapon;
import adfinir.game.inventory.WeaponSpriteManager;
import adfinir.game.player.AttackShape;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Dessine les entités sous forme de rectangles colorés.
 * À remplacer par des sprites quand les assets seront disponibles.
 */
public class RenderSystem extends IteratingSystem {

    private final ComponentMapper<TransformComponent> tm = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<RenderComponent>    rm = ComponentMapper.getFor(RenderComponent.class);
    private final ComponentMapper<CombatComponent>    cm = ComponentMapper.getFor(CombatComponent.class);
    private final ComponentMapper<EnemyStatsComponent> esm = ComponentMapper.getFor(EnemyStatsComponent.class);

    private final ShapeRenderer shapeRenderer;

    public RenderSystem(ShapeRenderer shapeRenderer) {
        super(Family.all(TransformComponent.class, RenderComponent.class).get(), 10);
        this.shapeRenderer = shapeRenderer;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        EnemyStatsComponent enemyStats = esm.get(entity);
        if (enemyStats != null && enemyStats.isDead) return;

        TransformComponent pos    = tm.get(entity);
        RenderComponent    render = rm.get(entity);

        shapeRenderer.setColor(render.color);
        // Centré sur pos.x / pos.y
        shapeRenderer.rect(
            pos.x - render.width  / 2f,
            pos.y - render.height / 2f,
            render.width,
            render.height
        );

        // Visualisation de l'attaque
        CombatComponent combat = cm.get(entity);
        if (combat != null && combat.isAttacking && combat.weapon != null) {
            Weapon weapon = combat.weapon;
            shapeRenderer.setColor(Color.RED);

            float dx = combat.attackDirX;
            float dy = combat.attackDirY;

            // La forme est portée par l'attaque active du combo (socketée), plus par le type d'arme
            java.util.List<adfinir.game.inventory.WeaponAttack> activeCombo = weapon.getActiveCombo();
            if (combat.comboIndex >= activeCombo.size()) return; // combo raccourci en cours de swing
            adfinir.game.inventory.WeaponAttack currentAttack = activeCombo.get(combat.comboIndex);
            AttackShape shape = currentAttack.shape;
            float range = currentAttack.areaOfEffect * weapon.type.rangeMod;

            switch (shape) {
                case CONE:
                    // Épée : On utilise l'arc de ShapeRenderer pour créer un cône (wedge)
                    float angle = 60f; // Largeur du cône en degrés
                    float centralAngle = (float) Math.toDegrees(Math.atan2(dy, dx));
                    float startAngle = centralAngle - angle / 2f;

                    shapeRenderer.arc(
                        pos.x, pos.y,
                        range / 2f,
                        startAngle,
                        angle
                    );
                    break;
                case RECTANGLE:
                    // Lance : rectangle long et étroit orienté vers la direction d'attaque
                    float halfW = 15f;
                    float length = range;

                    if (Math.abs(dx) > Math.abs(dy)) {
                        shapeRenderer.rect(
                            pos.x + (dx > 0 ? 0 : -length),
                            pos.y - halfW,
                            length,
                            halfW * 2f
                        );
                    } else {
                        shapeRenderer.rect(
                            pos.x - halfW,
                            pos.y + (dy > 0 ? 0 : -length),
                            halfW * 2f,
                            length
                        );
                    }
                    break;
                case ARC:
                    // Hache : On simule un arc par un rectangle large et court
                    float arcWidth = range * 1.5f;
                    float arcDepth = range;

                    if (Math.abs(dx) > Math.abs(dy)) {
                        shapeRenderer.rect(
                            pos.x + (dx > 0 ? 0 : -arcWidth),
                            pos.y - arcDepth / 2f,
                            arcWidth,
                            arcDepth
                        );
                    } else {
                        shapeRenderer.rect(
                            pos.x - arcWidth / 2f,
                            pos.y + (dy > 0 ? 0 : -arcDepth),
                            arcWidth,
                            arcDepth
                        );
                    }
                    break;
                case CIRCLE:
                    // Disque plein centré sur le joueur
                    shapeRenderer.circle(pos.x, pos.y, range, 24);
                    break;
                case FIXED_DISTANCE:
                    // Hitbox statique à distance fixe devant le joueur (areaOfEffect = distance)
                    shapeRenderer.circle(pos.x + dx * range, pos.y + dy * range,
                        MeleeHitSystem.FIXED_DISTANCE_HIT_RADIUS, 16);
                    break;
                case PROJECTILE:
                    // Rien à dessiner ici : l'entité projectile se rend elle-même (RenderComponent)
                    break;
            }
        }
    }
}
