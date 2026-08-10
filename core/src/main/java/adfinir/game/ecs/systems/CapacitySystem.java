package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.CapacityComponent;
import adfinir.game.ecs.components.InventoryComponent;
import adfinir.game.ecs.components.ProjectileComponent;
import adfinir.game.ecs.components.RenderComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.ecs.components.VelocityComponent;
import adfinir.game.inventory.Capacity;
import adfinir.game.inventory.CapacityEffect;
import adfinir.game.inventory.CapacityModifier;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Consomme le cooldown de CapacityComponent et, quand une capacité est
 * déclenchée (clic droit via PlayerInputSystem), fait apparaître le/les
 * projectile(s) correspondant à l'effet actif + modifiers actifs de la
 * Capacity équipée (getActiveEffect()/getActiveModifiers(), calculés à la
 * volée depuis les sockets — aucune duplication d'état ici).
 */
public class CapacitySystem extends IteratingSystem {

    private static final float DUPLICATE_SPREAD_DEG = 15f;

    private final ComponentMapper<CapacityComponent>  cm = ComponentMapper.getFor(CapacityComponent.class);
    private final ComponentMapper<InventoryComponent> im = ComponentMapper.getFor(InventoryComponent.class);
    private final ComponentMapper<TransformComponent> tm = ComponentMapper.getFor(TransformComponent.class);

    public CapacitySystem() {
        super(Family.all(CapacityComponent.class, TransformComponent.class, InventoryComponent.class).get(), 4);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        CapacityComponent cap = cm.get(entity);

        if (cap.timer > 0f) {
            cap.timer -= deltaTime;
        }

        if (!cap.triggered) return;
        cap.triggered = false;

        InventoryComponent inv = im.get(entity);
        Capacity capacity = inv.capacity;
        if (capacity == null) return;

        TransformComponent pos = tm.get(entity);
        cast(entity, capacity, pos.x, pos.y, cap.dirX, cap.dirY);
    }

    private void cast(Entity owner, Capacity capacity, float x, float y, float dirX, float dirY) {
        CapacityEffect effect = capacity.getActiveEffect();
        List<CapacityModifier> mods = capacity.getActiveModifiers();

        int duplicateCount = 1;
        for (CapacityModifier m : mods) {
            if (m.type == CapacityModifier.ModType.DUPLICATE) {
                duplicateCount += Math.round(m.intensity);
            }
        }

        float speed = effect.speed;
        for (CapacityModifier m : mods) {
            if (m.type == CapacityModifier.ModType.SPEED_UP) {
                speed *= (1f + m.intensity);
            }
        }

        // Éventail centré sur la direction visée quand plusieurs projectiles sont dupliqués
        float totalSpread = DUPLICATE_SPREAD_DEG * (duplicateCount - 1);
        float startAngle = -totalSpread / 2f;
        float baseAngle = (float) Math.toDegrees(Math.atan2(dirY, dirX));

        for (int i = 0; i < duplicateCount; i++) {
            float angle = (float) Math.toRadians(baseAngle + startAngle + i * DUPLICATE_SPREAD_DEG);
            float dx = (float) Math.cos(angle);
            float dy = (float) Math.sin(angle);
            spawnProjectile(owner, effect, mods, x, y, dx, dy, speed);
        }
    }

    private void spawnProjectile(Entity owner, CapacityEffect effect, List<CapacityModifier> mods,
                                 float x, float y, float dirX, float dirY, float speed) {
        Entity projectile = new Entity();

        TransformComponent transform = new TransformComponent();
        transform.x = x;
        transform.y = y;
        transform.rotation = (float) Math.toDegrees(Math.atan2(dirY, dirX));

        VelocityComponent vel = new VelocityComponent();
        vel.vx = dirX * speed;
        vel.vy = dirY * speed;

        RenderComponent render = new RenderComponent();
        render.width = effect.radius;
        render.height = effect.radius;
        render.color = colorFor(effect.type);

        ProjectileComponent proj = new ProjectileComponent();
        proj.owner = owner;
        proj.damage = effect.baseDamage;
        proj.hitRadius = effect.radius;
        proj.modifiers = new ArrayList<>(mods);

        for (CapacityModifier m : mods) {
            switch (m.type) {
                case BOUNCE:
                    proj.bouncesLeft = Math.max(1, Math.round(m.intensity));
                    break;
                case RICOCHET:
                    proj.ricochetsLeft = Math.max(1, Math.round(m.intensity));
                    break;
                case EXPLOSION:
                    proj.explosionRadius = effect.radius * (1f + m.intensity);
                    break;
                case ARC:
                    proj.arcDegreesPerSecond = 90f * m.intensity;
                    break;
                default:
                    break; // DUPLICATE / SPEED_UP déjà résolus avant le spawn
            }
        }

        projectile.add(transform);
        projectile.add(vel);
        projectile.add(render);
        projectile.add(proj);

        getEngine().addEntity(projectile);
    }

    private Color colorFor(String element) {
        if ("ICE".equals(element))  return new Color(0.5f, 0.85f, 1f, 1f);
        if ("FIRE".equals(element)) return new Color(1f, 0.5f, 0.1f, 1f);
        return new Color(0.8f, 0.8f, 0.2f, 1f); // défaut (électrique/inconnu)
    }
}
