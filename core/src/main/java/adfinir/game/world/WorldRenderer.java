package adfinir.game.world;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/**
 * Dessine la carte style Pokémon Gen 1 — esthétique pixel art vectoriel.
 *
 * Principes visuels :
 *  - Couleurs franches et saturées (pas de dégradés)
 *  - Arbres bloquants rectangulaires et géométriques
 *  - Chemins sableux avec grille visible
 *  - Eau bleue avec reflets horizontaux
 *  - Herbe haute avec "brins" dessinés
 */
public class WorldRenderer {

    private final WorldMap map;

    // ── Palette Pokémon ───────────────────────────────────────────────────

    // Herbe normale
    private static final float[] G_BASE = { 0.35f, 0.72f, 0.24f };
    private static final float[] G_DARK = { 0.28f, 0.60f, 0.18f };
    // Chemin (sable/terre)
    private static final float[] P_BASE = { 0.74f, 0.62f, 0.40f };
    private static final float[] P_DARK = { 0.60f, 0.50f, 0.30f };
    // Herbe haute
    private static final float[] TG_BASE = { 0.20f, 0.54f, 0.15f };
    private static final float[] TG_DARK = { 0.14f, 0.40f, 0.10f };
    // Arbre
    private static final float[] T_EDGE  = { 0.08f, 0.32f, 0.06f };
    private static final float[] T_MID   = { 0.14f, 0.52f, 0.10f };
    private static final float[] T_HIGH  = { 0.22f, 0.70f, 0.16f };
    private static final float[] T_LIGHT = { 0.30f, 0.82f, 0.22f };
    private static final float[] T_SHADE = { 0.06f, 0.22f, 0.04f };
    // Eau
    private static final float[] W_BASE  = { 0.22f, 0.50f, 0.90f };
    private static final float[] W_WAVE  = { 0.35f, 0.65f, 0.98f };
    private static final float[] W_DARK  = { 0.14f, 0.35f, 0.72f };
    // Fleur
    private static final float[][] FLOWER_COLS = {
        { 0.98f, 0.92f, 0.20f }, // jaune
        { 0.95f, 0.35f, 0.65f }, // rose
        { 0.95f, 0.95f, 0.95f }, // blanc
        { 0.55f, 0.75f, 0.98f }, // bleu clair
    };
    // Rocher
    private static final float[] R_BASE  = { 0.52f, 0.50f, 0.44f };
    private static final float[] R_HIGH  = { 0.68f, 0.65f, 0.58f };
    private static final float[] R_DARK  = { 0.32f, 0.30f, 0.26f };

    public WorldRenderer(WorldMap map) { this.map = map; }

    /**
     * Appeler entre shapeRenderer.begin(Filled) et end().
     */
    public void render(ShapeRenderer sr, Rectangle vp) {
        int ts = WorldMap.TILE_SIZE;
        int c0 = Math.max(0, (int) Math.floor(vp.x / ts) - 1);
        int r0 = Math.max(0, (int) Math.floor(vp.y / ts) - 1);
        int c1 = Math.min(map.cols - 1, (int) Math.ceil((vp.x + vp.width)  / ts) + 1);
        int r1 = Math.min(map.rows - 1, (int) Math.ceil((vp.y + vp.height) / ts) + 1);

        // Passe 1 — sol
        for (int row = r0; row <= r1; row++) {
            for (int col = c0; col <= c1; col++) {
                float px = col * ts, py = row * ts;
                switch (map.getTile(col, row)) {
                    case WorldMap.TILE_GRASS:      drawGrass(sr, px, py, ts, col, row); break;
                    case WorldMap.TILE_PATH:       drawPath(sr, px, py, ts, col, row);  break;
                    case WorldMap.TILE_TALL_GRASS: drawTallGrass(sr, px, py, ts, col, row); break;
                    case WorldMap.TILE_FLOWER:     drawFlower(sr, px, py, ts, col, row); break;
                    case WorldMap.TILE_WATER:      drawGrass(sr, px, py, ts, col, row); // fond sous l'eau
                }
            }
        }
        // Passe 2 — eau (séparée pour ne pas écraser le sol sous les rives)
        for (int row = r0; row <= r1; row++) {
            for (int col = c0; col <= c1; col++) {
                byte t = map.getTile(col, row);
                float px = col * ts, py = row * ts;
                if (t == WorldMap.TILE_WATER) drawWater(sr, px, py, ts, col, row);
            }
        }
        // Passe 3 — obstacles élevés (arbres, rochers) — après le sol
        for (int row = r0; row <= r1; row++) {
            for (int col = c0; col <= c1; col++) {
                byte t = map.getTile(col, row);
                float px = col * ts, py = row * ts;
                if (t == WorldMap.TILE_TREE) drawTree(sr, px, py, ts);
                else if (t == WorldMap.TILE_ROCK) drawRock(sr, px, py, ts, col, row);
            }
        }
    }

    // ── Herbe ─────────────────────────────────────────────────────────────

    private void drawGrass(ShapeRenderer sr, float px, float py, int ts, int col, int row) {
        int h = WorldMap.hash(col, row);
        sr.setColor(h % 4 == 0 ? G_DARK[0] : G_BASE[0],
                    h % 4 == 0 ? G_DARK[1] : G_BASE[1],
                    h % 4 == 0 ? G_DARK[2] : G_BASE[2], 1f);
        sr.rect(px, py, ts, ts);
        // Grille de tiles (look pixel-art)
        sr.setColor(G_DARK[0], G_DARK[1], G_DARK[2], 1f);
        sr.rect(px, py, ts, 1f);
        sr.rect(px, py, 1f, ts);
    }

    // ── Chemin ────────────────────────────────────────────────────────────

    private void drawPath(ShapeRenderer sr, float px, float py, int ts, int col, int row) {
        sr.setColor(P_BASE[0], P_BASE[1], P_BASE[2], 1f);
        sr.rect(px, py, ts, ts);
        // Bordure plus sombre (aspect pavé)
        sr.setColor(P_DARK[0], P_DARK[1], P_DARK[2], 1f);
        sr.rect(px, py, ts, 1f);
        sr.rect(px, py, 1f, ts);
        // Petites irrégularités de texture
        int h = WorldMap.hash(col, row);
        if (h % 7 == 0) {
            sr.setColor(P_DARK[0], P_DARK[1], P_DARK[2], 1f);
            sr.rect(px + (h % 10) + 2, py + ((h >> 3) % 10) + 2, 2.5f, 2.5f);
        }
    }

    // ── Herbe haute ───────────────────────────────────────────────────────

    private void drawTallGrass(ShapeRenderer sr, float px, float py, int ts, int col, int row) {
        sr.setColor(TG_BASE[0], TG_BASE[1], TG_BASE[2], 1f);
        sr.rect(px, py, ts, ts);
        // Brins d'herbe (style Pokémon)
        sr.setColor(TG_DARK[0], TG_DARK[1], TG_DARK[2], 1f);
        sr.rect(px + 3,  py + 4,  2, 8);  // brin gauche
        sr.rect(px + 2,  py + 9,  3, 5);  // ramification gauche
        sr.rect(px + 11, py + 4,  2, 8);  // brin droit
        sr.rect(px + 10, py + 9,  3, 5);  // ramification droite
        sr.rect(px + 7,  py + 3,  2, 9);  // brin central
        sr.rect(px + 6,  py + 10, 3, 4);  // ramification centrale
    }

    // ── Fleur ─────────────────────────────────────────────────────────────

    private void drawFlower(ShapeRenderer sr, float px, float py, int ts, int col, int row) {
        // Fond herbe sous la fleur
        sr.setColor(G_BASE[0], G_BASE[1], G_BASE[2], 1f);
        sr.rect(px, py, ts, ts);
        sr.setColor(G_DARK[0], G_DARK[1], G_DARK[2], 1f);
        sr.rect(px, py, ts, 1f);
        sr.rect(px, py, 1f, ts);
        // Fleur
        int h = WorldMap.hash(col, row);
        float[] fc = FLOWER_COLS[h % FLOWER_COLS.length];
        float ox = (h % 8) + 3f, oy = ((h >> 3) % 8) + 3f;
        // Pétales (4 petits rectangles)
        sr.setColor(fc[0], fc[1], fc[2], 1f);
        sr.rect(px + ox - 1, py + oy + 2,  5, 2);  // haut/bas
        sr.rect(px + ox + 2, py + oy - 1,  2, 5);  // gauche/droite
        // Centre jaune
        sr.setColor(1f, 0.90f, 0.25f, 1f);
        sr.rect(px + ox + 1, py + oy + 1, 2, 2);
    }

    // ── Eau ───────────────────────────────────────────────────────────────

    private void drawWater(ShapeRenderer sr, float px, float py, int ts, int col, int row) {
        sr.setColor(W_BASE[0], W_BASE[1], W_BASE[2], 1f);
        sr.rect(px, py, ts, ts);
        // Bordure sombre sur les bords adjacents à la terre
        if (!isTileWater(col, row - 1)) { sr.setColor(W_DARK[0], W_DARK[1], W_DARK[2], 1f); sr.rect(px, py, ts, 2f); }
        if (!isTileWater(col, row + 1)) { sr.setColor(W_DARK[0], W_DARK[1], W_DARK[2], 1f); sr.rect(px, py + ts - 2, ts, 2f); }
        if (!isTileWater(col - 1, row)) { sr.setColor(W_DARK[0], W_DARK[1], W_DARK[2], 1f); sr.rect(px, py, 2f, ts); }
        if (!isTileWater(col + 1, row)) { sr.setColor(W_DARK[0], W_DARK[1], W_DARK[2], 1f); sr.rect(px + ts - 2, py, 2f, ts); }
        // Reflets (lignes horizontales décalées selon position)
        int wOff = WorldMap.hash(col, row) % 4;
        sr.setColor(W_WAVE[0], W_WAVE[1], W_WAVE[2], 1f);
        sr.rect(px + 2, py + wOff + 3,  ts - 4, 2f);
        sr.rect(px + 2, py + wOff + 10, ts - 4, 2f);
    }

    private boolean isTileWater(int col, int row) {
        return map.getTile(col, row) == WorldMap.TILE_WATER;
    }

    // ── Arbre (Pokémon — style bloc géométrique) ──────────────────────────

    private void drawTree(ShapeRenderer sr, float px, float py, int ts) {
        // Base (bordure sombre)
        sr.setColor(T_EDGE[0],  T_EDGE[1],  T_EDGE[2],  1f); sr.rect(px, py, ts, ts);
        // Corps principal
        sr.setColor(T_MID[0],   T_MID[1],   T_MID[2],   1f); sr.rect(px+1, py+1, ts-2, ts-2);
        // Partie haute plus claire
        sr.setColor(T_HIGH[0],  T_HIGH[1],  T_HIGH[2],  1f); sr.rect(px+2, py+5, ts-4, ts-7);
        // Ombre centrale (trou sombre — look buisson Pokémon)
        sr.setColor(T_SHADE[0], T_SHADE[1], T_SHADE[2],  1f); sr.rect(px+4, py+4, ts-8, ts-9);
        // Reflet clair haut-gauche
        sr.setColor(T_LIGHT[0], T_LIGHT[1], T_LIGHT[2],  1f); sr.rect(px+4, py+8,  5, 3);
    }

    // ── Rocher ────────────────────────────────────────────────────────────

    private void drawRock(ShapeRenderer sr, float px, float py, int ts, int col, int row) {
        // Corps
        sr.setColor(R_DARK[0], R_DARK[1], R_DARK[2], 1f); sr.rect(px+1, py+1, ts-2, ts-4);
        sr.setColor(R_BASE[0], R_BASE[1], R_BASE[2], 1f); sr.rect(px+2, py+2, ts-4, ts-6);
        // Reflet haut-gauche
        sr.setColor(R_HIGH[0], R_HIGH[1], R_HIGH[2], 1f); sr.rect(px+4, py+7, 5, 2);
        // Ombre en bas
        sr.setColor(R_DARK[0], R_DARK[1], R_DARK[2], 1f); sr.rect(px+2, py+1, ts-4, 2);
    }

    public void dispose() {}
}
