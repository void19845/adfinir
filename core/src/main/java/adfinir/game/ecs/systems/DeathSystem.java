package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.EnemyStatsComponent;
import adfinir.game.ecs.components.LootComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import adfinir.game.ecs.components.RenderComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.inventory.Item;
import adfinir.game.inventory.ItemGenerator;
import adfinir.game.ui.RarityColors;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

/**
 * Système qui retire les entités mortes du moteur, en laissant parfois un objet au sol
 * et en versant de l'or au joueur.
 */
public class DeathSystem extends IteratingSystem {
    private static final float DROP_CHANCE = 0.35f;

    private final ComponentMapper<EnemyStatsComponent> enemyStatsMapper = ComponentMapper.getFor(EnemyStatsComponent.class);
    private final ComponentMapper<TransformComponent> transformMapper = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<PlayerStatsComponent> playerStatsMapper = ComponentMapper.getFor(PlayerStatsComponent.class);

    private final Entity player;

    public DeathSystem(Entity player) {
        super(Family.all(EnemyStatsComponent.class).get(), 100); // Tourne à la fin
        this.player = player;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        EnemyStatsComponent stats = enemyStatsMapper.get(entity);
        if (stats != null && stats.isDead) {
            if (MathUtils.random() < DROP_CHANCE) {
                TransformComponent pos = transformMapper.get(entity);
                spawnLoot(pos.x, pos.y);
            }
            awardGold(stats);
            // On ne peut pas supprimer une entité directement pendant l'itération d'un IteratingSystem
            // sans risque, mais Ashley gère assez bien engine.removeEntity().
            getEngine().removeEntity(entity);
        }
    }

    private void awardGold(EnemyStatsComponent stats) {
        if (player == null) return;
        PlayerStatsComponent playerStats = playerStatsMapper.get(player);
        if (playerStats == null) return;
        int reward = Math.round(stats.maxHp * 0.5f);
        playerStats.addGold(reward);
    }

    private void spawnLoot(float x, float y) {
        Entity lootEntity = new Entity();

        TransformComponent transform = new TransformComponent();
        transform.x = x;
        transform.y = y;

        Item item = rollItem();

        LootComponent loot = new LootComponent();
        loot.item = item;
        loot.rarity = item.rarity;

        RenderComponent render = new RenderComponent();
        render.color = new Color(RarityColors.get(item.rarity));
        render.width = 12f;
        render.height = 12f;

        lootEntity.add(transform);
        lootEntity.add(render);
        lootEntity.add(loot);

        getEngine().addEntity(lootEntity);
    }

    private Item rollItem() {
        switch (MathUtils.random(3)) {
            case 0:  return ItemGenerator.generateWeapon();
            case 1:  return ItemGenerator.generateArmor();
            case 2:  return ItemGenerator.generateCapacity();
            default: return ItemGenerator.generateArtifact();
        }
    }
}
