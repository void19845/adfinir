package adfinir.game.util;

import adfinir.game.dungeon.DungeonMap;
import com.badlogic.gdx.math.Vector2;
import java.util.*;

public class Pathfinding {
    public static Vector2 findNextStep(DungeonMap map, Vector2 start, Vector2 target) {
        int startCol = (int) (start.x / DungeonMap.TILE_SIZE);
        int startRow = (int) (start.y / DungeonMap.TILE_SIZE);
        int targetCol = (int) (target.x / DungeonMap.TILE_SIZE);
        int targetRow = (int) (target.y / DungeonMap.TILE_SIZE);

        if (startCol == targetCol && startRow == targetRow) return null;

        Queue<int[]> queue = new LinkedList<>();
        Map<String, int[]> parent = new HashMap<>();

        queue.add(new int[]{startCol, startRow});
        parent.put(startCol + "," + startRow, null);

        // Cardinales + diagonales
        int[][] dirs = {
            {0, 1}, {0, -1}, {1, 0}, {-1, 0},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int c = curr[0];
            int r = curr[1];

            if (c == targetCol && r == targetRow) {
                List<int[]> path = new ArrayList<>();
                int[] temp = curr;
                while (temp != null) {
                    path.add(temp);
                    temp = parent.get(temp[0] + "," + temp[1]);
                }
                Collections.reverse(path);

                if (path.size() > 1) {
                    int[] next = path.get(1);
                    return new Vector2(
                        next[0] * DungeonMap.TILE_SIZE + DungeonMap.TILE_SIZE / 2f,
                        next[1] * DungeonMap.TILE_SIZE + DungeonMap.TILE_SIZE / 2f
                    );
                }
                return null;
            }

            for (int[] d : dirs) {
                int nc = c + d[0];
                int nr = r + d[1];
                String key = nc + "," + nr;

                if (nc < 0 || nc >= map.cols || nr < 0 || nr >= map.rows) continue;
                if (map.getTile(nc, nr) == DungeonMap.TILE_WALL) continue;
                if (parent.containsKey(key)) continue;

                // Anti-corner-cutting : en diagonale, les deux cases adjacentes
                // (horizontale + verticale) doivent aussi être libres
                boolean isDiagonal = d[0] != 0 && d[1] != 0;
                if (isDiagonal) {
                    boolean sideBlocked = map.getTile(c + d[0], r) == DungeonMap.TILE_WALL
                        || map.getTile(c, r + d[1]) == DungeonMap.TILE_WALL;
                    if (sideBlocked) continue;
                }

                parent.put(key, curr);
                queue.add(new int[]{nc, nr});
            }
        }

        return null; // Aucun chemin trouvé
    }
}
