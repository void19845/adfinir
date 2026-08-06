package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.CombatComponent;
import adfinir.game.ecs.components.RenderComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.player.AttackShape;
import adfinir.game.player.Weapon;
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

    private final ShapeRenderer shapeRenderer;

    public RenderSystem(ShapeRenderer shapeRenderer) {
        super(Family.all(TransformComponent.class, RenderComponent.class).get(), 10);
        this.shapeRenderer = shapeRenderer;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
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

            switch (weapon.shape) {
                case SQUARE:
                    shapeRenderer.rect(
                        pos.x - weapon.range / 2f,
                        pos.y - weapon.range / 2f,
                        weapon.range,
                        weapon.range
                    );
                    break;
                case RECTANGLE:
                    // Lance : rectangle long et étroit orienté vers la direction d'attaque
                    float halfW = weapon.width / 2f;
                    float halfL = weapon.range / 2f;

                    // On centre le rectangle sur le joueur mais on le décale légèrement vers l'avant
                    float centerX = pos.x + dx * (weapon.range / 2f);
                    float centerY = pos.y + dy * (weapon.range / 2f);

                    if (Math.abs(dx) > Math.abs(dy)) {
                        shapeRenderer.rect(
                            centerX - (dx > 0 ? weapon.range/2f : 0),
                            centerY - halfW,
                            weapon.range,
                            weapon.width
                        );
                    } else {
                        shapeRenderer.rect(
                            centerX - halfW,
                            centerY - (dy > 0 ? weapon.range/2f : 0),
                            weapon.width,
                            weapon.range
                        );
                    }
                    break;
                case ARC:
                    // Hache : cercle pour représenter la portée de l'arc
                    shapeRenderer.circle(pos.x, pos.y, weapon.range / 2f);
                    break;
            }
        }
    }
}
