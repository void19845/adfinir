package adfinir.game.ecs.systems;

import adfinir.game.ecs.components.RenderComponent;
import adfinir.game.ecs.components.TransformComponent;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Dessine chaque entité avec des formes vectorielles (ShapeRenderer).
 * Aucun asset image requis.
 *
 * PLAYER_1 — Chevalier en armure bleue acier / or
 * PLAYER_2 — Rôdeuse de forêt en vert / bronze
 * MOB      — Créature maudite en cramoisi avec yeux dorés
 */
public class RenderSystem extends IteratingSystem {

    private final ComponentMapper<TransformComponent> tm = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<RenderComponent>    rm = ComponentMapper.getFor(RenderComponent.class);

    private final ShapeRenderer sr;

    public RenderSystem(ShapeRenderer sr) {
        super(Family.all(TransformComponent.class, RenderComponent.class).get(), 10);
        this.sr = sr;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent pos    = tm.get(entity);
        RenderComponent    render = rm.get(entity);

        float cx = pos.x;
        float cy = pos.y;
        float w  = render.width;
        float h  = render.height;
        float lx = cx - w / 2f;
        float ly = cy - h / 2f;

        switch (render.type) {
            case PLAYER_1: drawKnight(cx, cy, lx, ly, w, h, false); break;
            case PLAYER_2: drawKnight(cx, cy, lx, ly, w, h, true);  break;
            case MOB:      drawMob(cx, cy, lx, ly, w, h);            break;
        }
    }

    // ── Chevalier (P1 = bleu, P2 = vert) ─────────────────────────────────

    private void drawKnight(float cx, float cy, float lx, float ly,
                             float w, float h, boolean alt) {
        // Couleurs : PLAYER_1 = bleu/or, PLAYER_2 = vert/bronze
        float[] bodyColor  = alt ? new float[]{0.18f, 0.48f, 0.22f} : new float[]{0.22f, 0.42f, 0.78f};
        float[] darkBody   = alt ? new float[]{0.11f, 0.30f, 0.13f} : new float[]{0.12f, 0.22f, 0.48f};
        float[] trimColor  = alt ? new float[]{0.60f, 0.42f, 0.15f} : new float[]{0.80f, 0.65f, 0.18f};
        float[] helmColor  = alt ? new float[]{0.13f, 0.35f, 0.15f} : new float[]{0.15f, 0.28f, 0.58f};
        float[] skinColor  = new float[]{0.87f, 0.73f, 0.57f};
        float[] visorColor = alt ? new float[]{0.06f, 0.15f, 0.07f} : new float[]{0.07f, 0.12f, 0.28f};

        // Ombre au sol
        sr.setColor(0.08f, 0.10f, 0.04f, 1f);
        sr.ellipse(cx - 5f, ly, 10f, 3f, 8);

        // Bottes
        sr.setColor(darkBody[0], darkBody[1], darkBody[2], 1f);
        sr.rect(lx + 2f, ly, w - 4f, 4f);

        // Corps / armure (torse)
        sr.setColor(bodyColor[0], bodyColor[1], bodyColor[2], 1f);
        sr.rect(lx + 2f, ly + 3f, w - 4f, 7f);

        // Épaulières
        sr.setColor(bodyColor[0] + 0.08f, bodyColor[1] + 0.08f, bodyColor[2] + 0.08f, 1f);
        sr.rect(lx,         ly + 7f, 3f, 3f);
        sr.rect(lx + w - 3f, ly + 7f, 3f, 3f);

        // Lisière dorée (pectoral)
        sr.setColor(trimColor[0], trimColor[1], trimColor[2], 1f);
        sr.rect(lx + 3f, ly + 8.5f, w - 6f, 1.5f);

        // Peau (visage)
        sr.setColor(skinColor[0], skinColor[1], skinColor[2], 1f);
        sr.circle(cx, ly + 13f, 3.2f, 8);

        // Casque
        sr.setColor(helmColor[0], helmColor[1], helmColor[2], 1f);
        sr.rect(lx + 3f, ly + 11.5f, w - 6f, 4f);

        // Visière (fente sombre)
        sr.setColor(visorColor[0], visorColor[1], visorColor[2], 1f);
        sr.rect(lx + 4f, ly + 13f, w - 8f, 1.5f);

        // Reflet casque
        sr.setColor(trimColor[0], trimColor[1], trimColor[2], 1f);
        sr.rect(lx + 3f, ly + 14.5f, 4f, 1f);
    }

    // ── Mob — créature maudite ────────────────────────────────────────────

    private void drawMob(float cx, float cy, float lx, float ly, float w, float h) {
        // Ombre
        sr.setColor(0.12f, 0.02f, 0.02f, 1f);
        sr.ellipse(cx - 5f, ly, 10f, 3f, 8);

        // Corps (manteau cramoisi)
        sr.setColor(0.38f, 0.06f, 0.06f, 1f);
        sr.rect(lx + 2f, ly, w - 4f, h - 6f);

        // Flancs (effet capuche)
        sr.setColor(0.28f, 0.04f, 0.04f, 1f);
        sr.rect(lx,         ly + 2f, 3f, h - 8f);
        sr.rect(lx + w - 3f, ly + 2f, 3f, h - 8f);

        // Tête
        sr.setColor(0.45f, 0.10f, 0.10f, 1f);
        sr.circle(cx, ly + h - 4f, 4f, 8);

        // Yeux lumineux (dorés)
        sr.setColor(0.95f, 0.88f, 0.04f, 1f);
        sr.circle(cx - 1.8f, ly + h - 3.8f, 1.3f, 6);
        sr.circle(cx + 1.8f, ly + h - 3.8f, 1.3f, 6);

        // Reflet pupille (noir)
        sr.setColor(0.05f, 0.05f, 0.05f, 1f);
        sr.circle(cx - 1.8f, ly + h - 4f, 0.5f, 4);
        sr.circle(cx + 1.8f, ly + h - 4f, 0.5f, 4);

        // Griffes (petits rectangles au bas)
        sr.setColor(0.65f, 0.55f, 0.40f, 1f);
        sr.rect(lx + 2f, ly,       2f, 2f);
        sr.rect(lx + 5f, ly - 1f,  2f, 2.5f);
        sr.rect(lx + 8f, ly,       2f, 2f);
    }
}
