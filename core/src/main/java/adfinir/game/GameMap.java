// GameMap.java
package adfinir.game;
import java.util.Random;

public class GameMap {
    private final int width;
    private final int height;
    private final char[][] grid;
    private final Random random;

    // Constantes pour nos "textures" ASCII
    public static final char WATER = '-';
    public static final char LAND = '/';
    public static final char FOREST = '#';
    public static final char MOUNTAIN = '^';

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new char[height][width];
        this.random = new Random();
        generateMap();
    }

    private void generateMap() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int chance = random.nextInt(100);
                if (chance < 35) {
                    grid[y][x] = WATER;
                } else if (chance < 75) {
                    grid[y][x] = LAND;
                } else if (chance < 90) {
                    grid[y][x] = FOREST;
                } else {
                    grid[y][x] = MOUNTAIN;
                }
            }
        }
    }

    /**
     * @return Le tableau 2D de caractères représentant la carte.
     */
    public char[][] getGrid() {
        return grid;
    }

    public int getWidth() {
        return width;
    }



    public int getHeight() {
        return height;
    }
}
