package adfinir.game.ecs.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import adfinir.game.ecs.components.HealthComponent;
import adfinir.game.ecs.components.TransformComponent;

public class HealthBarRenderSystem {

    private final ShapeRenderer shapeRenderer;
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ComponentMapper<HealthComponent> healthMapper;
    private final Family family;
    private Engine engine;

    private static final float BAR_WIDTH = 16f;
    private static final float BAR_HEIGHT = 3f;
    private static final float Y_OFFSET = 10f;

    public HealthBarRenderSystem(ShapeRenderer shapeRenderer) {
        this.shapeRenderer = shapeRenderer;
        this.family = Family.all(TransformComponent.class, HealthComponent.class).get();
        this.transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.healthMapper = ComponentMapper.getFor(HealthComponent.class);
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public void update(float delta) {
        if (engine == null) return;
        ImmutableArray<Entity> entities = engine.getEntitiesFor(family);
        for (Entity entity : entities) {
            processEntity(entity);
        }
    }

    private void processEntity(Entity entity) {
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
