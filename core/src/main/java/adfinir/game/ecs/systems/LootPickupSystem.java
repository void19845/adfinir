package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.CombatComponent;
import adfinir.game.ecs.components.InventoryComponent;
import adfinir.game.ecs.components.LootComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.inventory.ItemGenerator;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;

/**
 * Ramasse automatiquement les objets de loot au sol quand le joueur marche dessus.
 * L'objet remplace l'équipement actuel du slot correspondant (pas de sac séparé,
 * cohérent avec les 4 slots gérés par InventoryComponent).
 */
public class LootPickupSystem extends IteratingSystem {
    private static final float PICKUP_RANGE = 10f;

    private final ComponentMapper<LootComponent> lootMapper = ComponentMapper.getFor(LootComponent.class);
    private final ComponentMapper<TransformComponent> transformMapper = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<CombatComponent> combatMapper = ComponentMapper.getFor(CombatComponent.class);

    private final Entity player;
    private final InventoryComponent inventory;
    private final PlayerStatsComponent playerStats;

    public LootPickupSystem(Entity player, InventoryComponent inventory, PlayerStatsComponent playerStats) {
        super(Family.all(LootComponent.class, TransformComponent.class).get(), 6);
        this.player = player;
        this.inventory = inventory;
        this.playerStats = playerStats;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent pos = transformMapper.get(entity);
        TransformComponent playerPos = transformMapper.get(player);
        if (playerPos == null) return;

        float dx = playerPos.x - pos.x;
        float dy = playerPos.y - pos.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > PICKUP_RANGE) return;

        LootComponent loot = lootMapper.get(entity);
        switch (loot.type) {
            case WEAPON:
                inventory.equipWeapon(ItemGenerator.generateWeapon(loot.threatFactor));
                CombatComponent combat = combatMapper.get(player);
                if (combat != null) combat.weapon = inventory.weapon;
                break;
            case ARMOR:
                inventory.equipArmor(ItemGenerator.generateArmor(loot.threatFactor));
                break;
            case CAPACITY:
                inventory.equipCapacity(ItemGenerator.generateCapacity(loot.threatFactor));
                break;
            case ARTIFACT:
                inventory.equipArtifact(ItemGenerator.generateArtifact(loot.threatFactor));
                break;
        }

        inventory.updateStats(playerStats.stats);
        getEngine().removeEntity(entity);
    }
}
