package adfinir.game.world;

/**
 * Carte open-world style Pokémon Gen 1.
 *
 * Structure :
 *  - Routes principales (E-O et N-S) qui se croisent au centre
 *  - Routes secondaires créant une grille de chemins
 *  - Herbes hautes le long des routes (zones de rencontre)
 *  - Forêts denses dans les coins et entre les routes
 *  - 4 lacs dans les quadrants
 *  - Grande clairière autour du spawn
 *
 * Types de tuiles :
 *  0 GRASS        herbe normale (praticable)
 *  1 PATH         chemin sableux (praticable)
 *  2 TREE         arbre (solide)
 *  3 TALL_GRASS   herbe haute (praticable, mobs)
 *  4 WATER        eau (solide)
 *  5 FLOWER       fleur décorative (praticable)
 *  6 ROCK         rocher (solide)
 */
public class WorldMap {

    public static final byte TILE_GRASS      = 0;
    public static final byte TILE_PATH       = 1;
    public static final byte TILE_TREE       = 2;
    public static final byte TILE_TALL_GRASS = 3;
    public static final byte TILE_WATER      = 4;
    public static final byte TILE_FLOWER     = 5;
    public static final byte TILE_ROCK       = 6;

    public static final int TILE_SIZE = 16;

    public final int cols, rows, spawnCol, spawnRow;
    private final byte[][] grid;

    public WorldMap(int cols, int rows) {
        this.cols = cols; this.rows = rows;
        this.spawnCol = cols / 2; this.spawnRow = rows / 2;
        this.grid = new byte[rows][cols];
        generate();
    }

    // ── Génération ────────────────────────────────────────────────────────

    private void generate() {
        fill(TILE_GRASS);
        carveAllPaths();
        addTallGrass();
        addLakes();
        addForests();
        addRocks();
        addFlowers();
        clearSpawn();
        addBorder();
    }

    /** Remplit toute la carte avec un type de tuile. */
    private void fill(byte tile) {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                grid[r][c] = tile;
    }

    /** Creuse les routes principales et secondaires (largeur 3). */
    private void carveAllPaths() {
        // Routes principales qui se croisent au centre
        carvePath(true,  rows / 2);
        carvePath(false, cols / 2);
        // Routes secondaires (quadrillage)
        carvePath(true,  rows / 4);
        carvePath(true,  3 * rows / 4);
        carvePath(false, cols / 4);
        carvePath(false, 3 * cols / 4);
    }

    private void carvePath(boolean horizontal, int center) {
        for (int off = -1; off <= 1; off++) {
            if (horizontal) {
                int r = center + off;
                if (r < 0 || r >= rows) continue;
                for (int c = 0; c < cols; c++) grid[r][c] = TILE_PATH;
            } else {
                int c = center + off;
                if (c < 0 || c >= cols) continue;
                for (int r = 0; r < rows; r++) grid[r][c] = TILE_PATH;
            }
        }
    }

    /** Pose de l'herbe haute sur les tuiles GRASS adjacentes aux chemins. */
    private void addTallGrass() {
        byte[][] copy = copyGrid();
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                if (copy[r][c] != TILE_GRASS) continue;
                if (bordersPath(copy, r, c, 2) && hash(c, r) % 3 != 0)
                    grid[r][c] = TILE_TALL_GRASS;
            }
        }
    }

    /** Vrai si une tuile GRASS se trouve à ≤ dist d'un chemin. */
    private boolean bordersPath(byte[][] g, int row, int col, int dist) {
        for (int dr = -dist; dr <= dist; dr++) {
            for (int dc = -dist; dc <= dist; dc++) {
                int r = row + dr, c = col + dc;
                if (r < 0 || r >= rows || c < 0 || c >= cols) continue;
                if (g[r][c] == TILE_PATH) return true;
            }
        }
        return false;
    }

    /** Ajoute 4 lacs ovales dans les quadrants. */
    private void addLakes() {
        addLake(cols / 4,       rows / 4,       7, 5);
        addLake(3 * cols / 4,   rows / 4,       6, 4);
        addLake(cols / 4,       3 * rows / 4,   5, 6);
        addLake(3 * cols / 4,   3 * rows / 4,   8, 5);
    }

    private void addLake(int cx, int cy, int rw, int rh) {
        for (int r = cy - rh; r <= cy + rh; r++) {
            for (int c = cx - rw; c <= cx + rw; c++) {
                if (r < 2 || r >= rows - 2 || c < 2 || c >= cols - 2) continue;
                float dx = (float)(c - cx) / rw, dy = (float)(r - cy) / rh;
                if (dx * dx + dy * dy <= 1.0f) grid[r][c] = TILE_WATER;
            }
        }
    }

    /**
     * Pose des forêts dans les zones loin des routes.
     * Moins dense près des routes, plus dense loin.
     */
    private void addForests() {
        // Pré-calculer la distance aux chemins (dilation BFS à 5 tiles)
        boolean[][] nearPath = new boolean[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                nearPath[r][c] = (grid[r][c] == TILE_PATH);

        for (int pass = 0; pass < 5; pass++) {
            boolean[][] next = cloneBoolean(nearPath);
            for (int r = 1; r < rows - 1; r++) {
                for (int c = 1; c < cols - 1; c++) {
                    if (nearPath[r][c]) continue;
                    if (nearPath[r-1][c] || nearPath[r+1][c]
                     || nearPath[r][c-1] || nearPath[r][c+1])
                        next[r][c] = true;
                }
            }
            System.arraycopy(next, 0, nearPath, 0, rows);
            for (int r = 0; r < rows; r++) nearPath[r] = next[r];
        }

        for (int r = 2; r < rows - 2; r++) {
            for (int c = 2; c < cols - 2; c++) {
                if (grid[r][c] != TILE_GRASS && grid[r][c] != TILE_TALL_GRASS) continue;
                if (nearPath[r][c]) continue;

                int h = hash(c, r);
                // Arbre naturel (~11 %)
                boolean isTree = (h % 9 == 0);
                // Clustering : arbre si voisin est arbre
                if (!isTree && h % 3 == 0) {
                    isTree = adjacentTree(r, c);
                }
                if (isTree) grid[r][c] = TILE_TREE;
            }
        }
    }

    private boolean adjacentTree(int r, int c) {
        return tileIs(r-1,c,TILE_TREE) || tileIs(r+1,c,TILE_TREE)
            || tileIs(r,c-1,TILE_TREE) || tileIs(r,c+1,TILE_TREE);
    }

    private boolean tileIs(int r, int c, byte tile) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return false;
        return grid[r][c] == tile;
    }

    /** Rochers épars en pleine herbe. */
    private void addRocks() {
        for (int r = 2; r < rows - 2; r++)
            for (int c = 2; c < cols - 2; c++)
                if (grid[r][c] == TILE_GRASS && hash(c, r) % 55 == 7)
                    grid[r][c] = TILE_ROCK;
    }

    /** Fleurs décoratives sur les tuiles GRASS. */
    private void addFlowers() {
        for (int r = 2; r < rows - 2; r++)
            for (int c = 2; c < cols - 2; c++)
                if (grid[r][c] == TILE_GRASS && hash(c, r) % 12 == 5)
                    grid[r][c] = TILE_FLOWER;
    }

    /** Grande clairière de chemin autour du point de spawn. */
    private void clearSpawn() {
        int radius = 14;
        for (int r = spawnRow - radius; r <= spawnRow + radius; r++) {
            for (int c = spawnCol - radius; c <= spawnCol + radius; c++) {
                if (r < 0 || r >= rows || c < 0 || c >= cols) continue;
                float dx = c - spawnCol, dy = r - spawnRow;
                if (dx * dx + dy * dy <= (float)(radius * radius))
                    grid[r][c] = TILE_PATH;
            }
        }
    }

    /** Bordure infranchissable d'arbres. */
    private void addBorder() {
        for (int r = 0; r < rows; r++) {
            grid[r][0] = grid[r][1] = TILE_TREE;
            grid[r][cols-2] = grid[r][cols-1] = TILE_TREE;
        }
        for (int c = 0; c < cols; c++) {
            grid[0][c] = grid[1][c] = TILE_TREE;
            grid[rows-2][c] = grid[rows-1][c] = TILE_TREE;
        }
    }

    // ── Interface publique ────────────────────────────────────────────────

    public byte getTile(int col, int row) {
        if (col < 0 || row < 0 || col >= cols || row >= rows) return TILE_TREE;
        return grid[row][col];
    }

    public boolean isSolid(float px, float py) {
        int col = (int) Math.floor(px / TILE_SIZE);
        int row = (int) Math.floor(py / TILE_SIZE);
        byte t = getTile(col, row);
        return t == TILE_TREE || t == TILE_WATER || t == TILE_ROCK;
    }

    /** Vrai si la tuile pixel est de l'herbe haute (zone de mobs). */
    public boolean isTallGrass(float px, float py) {
        int col = (int) Math.floor(px / TILE_SIZE);
        int row = (int) Math.floor(py / TILE_SIZE);
        return getTile(col, row) == TILE_TALL_GRASS;
    }

    public float getSpawnPixelX() { return spawnCol * TILE_SIZE + TILE_SIZE / 2f; }
    public float getSpawnPixelY() { return spawnRow * TILE_SIZE + TILE_SIZE / 2f; }
    public float getPixelWidth()  { return cols * TILE_SIZE; }
    public float getPixelHeight() { return rows * TILE_SIZE; }

    // ── Helpers ───────────────────────────────────────────────────────────

    public static int hash(int col, int row) {
        int v = col * 73856093 ^ row * 19349663;
        v ^= (v >>> 13);
        v *= 0x45d9f3b;
        v ^= (v >>> 15);
        return Math.abs(v);
    }

    private byte[][] copyGrid() {
        byte[][] copy = new byte[rows][cols];
        for (int r = 0; r < rows; r++) copy[r] = grid[r].clone();
        return copy;
    }

    private boolean[][] cloneBoolean(boolean[][] src) {
        boolean[][] dst = new boolean[rows][cols];
        for (int r = 0; r < rows; r++) dst[r] = src[r].clone();
        return dst;
    }
}
