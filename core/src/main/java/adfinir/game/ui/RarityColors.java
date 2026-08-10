package adfinir.game.ui;

import adfinir.game.inventory.Rarity;
import com.badlogic.gdx.graphics.Color;

/** Couleur associée à chaque rareté d'objet, réutilisée par l'inventaire et le loot au sol. */
public class RarityColors {
    public static Color get(Rarity rarity) {
        switch (rarity) {
            case LEGENDARY: return Color.ORANGE;
            case EPIC:      return Color.PURPLE;
            case RARE:      return Color.CYAN;
            case MYTHICAL:  return Color.MAGENTA;
            case COMMON:
            default:        return Color.WHITE;
        }
    }
}
