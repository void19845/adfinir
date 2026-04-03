package adfinir.game.dungeon;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/**
 * Dessine la carte du donjon avec ShapeRenderer (aucun asset image).
 * Biome 1 — Forêt souterraine : sol de pierre moussu, murs de pierre noire,
 * torches orange, piliers aux angles, racines sur les murs.
 */
public class DungeonRenderer {

    private final DungeonMap map;

    // ── Palette biome Forêt ────────────────────────────────────────────────
    // Sol (salle)
    private static final float[] FLOOR_BASE  = { 0.20f, 0.28f, 0.13f, 1f };
    private static final float[] FLOOR_MOSS  = { 0.16f, 0.23f, 0.10f, 1f };
    private static final float[] FLOOR_CRACK = { 0.14f, 0.20f, 0.08f, 1f };
    // Couloir
    private static final float[] CORR_BASE   = { 0.14f, 0.18f, 0.09f, 1f };
    private static final float[] CORR_DARK   = { 0.10f, 0.13f, 0.06f, 1f };
    // Mur
    private static final float[] WALL_BASE   = { 0.10f, 0.09f, 0.12f, 1f };
    private static final float[] WALL_FACE   = { 0.17f, 0.16f, 0.21f, 1f };
    private static final float[] WALL_SHADOW = { 0.06f, 0.06f, 0.08f, 1f };
    private static final float[] WALL_ROOT   = { 0.22f, 0.14f, 0.06f, 1f };
    // Torche
    private static final float[] TORCH_WOOD  = { 0.38f, 0.24f, 0.10f, 1f };
    private static final float[] TORCH_OUTER = { 0.92f, 0.52f, 0.08f, 1f };
    private static final float[] TORCH_INNER = { 1.00f, 0.88f, 0.38f, 1f };
    // Pilier
    private static final float[] PILLAR      = { 0.22f, 0.20f, 0.27f, 1f };
    private static final float[] PILLAR_TOP  = { 0.30f, 0.28f, 0.36f, 1f };

    public DungeonRenderer(DungeonMap map) {
        this.map = map;
    }

    /**
     * Dessine les tuiles visibles dans le viewport.
     * Doit être appelé entre shapeRenderer.begin(Filled) et end().
     */
    public void render(ShapeRenderer sr, Rectangle viewport) {
        int ts = DungeonMap.TILE_SIZE;

        int colMin = Math.max(0,            (int) Math.floor(viewport.x / ts));
        int rowMin = Math.max(0,            (int) Math.floor(viewport.y / ts));
        int colMax = Math.min(map.cols - 1, (int) Math.ceil((viewport.x + viewport.width)  / ts));
        int rowMax = Math.min(map.rows - 1, (int) Math.ceil((viewport.y + viewport.height) / ts));

        // Passe 1 : sol et murs (fond)
        for (int row = rowMin; row <= rowMax; row++) {
            for (int col = colMin; col <= colMax; col++) {
                int tile = map.getTile(col, row);
                float px  = col * ts;
                float py  = row * ts;

                switch (tile) {
                    case DungeonMap.TILE_FLOOR:
                        drawFloor(sr, px, py, ts, col, row);
                        break;
                    case DungeonMap.TILE_CORRIDOR:
                        drawCorridor(sr, px, py, ts, col, row);
                        break;
                    case DungeonMap.TILE_WALL:
                        drawWall(sr, px, py, ts, col, row);
                        break;
                    default:
                        // Vide : noir absolu (déjà la couleur de clear)
                        break;
                }
            }
        }

        // Passe 2 : décorations (torches, piliers, racines) par-dessus
        for (int row = rowMin; row <= rowMax; row++) {
            for (int col = colMin; col <= colMax; col++) {
                int tile = map.getTile(col, row);
                float px  = col * ts;
                float py  = row * ts;

                if (tile == DungeonMap.TILE_WALL) {
                    drawWallDecor(sr, px, py, ts, col, row);
                }
            }
        }
    }

    // ── Dalles de salle ───────────────────────────────────────────────────

    private void drawFloor(ShapeRenderer sr, float px, float py, int ts, int col, int row) {
        // Base verte-brune
        sr.setColor(FLOOR_BASE[0], FLOOR_BASE[1], FLOOR_BASE[2], 1f);
        sr.rect(px, py, ts, ts);

        // Grille de pierre : fins séparateurs sombres
        sr.setColor(FLOOR_CRACK[0], FLOOR_CRACK[1], FLOOR_CRACK[2], 1f);
        sr.rect(px, py, ts, 1f);              // bas
        sr.rect(px, py, 1f, ts);              // gauche

        // Patch de mousse déterministe
        int h = hash(col, row);
        if (h % 5 == 0) {
            sr.setColor(FLOOR_MOSS[0], FLOOR_MOSS[1], FLOOR_MOSS[2], 1f);
            float ox = (h % 7) + 2f;
            float oy = ((h >> 3) % 7) + 2f;
            sr.rect(px + ox, py + oy, 5f, 3f);
        }
        if (h % 7 == 3) {
            sr.setColor(FLOOR_MOSS[0], FLOOR_MOSS[1], FLOOR_MOSS[2], 1f);
            float ox = (h % 9) + 1f;
            float oy = ((h >> 4) % 9) + 1f;
            sr.ellipse(px + ox, py + oy, 4f, 2.5f, 6);
        }
    }

    // ── Couloir ───────────────────────────────────────────────────────────

    private void drawCorridor(ShapeRenderer sr, float px, float py, int ts, int col, int row) {
        sr.setColor(CORR_BASE[0], CORR_BASE[1], CORR_BASE[2], 1f);
        sr.rect(px, py, ts, ts);

        // Léger assombrissement sur les bords du couloir
        sr.setColor(CORR_DARK[0], CORR_DARK[1], CORR_DARK[2], 1f);
        sr.rect(px, py, ts, 2f);
        sr.rect(px, py + ts - 2f, ts, 2f);
    }

    // ── Tuile de mur ──────────────────────────────────────────────────────

    private void drawWall(ShapeRenderer sr, float px, float py, int ts, int col, int row) {
        // Base sombre
        sr.setColor(WALL_BASE[0], WALL_BASE[1], WALL_BASE[2], 1f);
        sr.rect(px, py, ts, ts);

        // Face supérieure (surface visible de la pierre)
        sr.setColor(WALL_FACE[0], WALL_FACE[1], WALL_FACE[2], 1f);
        sr.rect(px, py + ts - 4f, ts, 4f);

        // Ombre inférieure
        sr.setColor(WALL_SHADOW[0], WALL_SHADOW[1], WALL_SHADOW[2], 1f);
        sr.rect(px, py, ts, 2f);

        // Séparateur vertical (joint de pierre)
        int h = hash(col, row);
        if (h % 3 == 0) {
            sr.setColor(WALL_SHADOW[0], WALL_SHADOW[1], WALL_SHADOW[2], 1f);
            sr.rect(px + ts / 2f, py + 3f, 1f, ts - 7f);
        }
    }

    // ── Décorations sur les murs ──────────────────────────────────────────

    private void drawWallDecor(ShapeRenderer sr, float px, float py, int ts, int col, int row) {
        boolean floorBelow = map.isFloorLike(map.getTile(col, row - 1));
        boolean floorAbove = map.isFloorLike(map.getTile(col, row + 1));
        boolean floorLeft  = map.isFloorLike(map.getTile(col - 1, row));
        boolean floorRight = map.isFloorLike(map.getTile(col + 1, row));

        int h = hash(col, row);

        // Torche : mur avec sol au-dessus, toutes les ~11 colonnes
        if (floorAbove && h % 11 == 0) {
            drawTorch(sr, px + ts / 2f - 1f, py + ts - 7f);
        }

        // Pilier aux angles intérieurs (mur entouré de sol sur deux côtés)
        if (floorBelow && floorRight) {
            drawPillar(sr, px + ts - 4f, py, 4f);
        }
        if (floorBelow && floorLeft) {
            drawPillar(sr, px, py, 4f);
        }

        // Racine/vigne sur les murs adjacents au sol
        if (floorBelow && h % 7 == 2) {
            sr.setColor(WALL_ROOT[0], WALL_ROOT[1], WALL_ROOT[2], 1f);
            float rx = px + (h % 11) + 1f;
            sr.rect(rx, py + 2f, 1.5f, 5f);
            sr.rect(rx - 1f, py + 5f, 3f, 1.5f);
        }
    }

    private void drawTorch(ShapeRenderer sr, float cx, float baseY) {
        // Manche en bois
        sr.setColor(TORCH_WOOD[0], TORCH_WOOD[1], TORCH_WOOD[2], 1f);
        sr.rect(cx, baseY, 2f, 4f);
        // Flamme extérieure
        sr.setColor(TORCH_OUTER[0], TORCH_OUTER[1], TORCH_OUTER[2], 1f);
        sr.rect(cx - 1f, baseY + 3.5f, 4f, 3f);
        // Flamme intérieure
        sr.setColor(TORCH_INNER[0], TORCH_INNER[1], TORCH_INNER[2], 1f);
        sr.rect(cx, baseY + 4.5f, 2f, 2f);
    }

    private void drawPillar(ShapeRenderer sr, float px, float py, float size) {
        sr.setColor(PILLAR[0], PILLAR[1], PILLAR[2], 1f);
        sr.rect(px, py, size, size);
        sr.setColor(PILLAR_TOP[0], PILLAR_TOP[1], PILLAR_TOP[2], 1f);
        sr.rect(px, py + size - 1.5f, size, 1.5f);
    }

    // ── Utilitaire ────────────────────────────────────────────────────────

    /** Hash déterministe à partir de la position de tuile. */
    private static int hash(int col, int row) {
        int v = col * 73856093 ^ row * 19349663;
        v ^= (v >>> 13);
        v *= 0x45d9f3b;
        v ^= (v >>> 15);
        return Math.abs(v);
    }

    public void dispose() {
        // Rien à libérer : aucun asset chargé
    }
}
