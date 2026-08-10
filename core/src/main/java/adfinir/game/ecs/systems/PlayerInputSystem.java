package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.CapacityComponent;
import adfinir.game.ecs.components.CombatComponent;
import adfinir.game.ecs.components.InventoryComponent;
import adfinir.game.ecs.components.LootBarComponent;
import adfinir.game.ecs.components.PlayerInputComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import adfinir.game.ecs.components.VelocityComponent;
import adfinir.game.inventory.ItemGenerator;
import adfinir.game.inventory.Weapon;
import adfinir.game.inventory.WeaponType;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

public class PlayerInputSystem extends IteratingSystem {

    /** Touches 1 à 8 : sélectionnent/équipent le slot correspondant de la barre de loot. */
    private static final int[] BAR_SELECT_KEYS = {
        Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4,
        Input.Keys.NUM_5, Input.Keys.NUM_6, Input.Keys.NUM_7, Input.Keys.NUM_8
    };

    private final ComponentMapper<VelocityComponent>    vm = ComponentMapper.getFor(VelocityComponent.class);
    private final ComponentMapper<PlayerInputComponent> pm = ComponentMapper.getFor(PlayerInputComponent.class);
    private final ComponentMapper<CombatComponent>      cm = ComponentMapper.getFor(CombatComponent.class);
    private final ComponentMapper<CapacityComponent>    capm = ComponentMapper.getFor(CapacityComponent.class);
    private final ComponentMapper<InventoryComponent>   im = ComponentMapper.getFor(InventoryComponent.class);
    private final ComponentMapper<LootBarComponent>     lbm = ComponentMapper.getFor(LootBarComponent.class);
    private final ComponentMapper<PlayerStatsComponent> sm = ComponentMapper.getFor(PlayerStatsComponent.class);

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
        if (len > 0f) {
            dx /= len;
            dy /= len;
            // Mise à jour de l'orientation basée sur le mouvement actuel
            input.lastDirX = dx;
            input.lastDirY = dy;
        }

        vel.vx = dx * input.speed;
        vel.vy = dy * input.speed;

        // Attaque : Clic gauche (JustPressed) ou Touche Espace
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            CombatComponent combat = cm.get(entity);
            if (combat != null && combat.canAttack()) {
                // Utiliser la dernière direction enregistrée
                combat.triggerAttack(input.lastDirX, input.lastDirY);
            }
        }

        // Capacité : Clic droit (JustPressed)
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            CapacityComponent capacityComp = capm.get(entity);
            InventoryComponent inv = im.get(entity);
            if (capacityComp != null && inv != null && inv.capacity != null && capacityComp.canCast()) {
                capacityComp.trigger(input.lastDirX, input.lastDirY, inv.capacity.getActiveEffect().cooldown);
            }
        }

        // Switch arme : Touche X
        if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
            InventoryComponent inv = im.get(entity);
            if (inv != null) {
                // Cycle : SWORD -> SPEAR -> CLAYMORE -> SWORD
                WeaponType currentType = (inv.weapon != null) ? inv.weapon.type : WeaponType.SWORD;
                WeaponType nextType;
                switch (currentType) {
                    case SWORD:    nextType = WeaponType.SPEAR; break;
                    case SPEAR:    nextType = WeaponType.CLAYMORE; break;
                    case CLAYMORE: nextType = WeaponType.SWORD; break;
                    default:       nextType = WeaponType.SWORD; break;
                }

                inv.equipWeapon(ItemGenerator.createWeaponOfType(nextType));
                Gdx.app.log("Input", "Weapon switched to: " + nextType.name());

                // Synchronise le CombatComponent
                CombatComponent combat = cm.get(entity);
                if (combat != null) {
                    combat.weapon = inv.weapon;
                }
            }
        }

        // Sélection/équipement depuis la barre de loot : touches 1 à 8
        for (int i = 0; i < BAR_SELECT_KEYS.length; i++) {
            if (Gdx.input.isKeyJustPressed(BAR_SELECT_KEYS[i])) {
                LootBarComponent bar = lbm.get(entity);
                InventoryComponent inv = im.get(entity);
                PlayerStatsComponent stats = sm.get(entity);
                if (bar != null && inv != null && stats != null) {
                    inv.equipFromBarAndSync(bar, i, cm.get(entity), stats.stats);
                }
                break;
            }
        }
    }
}
