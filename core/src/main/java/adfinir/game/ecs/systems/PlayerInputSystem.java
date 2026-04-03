package adfinir.game.ecs.systems;

import adfinir.game.GameState;
import adfinir.game.ecs.components.CombatComponent;
import adfinir.game.ecs.components.PlayerInputComponent;
import adfinir.game.ecs.components.TransformComponent;
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
    private final ComponentMapper<TransformComponent>   tm = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<CombatComponent>      cm = ComponentMapper.getFor(CombatComponent.class);

    public PlayerInputSystem() {
        super(Family.all(PlayerInputComponent.class, VelocityComponent.class, TransformComponent.class).get(), 1);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        VelocityComponent    vel   = vm.get(entity);
        PlayerInputComponent input = pm.get(entity);
        TransformComponent   pos   = tm.get(entity);

        // Si l'inventaire est ouvert : arrêt complet du mouvement
        if (GameState.inventoryOpen) {
            vel.vx = vel.vy = 0;
            return;
        }

        // ── Mouvement (flèches + ZQSD + WASD) ────────────────────────────
        boolean up    = Gdx.input.isKeyPressed(input.upKey)
                     || Gdx.input.isKeyPressed(Input.Keys.Z)
                     || Gdx.input.isKeyPressed(Input.Keys.W);
        boolean down  = Gdx.input.isKeyPressed(input.downKey)
                     || Gdx.input.isKeyPressed(Input.Keys.S);
        boolean left  = Gdx.input.isKeyPressed(input.leftKey)
                     || Gdx.input.isKeyPressed(Input.Keys.Q)
                     || Gdx.input.isKeyPressed(Input.Keys.A);
        boolean right = Gdx.input.isKeyPressed(input.rightKey)
                     || Gdx.input.isKeyPressed(Input.Keys.D);

        float dx = 0f, dy = 0f;
        if (up)    dy += 1f;
        if (down)  dy -= 1f;
        if (left)  dx -= 1f;
        if (right) dx += 1f;

        // Normalisation diagonale
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 0f) {
            dx /= len; dy /= len;
            pos.facingX = dx; pos.facingY = dy;
        }
        vel.vx = dx * input.speed;
        vel.vy = dy * input.speed;

        // ── Attaques ──────────────────────────────────────────────────────
        CombatComponent combat = cm.get(entity);
        if (combat != null) {
            // Attaque rapide : Espace ou F
            if (Gdx.input.isKeyJustPressed(input.attackKey)
             || Gdx.input.isKeyJustPressed(Input.Keys.F))
                combat.wantsToAttack = true;

            // Attaque puissante : R
            if (Gdx.input.isKeyJustPressed(Input.Keys.R))
                combat.wantsPowerAttack = true;
        }
    }
}
