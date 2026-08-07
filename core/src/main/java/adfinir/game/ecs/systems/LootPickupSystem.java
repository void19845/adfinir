package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.LootBarComponent;
import adfinir.game.ecs.components.LootComponent;
import adfinir.game.ecs.components.RenderComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.inventory.Armor;
import adfinir.game.inventory.Capacity;
import adfinir.game.inventory.Item;
import adfinir.game.inventory.ItemGenerator;
import adfinir.game.inventory.Weapon;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;

/**
 * Gère les objets de loot au sol quand le joueur marche dessus :
 *  - Barre de loot (8 slots) non pleine : ramassage automatique, l'item généré
 *    rejoint le premier emplacement libre de la barre (PAS d'équipement direct :
 *    l'équipement se fait ensuite via InventoryComponent.equipFromBar, touche 1-8
 *    ou clic sur la barre).
 *  - Barre pleine : le ramassage automatique est bloqué. Le joueur peut appuyer
 *    sur [F] pour échanger manuellement l'item au sol avec l'item actuellement
 *    en surbrillance dans la barre ; l'ancien item de la barre est alors reposé
 *    au sol à la place de celui ramassé.
 */
public class LootPickupSystem extends IteratingSystem {
    private static final float PICKUP_RANGE = 10f;

    private final ComponentMapper<LootComponent> lootMapper = ComponentMapper.getFor(LootComponent.class);
    private final ComponentMapper<TransformComponent> transformMapper = ComponentMapper.getFor(TransformComponent.class);

    private final Entity player;
    private final LootBarComponent lootBar;

    public LootPickupSystem(Entity player, LootBarComponent lootBar) {
        super(Family.all(LootComponent.class, TransformComponent.class).get(), 6);
        this.player = player;
        this.lootBar = lootBar;
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

        if (!lootBar.isFull()) {
            // Ramassage automatique : rejoint la barre, pas d'équipement direct.
            Item picked = resolveItem(loot);
            int slot = lootBar.firstEmptySlot();
            lootBar.slots[slot] = picked;
            getEngine().removeEntity(entity);
            return;
        }

        // Barre pleine : échange manuel avec le slot en surbrillance via [F]
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            Item incoming = resolveItem(loot);
            int idx = lootBar.selectedIndex;
            Item outgoing = lootBar.slots[idx];

            lootBar.slots[idx] = incoming;

            getEngine().removeEntity(entity);
            spawnGroundItem(outgoing, pos.x, pos.y);
        }
    }

    /** Résout l'item à ramasser : celui déjà généré (item reposé au sol) ou un tirage neuf. */
    private Item resolveItem(LootComponent loot) {
        if (loot.concreteItem != null) return loot.concreteItem;
        switch (loot.type) {
            case WEAPON:   return ItemGenerator.generateWeapon(loot.threatFactor);
            case ARMOR:    return ItemGenerator.generateArmor(loot.threatFactor);
            case CAPACITY: return ItemGenerator.generateCapacity(loot.threatFactor);
            case ARTIFACT: return ItemGenerator.generateArtifact(loot.threatFactor);
            default:       return null;
        }
    }

    /** Repose un item concret au sol (échange [F]) — même apparence que GameScreen.spawnLoot(). */
    private void spawnGroundItem(Item item, float x, float y) {
        if (item == null) return;

        Entity lootEntity = new Entity();

        TransformComponent transform = new TransformComponent();
        transform.x = x;
        transform.y = y;

        RenderComponent render = new RenderComponent();
        render.color = new Color(1.0f, 0.85f, 0.2f, 1f); // Doré, cohérent avec le loot procédural
        render.width = 8f;
        render.height = 8f;

        LootComponent dropped = new LootComponent();
        dropped.type = typeOf(item);
        dropped.concreteItem = item;

        lootEntity.add(transform);
        lootEntity.add(render);
        lootEntity.add(dropped);

        getEngine().addEntity(lootEntity);
    }

    private LootComponent.LootType typeOf(Item item) {
        if (item instanceof Weapon)   return LootComponent.LootType.WEAPON;
        if (item instanceof Armor)    return LootComponent.LootType.ARMOR;
        if (item instanceof Capacity) return LootComponent.LootType.CAPACITY;
        return LootComponent.LootType.ARTIFACT;
    }
}
