package adfinir.game.combat;

import adfinir.game.inventory.Weapon;
import adfinir.game.player.AttackShape;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;

/**
 * Géométrie partagée des hitbox d'attaque (CONE/RECTANGLE/ARC).
 * Utilisée à la fois par RenderSystem (dessin) et CombatSystem (détection de coups)
 * pour que la hitbox visible corresponde toujours exactement à la hitbox réelle.
 */
public class AttackGeometry {

    public static final float CONE_ANGLE_DEG = 60f;
    public static final float RECTANGLE_HALF_WIDTH = 15f;
    public static final float ARC_WIDTH_MULT = 1.5f;
    public static final float HIT_RADIUS = 6f;

    public static float computeRange(Weapon weapon, int comboIndex) {
        java.util.List<adfinir.game.inventory.WeaponAttack> combo = weapon.getActiveCombo();
        if (comboIndex < 0 || comboIndex >= combo.size()) return 30f;
        return combo.get(comboIndex).areaOfEffect * weapon.type.rangeMod;
    }

    public static float coneCentralAngleDeg(float dirX, float dirY) {
        return (float) Math.toDegrees(Math.atan2(dirY, dirX));
    }

    /** Rectangle axis-aligned pour RECTANGLE (lance) et ARC (hache) — ne gère pas CONE. */
    public static Rectangle computeAxisAlignedRect(AttackShape shape, float originX, float originY,
                                                     float dirX, float dirY, float range) {
        switch (shape) {
            case RECTANGLE: {
                float length = range;
                if (Math.abs(dirX) > Math.abs(dirY)) {
                    return new Rectangle(
                        originX + (dirX > 0 ? 0 : -length),
                        originY - RECTANGLE_HALF_WIDTH,
                        length,
                        RECTANGLE_HALF_WIDTH * 2f
                    );
                } else {
                    return new Rectangle(
                        originX - RECTANGLE_HALF_WIDTH,
                        originY + (dirY > 0 ? 0 : -length),
                        RECTANGLE_HALF_WIDTH * 2f,
                        length
                    );
                }
            }
            case ARC: {
                float arcWidth = range * ARC_WIDTH_MULT;
                float arcDepth = range;
                if (Math.abs(dirX) > Math.abs(dirY)) {
                    return new Rectangle(
                        originX + (dirX > 0 ? 0 : -arcWidth),
                        originY - arcDepth / 2f,
                        arcWidth,
                        arcDepth
                    );
                } else {
                    return new Rectangle(
                        originX - arcWidth / 2f,
                        originY + (dirY > 0 ? 0 : -arcDepth),
                        arcWidth,
                        arcDepth
                    );
                }
            }
            default:
                throw new IllegalArgumentException("computeAxisAlignedRect ne gère pas " + shape);
        }
    }

    /** Teste si une cible (position + rayon) est touchée par la hitbox d'attaque courante. */
    public static boolean overlaps(AttackShape shape, float originX, float originY, float dirX, float dirY,
                                    float range, float targetX, float targetY, float targetRadius) {
        if (shape == AttackShape.CONE) {
            float dx = targetX - originX;
            float dy = targetY - originY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > range / 2f + targetRadius) return false;
            if (dist <= 0f) return true;
            float targetAngle = (float) Math.toDegrees(Math.atan2(dy, dx));
            float centralAngle = coneCentralAngleDeg(dirX, dirY);
            return Math.abs(angleDiffDeg(centralAngle, targetAngle)) <= CONE_ANGLE_DEG / 2f;
        }

        Rectangle rect = computeAxisAlignedRect(shape, originX, originY, dirX, dirY, range);
        return Intersector.overlaps(new Circle(targetX, targetY, targetRadius), rect);
    }

    /** Différence angulaire signée entre deux angles en degrés, ramenée à [-180, 180]. */
    private static float angleDiffDeg(float a, float b) {
        float diff = (b - a) % 360f;
        if (diff > 180f) diff -= 360f;
        if (diff < -180f) diff += 360f;
        return diff;
    }
}
