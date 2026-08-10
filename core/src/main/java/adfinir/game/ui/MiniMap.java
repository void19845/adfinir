package adfinir.game.ui;

import adfinir.game.dungeon.DungeonMap;
import adfinir.game.ecs.components.TransformComponent;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Affiche une version réduite de la carte en haut à droite de l'écran.
 */
public class MiniMap {
    private final float size = 100f; // Taille carrée de la mini-map en pixels
    private final float padding = 10f;

    public void draw(ShapeRenderer sr, DungeonMap map, TransformComponent playerTransform, float screenW, float screenH) {
        float mapWidth = map.getPixelWidth();
        float mapHeight = map.getPixelHeight();

        // On utilise le plus petit ratio pour garder les proportions
        float scale = Math.min(size / mapWidth, size / mapHeight);
        float scaledW = mapWidth * scale;
        float scaledH = mapHeight * scale;

        // Position en haut à droite
        float offsetX = screenW - scaledW - padding;
        float offsetY = screenH - scaledH - padding;

        float pad = 4f;

        // Panneau en relief autour de la carte + fond ardoise de la carte elle-même
        sr.begin(ShapeRenderer.ShapeType.Filled);
        UiTheme.panel(sr, offsetX - pad, offsetY - pad, scaledW + pad * 2, scaledH + pad * 2);
        sr.setColor(0.03f, 0.03f, 0.05f, 0.9f);
        sr.rect(offsetX, offsetY, scaledW, scaledH);

        // Dessin des murs
        sr.setColor(0.42f, 0.44f, 0.5f, 1f);
        for (int r = 0; r < map.rows; r++) {
            for (int c = 0; c < map.cols; c++) {
                if (map.getTile(c, r) == DungeonMap.TILE_WALL) {
                    sr.rect(
                        offsetX + c * DungeonMap.TILE_SIZE * scale,
                        offsetY + r * DungeonMap.TILE_SIZE * scale,
                        DungeonMap.TILE_SIZE * scale,
                        DungeonMap.TILE_SIZE * scale
                    );
                }
            }
        }

        // Joueur
        sr.setColor(new Color(0.4f, 0.75f, 1f, 1f));
        float playerX = offsetX + playerTransform.x * scale;
        float playerY = offsetY + playerTransform.y * scale;
        sr.rect(playerX - 2f, playerY - 2f, 4f, 4f);
        sr.end();
    }
}
