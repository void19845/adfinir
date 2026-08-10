package adfinir.game.ui;

import adfinir.game.dungeon.DungeonMap;
import adfinir.game.ecs.components.TransformComponent;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Affiche une version réduite de la carte en haut à droite de l'écran.
 */
public class MiniMap {
    private final float size = 100f; // Taille carrée de la mini-map en pixels
    private final float padding = 10f;

    public void draw(ShapeRenderer sr, DungeonMap map, TransformComponent playerTransform, float screenW, float screenH) {
        draw(sr, map, playerTransform, screenW, screenH, null, null);
    }

    public void draw(ShapeRenderer sr, DungeonMap map, TransformComponent playerTransform, float screenW, float screenH,
                      ImmutableArray<Entity> enemies, ComponentMapper<TransformComponent> transformMapper) {
        float mapWidth = map.getPixelWidth();
        float mapHeight = map.getPixelHeight();

        // On utilise le plus petit ratio pour garder les proportions
        float scale = Math.min(size / mapWidth, size / mapHeight);
        float scaledW = mapWidth * scale;
        float scaledH = mapHeight * scale;

        // Position en haut à droite
        float offsetX = screenW - scaledW - padding;
        float offsetY = screenH - scaledH - padding;

        // Fond de la mini-map
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0f, 0f, 0f, 0.5f);
        sr.rect(offsetX, offsetY, scaledW, scaledH);

        // Dessin des murs
        sr.setColor(0.5f, 0.5f, 0.5f, 1f);
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

        // Sortie du niveau (doré)
        sr.setColor(Color.GOLD);
        sr.rect(
            offsetX + map.exitCol * DungeonMap.TILE_SIZE * scale,
            offsetY + map.exitRow * DungeonMap.TILE_SIZE * scale,
            DungeonMap.TILE_SIZE * scale,
            DungeonMap.TILE_SIZE * scale
        );

        // Ennemis vivants (points rouges)
        if (enemies != null && transformMapper != null) {
            sr.setColor(Color.RED);
            for (Entity enemy : enemies) {
                TransformComponent t = transformMapper.get(enemy);
                float ex = offsetX + t.x * scale;
                float ey = offsetY + t.y * scale;
                sr.rect(ex - 1.5f, ey - 1.5f, 3f, 3f);
            }
        }

        // Joueur
        sr.setColor(Color.BLUE);
        float playerX = offsetX + playerTransform.x * scale;
        float playerY = offsetY + playerTransform.y * scale;
        sr.rect(playerX - 2f, playerY - 2f, 4f, 4f);
        sr.end();

        // Bordure
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(Color.LIGHT_GRAY);
        sr.rect(offsetX, offsetY, scaledW, scaledH);
        sr.end();
    }
}
