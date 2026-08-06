package adfinir.game.util;

import adfinir.game.dungeon.DungeonMap;
import com.badlogic.gdx.math.Vector2;
import java.util.*;

/**
 * Utilitaire pour trouver un chemin sur la grille du donjon.
 */
public class Pathfinding {
    public static Vector2 findNextStep(DungeonMap map, Vector2 start, Vector2 target) {
        int startCol = (int) (start.x / DungeonMap.TILE_SIZE);
        int startRow = (int) (start.y / DungeonMap.TILE_SIZE);
        int targetCol = (int) (target.x / DungeonMap.TILE_SIZE);
        int targetRow = (int) (target.y / DungeonMap.TILE_SIZE);

        if (startCol == targetCol && startRow == targetRow) return null;

        // BFS pour trouver le chemin le plus court vers la tile cible
        Queue<int[]> queue = new LinkedList<>();
        Map<String, int[]> parent = new HashMap<>();

        queue.add(new int[]{startCol, startRow});
        parent.put(startCol + "," + startRow, null);

        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int c = curr[0];
            int r = curr[1];

            if (c == targetCol && r == targetRow) {
                // On a trouvé la cible, on remonte le chemin pour trouver la première étape
                int[] step = curr;
                while (parent.get(step[0] + "," + step[1]) != null) {
                    // On ne veut pas remonter jusqu'au départ, mais juste à l'étape suivante
                    // On va stocker le chemin et prendre le premier élément
                }
                // Correction : on reconstruit le chemin complet
                List<int[]> path = new ArrayList<>();
                int[] temp = curr;
                while (temp != null) {
                    path.add(temp);
                    temp = parent.get(temp[0] + "," + temp[1]);
                }
                Collections.reverse(path);

                // Le premier élément est le départ, le deuxième est la première étape
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

                if (nc >= 0 && nc < map.cols && nr >= 0 && nr < map.rows
                    && map.getTile(nc, nr) != DungeonMap.TILE_WALL
                    && !parent.containsKey(key)) {
                    parent.put(key, curr);
                    queue.add(new int[]{nc, nr});
                }
            }
        }

        return null; // Aucun chemin trouvé
    }
}
