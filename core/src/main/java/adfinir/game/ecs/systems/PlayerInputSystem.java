package adfinir.game.ecs.systems;

import adfinir.game.combat.CapacityBurst;
import adfinir.game.ecs.components.CombatComponent;
import adfinir.game.ecs.components.InventoryComponent;
import adfinir.game.ecs.components.LootComponent;
import adfinir.game.ecs.components.PlayerInputComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.ecs.components.VelocityComponent;
import adfinir.game.inventory.Armor;
import adfinir.game.inventory.Artifact;
import adfinir.game.inventory.Capacity;
import adfinir.game.inventory.Item;
import adfinir.game.inventory.ItemGenerator;
import adfinir.game.inventory.Weapon;
import adfinir.game.inventory.WeaponType;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;

public class PlayerInputSystem extends IteratingSystem {

    private static final float PICKUP_RADIUS = 16f;

    private final ComponentMapper<VelocityComponent>    vm = ComponentMapper.getFor(VelocityComponent.class);
    private final ComponentMapper<PlayerInputComponent> pm = ComponentMapper.getFor(PlayerInputComponent.class);
    private final ComponentMapper<CombatComponent>      cm = ComponentMapper.getFor(CombatComponent.class);
    private final ComponentMapper<InventoryComponent>   im = ComponentMapper.getFor(InventoryComponent.class);
    private final ComponentMapper<PlayerStatsComponent>  sm = ComponentMapper.getFor(PlayerStatsComponent.class);
    private final ComponentMapper<TransformComponent>    tm = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<LootComponent>         lm = ComponentMapper.getFor(LootComponent.class);

    private final Family enemyFamily;
    private final Family lootFamily;

    public PlayerInputSystem(Family enemyFamily, Family lootFamily) {
        super(Family.all(PlayerInputComponent.class, VelocityComponent.class).get());
        this.enemyFamily = enemyFamily;
        this.lootFamily = lootFamily;
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

        // Compétences : touches 1 à 4 (hotbar façon LoL/Minecraft)
        int[] spellKeys = { Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4 };
        for (int slot = 0; slot < spellKeys.length; slot++) {
            if (Gdx.input.isKeyJustPressed(spellKeys[slot])) {
                tryCastCapacity(entity, input, slot);
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

        // Ramassage de loot : Touche F
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            tryPickupLoot(entity);
        }
    }

    private void tryCastCapacity(Entity entity, PlayerInputComponent input, int slot) {
        InventoryComponent inv = im.get(entity);
        CombatComponent combat = cm.get(entity);
        PlayerStatsComponent stats = sm.get(entity);
        TransformComponent origin = tm.get(entity);
        if (inv == null || inv.spells[slot] == null || combat == null || stats == null || origin == null) return;
        if (!combat.canUseCapacity(slot)) return;

        Capacity cap = inv.spells[slot];
        CapacityBurst.BurstParams p = CapacityBurst.resolve(cap);
        if (!stats.consumeStamina(p.staminaCost)) return;

        combat.spellTimers[slot] = p.cooldown;
        combat.capacityBursting = true;
        combat.capacityBurstTimer = 0.15f;
        combat.attackDirX = input.lastDirX;
        combat.attackDirY = input.lastDirY;

        float castX = origin.x + input.lastDirX * (p.radius * 0.5f);
        float castY = origin.y + input.lastDirY * (p.radius * 0.5f);
        CapacityBurst.applyBurst(getEngine(), enemyFamily, castX, castY, p.damage, p.radius);
    }

    private void tryPickupLoot(Entity entity) {
        InventoryComponent inv = im.get(entity);
        TransformComponent playerPos = tm.get(entity);
        if (inv == null || playerPos == null) return;

        ImmutableArray<Entity> lootEntities = getEngine().getEntitiesFor(lootFamily);
        for (Entity lootEntity : lootEntities) {
            TransformComponent lootPos = tm.get(lootEntity);
            if (Vector2.dst(playerPos.x, playerPos.y, lootPos.x, lootPos.y) > PICKUP_RADIUS) continue;

            LootComponent loot = lm.get(lootEntity);
            applyPickup(entity, inv, loot.item);
            getEngine().removeEntity(lootEntity);
            Gdx.app.log("Input", "Ramassé : " + loot.item.name);
            break; // un seul ramassage par pression de touche
        }
    }

    private void applyPickup(Entity entity, InventoryComponent inv, Item item) {
        if (item instanceof Weapon) {
            inv.equipWeapon((Weapon) item);
            CombatComponent combat = cm.get(entity);
            if (combat != null) combat.weapon = (Weapon) item;
        } else if (item instanceof Armor) {
            inv.equipArmor((Armor) item);
        } else if (item instanceof Capacity) {
            inv.equipSpellAuto((Capacity) item);
        } else if (item instanceof Artifact) {
            inv.equipArtifact((Artifact) item);
        }

        PlayerStatsComponent stats = sm.get(entity);
        if (stats != null) inv.updateStats(stats.stats);
    }
}
