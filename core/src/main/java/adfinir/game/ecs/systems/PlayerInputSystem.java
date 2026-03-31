package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.PlayerInputComponent;
import adfinir.game.ecs.components.VelocityComponent;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

public class PlayerInputSystem extends IteratingSystem {

    private final ComponentMapper<VelocityComponent>    vm = ComponentMapper.getFor(VelocityComponent.class);
    private final ComponentMapper<PlayerInputComponent> pm = ComponentMapper.getFor(PlayerInputComponent.class);

    public PlayerInputSystem() {
        super(Family.all(PlayerInputComponent.class, VelocityComponent.class).get(), 1);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        VelocityComponent    vel   = vm.get(entity);
        PlayerInputComponent input = pm.get(entity);

        float dx = 0f, dy = 0f;

        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))    dy += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))  dy -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))  dx -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dx += 1f;

        // Normaliser le vecteur diagonal pour éviter un mouvement plus rapide en diagonale
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 0f) { dx /= len; dy /= len; }

        vel.vx = dx * input.speed;
        vel.vy = dy * input.speed;
    }
}
