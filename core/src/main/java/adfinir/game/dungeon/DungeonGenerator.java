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
            for (int t = 0; t < CORRIDOR_WIDTH; t++) {
                int r = y + t;
                if (c >= 0 && c < cols && r >= 0 && r < rows)
                    grid[r][c] = DungeonMap.TILE_FLOOR;
            }
    }

    private void carveVCorridor(int y1, int y2, int x) {
        int from = Math.min(y1, y2);
        int to   = Math.max(y1, y2);
        for (int r = from; r <= to; r++)
            for (int t = 0; t < CORRIDOR_WIDTH; t++) {
                int c = x + t;
                if (r >= 0 && r < rows && c >= 0 && c < cols)
                    grid[r][c] = DungeonMap.TILE_FLOOR;
            }
    }

    // ---------------------------------------------------------------
    // Séparation des couloirs
    // ---------------------------------------------------------------

    /**
     * S'assure qu'il y a au moins CORRIDOR_SPACING tile(s) de mur entre
     * deux couloirs parallèles qui ne font pas partie de la même salle.
     *
     * Stratégie : on scanne chaque tile de sol. Si elle est adjacente à une
     * tile de sol dans une direction ET que cette voisine n'est pas dans
     * la même "bande" continue (i.e. il n'y a pas de connexion perpendiculaire),
     * on ne touche pas à la salle — on laisse les salles intactes et on
     * concentre l'effort sur les couloirs isolés.
     *
     * Implémentation simplifiée : on crée une copie de la grille et on
     * repasse les couloirs creusés en forçant un mur entre deux bandes
     * de sol séparées par moins de CORRIDOR_SPACING.
     */
    private void enforceCorridorSpacing() {
        // Parcourt toutes les tiles. Si une tile de sol a une voisine de sol
        // à exactement CORRIDOR_SPACING+1 de distance dans la même direction
        // (sans connexion entre elles), on insère un mur entre les deux.
        // On travaille sur une copie pour éviter les effets de bord.
        int[][] copy = new int[rows][cols];
        for (int r = 0; r < rows; r++)
            copy[r] = grid[r].clone();

        int gap = CORRIDOR_SPACING; // nombre de murs requis entre deux couloirs

        // --- Vérification horizontale : deux bandes horizontales trop proches ---
        for (int r = 1; r < rows - 1 - gap; r++) {
            for (int c = 1; c < cols - 1; c++) {
                if (grid[r][c] == DungeonMap.TILE_FLOOR) {
                    // Regarde si dans gap+1 tiles vers le bas il y a du sol
                    // ET que la colonne intermédiaire est aussi du sol (couloir adjacent)
                    boolean allFloorBelow = true;
                    for (int k = 1; k <= gap; k++) {
                        if (grid[r + k][c] != DungeonMap.TILE_FLOOR) { allFloorBelow = false; break; }
                    }
                    // Si toutes les tiles intermédiaires sont déjà du sol, pas de problème
                    // On cherche le cas où elles sont des murs (couloirs séparés par gap=0)
                    if (!allFloorBelow && grid[r + gap + 1][c] == DungeonMap.TILE_FLOOR) {
                        // Il y a un mur entre les deux — c'est OK, rien à faire
                    }
                    // Cas problématique : gap = 0, les deux couloirs se touchent directement
                    if (gap == 0 && grid[r + 1][c] == DungeonMap.TILE_FLOOR) {
                        // Vérifie que ce n'est pas une salle (connexion sur plusieurs colonnes)
                        boolean isRoom = (c > 0 && grid[r][c-1] == DungeonMap.TILE_FLOOR
                            && grid[r+1][c-1] == DungeonMap.TILE_FLOOR);
                        if (!isRoom) copy[r + 1][c] = DungeonMap.TILE_WALL;
                    }
                }
            }
        }

        // Recopie dans grid
        for (int r = 0; r < rows; r++)
            grid[r] = copy[r].clone();
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
