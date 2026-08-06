package adfinir.game.dungeon;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/**
 * Dessine la grille de tiles avec ShapeRenderer.
 * Chaque tile est un rectangle coloré — à remplacer par un TileMap/Sprite plus tard.
 */
public class DungeonRenderer {

    private static final Color COLOR_FLOOR = new Color(0.22f, 0.20f, 0.18f, 1f);
    private static final Color COLOR_WALL  = new Color(0.45f, 0.43f, 0.40f, 1f);
    private static final Color COLOR_GRID  = new Color(0.12f, 0.11f, 0.10f, 1f);
    private static final Color COLOR_EXIT  = new Color(0.10f, 0.80f, 0.30f, 1f); // vert vif

    private final DungeonMap map;

    public DungeonRenderer(DungeonMap map) {
        this.map = map;
    }

    /**
     * Dessine toutes les tiles visibles dans le viewport fourni.
     * Doit être appelé entre shapeRenderer.begin() et shapeRenderer.end().
     */
    public void render(ShapeRenderer sr, Rectangle viewport) {
        int ts = DungeonMap.TILE_SIZE;

        // Calcule la plage de tiles visible (culling)
        int colMin = Math.max(0, (int) Math.floor(viewport.x / ts));
        int rowMin = Math.max(0, (int) Math.floor(viewport.y / ts));
        int colMax = Math.min(map.cols - 1, (int) Math.ceil((viewport.x + viewport.width)  / ts));
        int rowMax = Math.min(map.rows - 1, (int) Math.ceil((viewport.y + viewport.height) / ts));

        for (int row = rowMin; row <= rowMax; row++) {
            for (int col = colMin; col <= colMax; col++) {
                int tile = map.getTile(col, row);
                float px = col * ts;
                float py = row * ts;

                switch (tile) {
                    case DungeonMap.TILE_FLOOR:
                        sr.setColor(COLOR_FLOOR);
                        sr.rect(px, py, ts, ts);
                        sr.setColor(COLOR_GRID);
                        sr.rect(px, py, ts, 1);
                        sr.rect(px, py, 1, ts);
                        break;
                    case DungeonMap.TILE_WALL:
                        sr.setColor(COLOR_WALL);
                        sr.rect(px, py, ts, ts);
                        break;
                    case DungeonMap.TILE_EXIT:
                        // Fond de sol + carré vert centré
                        sr.setColor(COLOR_FLOOR);
                        sr.rect(px, py, ts, ts);
                        sr.setColor(COLOR_EXIT);
                        float margin = ts * 0.2f;
                        sr.rect(px + margin, py + margin, ts - margin * 2, ts - margin * 2);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public void dispose() {
        // Rien à libérer ici (ShapeRenderer géré par GameScreen)
    }
}
