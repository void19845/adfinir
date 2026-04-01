package adfinir.game.ecs.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import adfinir.game.ecs.components.HealthComponent;
import adfinir.game.ecs.components.TransformComponent;

public class HealthBarRenderSystem extends IteratingSystem {

    private final ShapeRenderer shapeRenderer;
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ComponentMapper<HealthComponent> healthMapper;

    private static final float BAR_WIDTH = 16f;
    private static final float BAR_HEIGHT = 3f;
    private static final float Y_OFFSET = 10f; 

    public HealthBarRenderSystem(ShapeRenderer shapeRenderer) {
        super(Family.all(TransformComponent.class, HealthComponent.class).get());
        
        this.shapeRenderer = shapeRenderer;
        this.transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.healthMapper = ComponentMapper.getFor(HealthComponent.class);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        HealthComponent health = healthMapper.get(entity);

        if (transform == null || health == null) return;

        float healthPercentage = health.currentHealth / health.maxHealth;

        float barX = transform.x - (BAR_WIDTH / 2);
        float barY = transform.y + Y_OFFSET;

        shapeRenderer.setColor(Color.FIREBRICK);
        shapeRenderer.rect(barX, barY, BAR_WIDTH, BAR_HEIGHT);

        if (healthPercentage > 0) {
            shapeRenderer.setColor(Color.FOREST);
            shapeRenderer.rect(barX, barY, BAR_WIDTH * healthPercentage, BAR_HEIGHT);
        }
    }
}
