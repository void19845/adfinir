package adfinir.game.ecs.systems;

import adfinir.game.combat.AttackGeometry;
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
import com.badlogic.gdx.math.Rectangle;

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

        // Visualisation de l'attaque (même géométrie que la détection de coups dans CombatSystem)
        CombatComponent combat = cm.get(entity);
        if (combat != null && combat.isAttacking && combat.weapon != null) {
            Weapon weapon = combat.weapon;
            shapeRenderer.setColor(Color.RED);

            float dx = combat.attackDirX;
            float dy = combat.attackDirY;
            AttackShape shape = weapon.type.shape;
            float range = AttackGeometry.computeRange(weapon, combat.activeComboIndex);

            if (shape == AttackShape.CONE) {
                float centralAngle = AttackGeometry.coneCentralAngleDeg(dx, dy);
                float startAngle = centralAngle - AttackGeometry.CONE_ANGLE_DEG / 2f;
                shapeRenderer.arc(pos.x, pos.y, range / 2f, startAngle, AttackGeometry.CONE_ANGLE_DEG);
            } else {
                Rectangle r = AttackGeometry.computeAxisAlignedRect(shape, pos.x, pos.y, dx, dy, range);
                shapeRenderer.rect(r.x, r.y, r.width, r.height);
            }
        }

        // Flash visuel du burst de compétence
        if (combat != null && combat.capacityBursting) {
            shapeRenderer.setColor(Color.CYAN);
            float cx = pos.x + combat.attackDirX * 10f;
            float cy = pos.y + combat.attackDirY * 10f;
            shapeRenderer.circle(cx, cy, 8f);
        }
    }
}
