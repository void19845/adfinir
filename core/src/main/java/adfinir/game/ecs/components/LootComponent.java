package adfinir.game.ecs.components;

import com.badlogic.ashley.core.Component;

/**
 * Marque une entité comme un objet de loot posé au sol, généré par une tile TILE_LOOT.
 * Le type d'objet est tiré à la génération ; sa rareté effective est tirée au moment
 * du ramassage via ItemGenerator, en fonction du threatFactor de l'étage sur lequel
 * le loot a été semé.
 */
public class LootComponent implements Component {
    public enum LootType { WEAPON, ARMOR, CAPACITY, ARTIFACT }

    public LootType type;
    public float threatFactor = 1f;
}
