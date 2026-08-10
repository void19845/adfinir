package adfinir.game.ui;

import adfinir.game.inventory.Armor;
import adfinir.game.inventory.Artifact;
import adfinir.game.inventory.Capacity;
import adfinir.game.inventory.Item;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Icônes géométriques simples par catégorie d'objet (pas de sprite dédié disponible,
 * à l'inverse des armes qui utilisent WeaponSpriteManager).
 * À appeler dans un bloc shapes.begin(ShapeType.Filled) / end() déjà ouvert par l'appelant.
 */
public class ItemIcon {

    public static void draw(ShapeRenderer shapes, Item item, float x, float y, float w, float h) {
        float cx = x + w / 2f;
        float cy = y + h / 2f;
        float size = Math.min(w, h) * 0.3f;

        if (item instanceof Armor) {
            drawShield(shapes, cx, cy, size);
        } else if (item instanceof Capacity) {
            drawSpell(shapes, (Capacity) item, cx, cy, size);
        } else if (item instanceof Artifact) {
            drawDiamond(shapes, cx, cy, size);
        }
    }

    private static void drawShield(ShapeRenderer shapes, float cx, float cy, float size) {
        shapes.setColor(Color.LIGHT_GRAY);
        shapes.triangle(cx - size, cy + size * 0.5f, cx + size, cy + size * 0.5f, cx, cy - size);
        shapes.rect(cx - size, cy, size * 2f, size * 0.5f);
    }

    private static void drawSpell(ShapeRenderer shapes, Capacity cap, float cx, float cy, float size) {
        shapes.setColor(elementColor(cap.mainEffect.type));
        shapes.circle(cx, cy, size);
        shapes.setColor(Color.WHITE);
        shapes.circle(cx, cy, size * 0.35f);
    }

    private static void drawDiamond(ShapeRenderer shapes, float cx, float cy, float size) {
        shapes.setColor(Color.GOLD);
        shapes.triangle(cx, cy + size, cx - size, cy, cx, cy - size);
        shapes.triangle(cx, cy + size, cx + size, cy, cx, cy - size);
    }

    public static Color elementColor(String element) {
        if (element == null) return Color.LIGHT_GRAY;
        switch (element) {
            case "FIRE":     return Color.ORANGE;
            case "ICE":      return Color.CYAN;
            case "ELECTRIC": return Color.YELLOW;
            case "POISON":   return Color.GREEN;
            case "WIND":     return Color.WHITE;
            default:         return Color.PURPLE;
        }
    }
}
