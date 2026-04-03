package adfinir.game.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Génère un donjon procédural via BSP (Binary Space Partitioning).
 *
 * Algorithme :
 *  1. On part d'une grande partition couvrant toute la carte.
 *  2. On la divise récursivement en deux (horizontal ou vertical).
 *  3. Dans chaque partition feuille, on place une salle aléatoire.
 *  4. On relie les salles sœurs avec des couloirs en L.
 */
public class DungeonGenerator {

    // ---------------------------------------------------------------
    // Paramètres de génération
    // ---------------------------------------------------------------
    private static final int MIN_PARTITION_SIZE = 8;   // tiles
    private static final int MIN_ROOM_SIZE      = 4;   // tiles
    private static final int ROOM_PADDING       = 1;   // espace entre salle et bord de partition

    private final int cols;
    private final int rows;
    private final Random rng;

    private int[][] grid;

    // Position de spawn du joueur (centre de la première salle)
    private int spawnCol;
    private int spawnRow;

    public DungeonGenerator(int cols, int rows, long seed) {
        this.cols = cols;
        this.rows = rows;
        this.rng  = new Random(seed);
    }

    public DungeonGenerator(int cols, int rows) {
        this(cols, rows, System.currentTimeMillis());
    }

    // ---------------------------------------------------------------
    // Point d'entrée public
    // ---------------------------------------------------------------

    public DungeonMap generate() {
        // Initialise tout en murs
        grid = new int[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                grid[r][c] = DungeonMap.TILE_WALL;

        // BSP
        Partition root = new Partition(1, 1, cols - 2, rows - 2);
        split(root, 0);
        buildRooms(root);
        connectPartitions(root);

        // Spawn = centre de la première feuille trouvée
        Partition firstLeaf = getFirstLeaf(root);
        if (firstLeaf != null && firstLeaf.room != null) {
            spawnCol = firstLeaf.room.cx();
            spawnRow = firstLeaf.room.cy();
        } else {
            spawnCol = cols / 2;
            spawnRow = rows / 2;
        }

        return new DungeonMap(grid, spawnCol, spawnRow);
    }

    public int getSpawnCol() { return spawnCol; }
    public int getSpawnRow() { return spawnRow; }

    // ---------------------------------------------------------------
    // BSP : division récursive
    // ---------------------------------------------------------------

    private void split(Partition p, int depth) {
        if (depth > 5) return; // profondeur max

        boolean canSplitH = p.h >= MIN_PARTITION_SIZE * 2;
        boolean canSplitV = p.w >= MIN_PARTITION_SIZE * 2;

        if (!canSplitH && !canSplitV) return;

        boolean splitHorizontal;
        if (canSplitH && canSplitV) {
            splitHorizontal = rng.nextBoolean();
        } else {
            splitHorizontal = canSplitH;
        }

        if (splitHorizontal) {
            // Coupe horizontale : divise en haut/bas
            int minCut = p.y + MIN_PARTITION_SIZE;
            int maxCut = p.y + p.h - MIN_PARTITION_SIZE;
            if (minCut >= maxCut) return;
            int cut = rng.nextInt(maxCut - minCut) + minCut;
            p.left  = new Partition(p.x, p.y, p.w, cut - p.y);
            p.right = new Partition(p.x, cut, p.w, p.y + p.h - cut);
        } else {
            // Coupe verticale : divise en gauche/droite
            int minCut = p.x + MIN_PARTITION_SIZE;
            int maxCut = p.x + p.w - MIN_PARTITION_SIZE;
            if (minCut >= maxCut) return;
            int cut = rng.nextInt(maxCut - minCut) + minCut;
            p.left  = new Partition(p.x, p.y, cut - p.x, p.h);
            p.right = new Partition(cut, p.y, p.x + p.w - cut, p.h);
        }

        split(p.left,  depth + 1);
        split(p.right, depth + 1);
    }

    // ---------------------------------------------------------------
    // Placement des salles dans les feuilles
    // ---------------------------------------------------------------

    private void buildRooms(Partition p) {
        if (p.isLeaf()) {
            // Taille de la salle : aléatoire dans les limites de la partition
            int maxW = p.w - ROOM_PADDING * 2;
            int maxH = p.h - ROOM_PADDING * 2;
            if (maxW < MIN_ROOM_SIZE || maxH < MIN_ROOM_SIZE) return;

            int rw = rng.nextInt(maxW - MIN_ROOM_SIZE + 1) + MIN_ROOM_SIZE;
            int rh = rng.nextInt(maxH - MIN_ROOM_SIZE + 1) + MIN_ROOM_SIZE;

            // Position aléatoire dans la partition (avec padding)
            int rx = p.x + ROOM_PADDING + rng.nextInt(p.w - rw - ROOM_PADDING * 2 + 1);
            int ry = p.y + ROOM_PADDING + rng.nextInt(p.h - rh - ROOM_PADDING * 2 + 1);

            p.room = new Room(rx, ry, rw, rh);
            carveRoom(p.room);
        } else {
            if (p.left  != null) buildRooms(p.left);
            if (p.right != null) buildRooms(p.right);
        }
    }

    private void carveRoom(Room room) {
        for (int r = room.y; r < room.y + room.h; r++)
            for (int c = room.x; c < room.x + room.w; c++)
                if (r >= 0 && r < rows && c >= 0 && c < cols)
                    grid[r][c] = DungeonMap.TILE_FLOOR;
    }

    // ---------------------------------------------------------------
    // Connexion des salles (couloirs en L)
    // ---------------------------------------------------------------

    private void connectPartitions(Partition p) {
        if (p.isLeaf()) return;

        connectPartitions(p.left);
        connectPartitions(p.right);

        // Récupère le centre d'une salle dans chaque sous-arbre
        Room roomA = getAnyRoom(p.left);
        Room roomB = getAnyRoom(p.right);

        if (roomA != null && roomB != null) {
            carveCorridor(roomA.cx(), roomA.cy(), roomB.cx(), roomB.cy());
        }
    }

    /**
     * Couloir en L : horizontal puis vertical (ou l'inverse selon le RNG).
     */
    private void carveCorridor(int x1, int y1, int x2, int y2) {
        if (rng.nextBoolean()) {
            carveHCorridor(x1, x2, y1);
            carveVCorridor(y1, y2, x2);
        } else {
            carveVCorridor(y1, y2, x1);
            carveHCorridor(x1, x2, y2);
        }
    }

    private void carveHCorridor(int x1, int x2, int y) {
        int from = Math.min(x1, x2);
        int to   = Math.max(x1, x2);
        for (int c = from; c <= to; c++)
            if (c >= 0 && c < cols && y >= 0 && y < rows)
                if (grid[y][c] == DungeonMap.TILE_WALL)
                    grid[y][c] = DungeonMap.TILE_CORRIDOR;
    }

    private void carveVCorridor(int y1, int y2, int x) {
        int from = Math.min(y1, y2);
        int to   = Math.max(y1, y2);
        for (int r = from; r <= to; r++)
            if (r >= 0 && r < rows && x >= 0 && x < cols)
                if (grid[r][x] == DungeonMap.TILE_WALL)
                    grid[r][x] = DungeonMap.TILE_CORRIDOR;
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Room getAnyRoom(Partition p) {
        if (p == null) return null;
        if (p.isLeaf()) return p.room;
        Room r = getAnyRoom(p.left);
        return (r != null) ? r : getAnyRoom(p.right);
    }

    private Partition getFirstLeaf(Partition p) {
        if (p == null) return null;
        if (p.isLeaf()) return p;
        Partition l = getFirstLeaf(p.left);
        return (l != null) ? l : getFirstLeaf(p.right);
    }

    // ---------------------------------------------------------------
    // Structures internes
    // ---------------------------------------------------------------

    private static class Partition {
        int x, y, w, h;
        Partition left, right;
        Room room;

        Partition(int x, int y, int w, int h) {
            this.x = x; this.y = y;
            this.w = w; this.h = h;
        }

        boolean isLeaf() { return left == null && right == null; }
    }

    static class Room {
        int x, y, w, h;

        Room(int x, int y, int w, int h) {
            this.x = x; this.y = y;
            this.w = w; this.h = h;
        }

        int cx() { return x + w / 2; }
        int cy() { return y + h / 2; }
    }
}
