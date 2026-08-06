package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.CombatComponent;
import adfinir.game.ecs.components.RenderComponent;
import adfinir.game.ecs.components.TransformComponent;
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
            // On dessine un rectangle devant le joueur.
            // Note: Pour l'instant on dessine un carré centré,
            // mais on pourrait le décaler selon la direction du mouvement.
            shapeRenderer.rect(
                pos.x - weapon.range / 2f,
                pos.y - weapon.range / 2f,
                weapon.range,
                weapon.range
            );
        }
    }
}
