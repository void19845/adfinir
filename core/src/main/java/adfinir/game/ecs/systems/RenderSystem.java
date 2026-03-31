package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.RenderComponent;
import adfinir.game.ecs.components.TransformComponent;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Dessine les entités sous forme de rectangles colorés.
 * À remplacer par des sprites quand les assets seront disponibles.
 */
public class RenderSystem extends IteratingSystem {

    private final ComponentMapper<TransformComponent> tm = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<RenderComponent>    rm = ComponentMapper.getFor(RenderComponent.class);

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
    }
}
