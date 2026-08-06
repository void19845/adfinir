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
                case CONE:
                    // Épée : On utilise l'arc de ShapeRenderer pour créer un cône (wedge)
                    float angle = 60f; // Largeur du cône en degrés
                    float centralAngle = (float) Math.toDegrees(Math.atan2(dy, dx));
                    float startAngle = centralAngle - angle / 2f;

                    shapeRenderer.arc(
                        pos.x, pos.y,
                        weapon.range / 2f,
                        startAngle,
                        angle
                    );
                    break;
                case RECTANGLE:
                    // Lance : rectangle long et étroit orienté vers la direction d'attaque
                    float halfW = weapon.width / 2f;
                    float length = weapon.range;

                    if (Math.abs(dx) > Math.abs(dy)) {
                        shapeRenderer.rect(
                            pos.x + (dx > 0 ? 0 : -length),
                            pos.y - halfW,
                            length,
                            weapon.width
                        );
                    } else {
                        shapeRenderer.rect(
                            pos.x - halfW,
                            pos.y + (dy > 0 ? 0 : -length),
                            weapon.width,
                            length
                        );
                    }
                    break;
                case ARC:
                    // Hache : On simule un arc par un rectangle large et court
                    float arcWidth = weapon.range * 1.5f;
                    float arcDepth = weapon.range;

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
            }
        }
    }
}
