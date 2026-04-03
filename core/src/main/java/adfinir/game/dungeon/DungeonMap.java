package adfinir.game.dungeon;

/**
 * Représente la carte du donjon sous forme de grille d'entiers.
 *
 * Valeurs de tiles :
 *   0 = VIDE (hors carte)
 *   1 = MUR
 *   2 = SOL
 */
public class DungeonMap {

    public static final int TILE_EMPTY    = 0;
    public static final int TILE_WALL     = 1;
    public static final int TILE_FLOOR    = 2;
    public static final int TILE_CORRIDOR = 3;

    public static final int TILE_SIZE = 16; // pixels par tile

    private final int[][] grid;
    public final int cols;
    public final int rows;
    public final int spawnCol;
    public final int spawnRow;

    /** Constructeur utilisé par DungeonGenerator (avec spawn). */
    public DungeonMap(int[][] grid, int spawnCol, int spawnRow) {
        this.grid     = grid;
        this.rows     = grid.length;
        this.cols     = (rows > 0) ? grid[0].length : 0;
        this.spawnCol = spawnCol;
        this.spawnRow = spawnRow;
    }

    /** Constructeur pour les cartes statiques. */
    public DungeonMap(int[][] grid) {
        this(grid, (grid[0].length / 2), (grid.length / 2));
    }

    /** Retourne le type de tile à la position grille (col, row). */
    public int getTile(int col, int row) {
        if (col < 0 || row < 0 || col >= cols || row >= rows) return TILE_WALL;
        return grid[row][col];
    }

    /** Retourne vrai si la position pixel (px, py) tombe sur une tile solide. */
    public boolean isSolid(float px, float py) {
        int col  = (int) Math.floor(px / TILE_SIZE);
        int row  = (int) Math.floor(py / TILE_SIZE);
        int tile = getTile(col, row);
        return tile == TILE_WALL || tile == TILE_EMPTY;
    }

    public boolean isFloorLike(int tile) {
        return tile == TILE_FLOOR || tile == TILE_CORRIDOR;
    }

    /** Coordonnée X pixel du centre de spawn. */
    public float getSpawnPixelX() { return spawnCol * TILE_SIZE + TILE_SIZE / 2f; }

    /** Coordonnée Y pixel du centre de spawn. */
    public float getSpawnPixelY() { return spawnRow * TILE_SIZE + TILE_SIZE / 2f; }

    /** Retourne la largeur totale de la carte en pixels. */
    public float getPixelWidth()  { return cols * TILE_SIZE; }

    /** Retourne la hauteur totale de la carte en pixels. */
    public float getPixelHeight() { return rows * TILE_SIZE; }

    /**
     * Crée une carte de test statique (20×15 tiles).
     */
    public static DungeonMap createTestMap() {
        int W = TILE_WALL;
        int F = TILE_FLOOR;
        int[][] layout = {
            {W,W,W,W,W,W,W,W,W,W,W,W,W,W,W,W,W,W,W,W},
            {W,F,F,F,F,F,F,F,W,F,F,F,F,F,F,F,F,F,F,W},
            {W,F,F,F,F,F,F,F,W,F,F,F,F,F,F,F,F,F,F,W},
            {W,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,W},
            {W,F,F,F,W,W,F,F,W,F,F,W,W,F,F,F,F,F,F,W},
            {W,F,F,F,W,W,F,F,W,F,F,W,W,F,F,F,F,F,F,W},
            {W,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,W},
            {W,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,W},
            {W,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,W},
            {W,F,F,W,W,F,F,F,F,F,F,F,F,F,W,W,F,F,F,W},
            {W,F,F,W,W,F,F,F,F,F,F,F,F,F,W,W,F,F,F,W},
            {W,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,W},
            {W,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,W},
            {W,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,W},
            {W,W,W,W,W,W,W,W,W,W,W,W,W,W,W,W,W,W,W,W},
        };
        return new DungeonMap(layout);
    }
}
