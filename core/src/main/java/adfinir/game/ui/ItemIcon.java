package adfinir.game.ui;

import adfinir.game.inventory.Armor;
import adfinir.game.inventory.Artifact;
import adfinir.game.inventory.Capacity;
import adfinir.game.inventory.Item;
import adfinir.game.inventory.ItemModifier;
import adfinir.game.inventory.WeaponComboMod;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Icônes vectorielles simples par catégorie d'objet, pour qu'un slot soit
 * identifiable au premier coup d'œil sans avoir à lire le nom (les armes ont
 * un vrai sprite via WeaponSpriteManager ; tout le reste n'a que ces formes
 * géométriques). Chaque forme a une silhouette distincte ET une couleur
 * distincte (indépendamment de la rareté, qui est portée par le cadre du
 * slot) pour rester lisible même en un coup d'œil rapide sur la hotbar.
 *
 * À appeler à l'intérieur d'un bloc shapes.begin(ShapeType.Filled) déjà
 * ouvert par l'appelant.
 */
public final class ItemIcon {

    private ItemIcon() {}

    public static void draw(ShapeRenderer shapes, Item item, float x, float y, float w, float h) {
        float cx = x + w / 2f;
        float cy = y + h / 2f;
        float size = Math.min(w, h) * 0.32f;

        if (item instanceof Armor) {
            drawShield(shapes, cx, cy, size);
        } else if (item instanceof Capacity) {
            drawOrb(shapes, elementColor(((Capacity) item).getActiveEffect().type), cx, cy, size);
        } else if (item instanceof Artifact) {
            drawGem(shapes, cx, cy, size);
        } else if (item instanceof ItemModifier) {
            drawModGlyph(shapes, (ItemModifier) item, cx, cy, size);
        }
    }

    /** Bouclier plein (Armure) : dôme + base, gris acier. */
    private static void drawShield(ShapeRenderer shapes, float cx, float cy, float size) {
        Color steel = new Color(0.68f, 0.72f, 0.78f, 1f);
        shapes.setColor(steel);
        shapes.triangle(cx - size, cy + size * 0.4f, cx + size, cy + size * 0.4f, cx, cy - size);
        shapes.rect(cx - size, cy, size * 2f, size * 0.4f);
        shapes.setColor(new Color(0.35f, 0.4f, 0.48f, 1f));
        shapes.rect(cx - size * 0.15f, cy - size * 0.6f, size * 0.3f, size * 1.3f); // renfort central
    }

    /** Orbe pleine (Capacité) : couleur = élément actif, cœur clair. */
    private static void drawOrb(ShapeRenderer shapes, Color elementColor, float cx, float cy, float size) {
        shapes.setColor(elementColor);
        shapes.circle(cx, cy, size, 20);
        shapes.setColor(new Color(1f, 1f, 1f, 0.75f));
        shapes.circle(cx, cy, size * 0.4f, 16);
    }

    /** Gemme losange (Artéfact) : doré, facette centrale plus claire. */
    private static void drawGem(ShapeRenderer shapes, float cx, float cy, float size) {
        shapes.setColor(new Color(0.95f, 0.78f, 0.15f, 1f));
        shapes.triangle(cx, cy + size, cx - size, cy, cx, cy - size);
        shapes.triangle(cx, cy + size, cx + size, cy, cx, cy - size);
        shapes.setColor(new Color(1f, 0.92f, 0.55f, 1f));
        shapes.triangle(cx, cy + size * 0.45f, cx - size * 0.35f, cy, cx, cy - size * 0.45f);
        shapes.triangle(cx, cy + size * 0.45f, cx + size * 0.35f, cy, cx, cy - size * 0.45f);
    }

    /** Losange plein tourné 45° (ItemModifier, ◈ agrandi) : rouge pour combo d'arme, bleu pour effet magique. */
    private static void drawModGlyph(ShapeRenderer shapes, ItemModifier mod, float cx, float cy, float size) {
        Color c = mod instanceof WeaponComboMod
            ? new Color(0.8f, 0.25f, 0.25f, 1f)
            : new Color(0.25f, 0.45f, 0.85f, 1f);
        shapes.setColor(c);
        shapes.triangle(cx, cy + size, cx - size * 0.7f, cy, cx + size * 0.7f, cy);
        shapes.triangle(cx, cy - size, cx - size * 0.7f, cy, cx + size * 0.7f, cy);
    }

    public static Color elementColor(String element) {
        if (element == null) return Color.LIGHT_GRAY;
        switch (element) {
            case "FIRE":     return new Color(1f, 0.45f, 0.1f, 1f);
            case "ICE":      return new Color(0.45f, 0.8f, 1f, 1f);
            case "ELECTRIC": return new Color(0.9f, 0.85f, 0.2f, 1f);
            case "POISON":   return new Color(0.4f, 0.85f, 0.3f, 1f);
            case "WIND":     return new Color(0.85f, 0.9f, 0.9f, 1f);
            default:         return new Color(0.7f, 0.4f, 0.9f, 1f);
        }
    }
}
