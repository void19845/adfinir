package adfinir.game.enemy;

import com.badlogic.gdx.graphics.Color;

/**
 * Archétype d'ennemi : chaque type a un profil de stats (multiplicateurs
 * appliqués APRÈS le scaling par threatFactor), une taille/couleur propres
 * pour être reconnaissable au premier coup d'œil, et un comportement
 * (mêlée vs tireur à distance, voir EnemyStatsComponent.ranged / EnemyAIComponent.preferredRange).
 */
public enum EnemyType {
    /** Standard, aucun trait particulier — le point de référence des autres types. */
    GRUNT("Rôdeur", 1.0f, 1.0f, 1.0f, 1.0f, 12f, new Color(0.82f, 0.24f, 0.24f, 1f), false),
    /** Beaucoup de PV et de dégâts, lent. */
    BRUTE("Brute", 2.0f, 1.6f, 1.5f, 0.6f, 17f, new Color(0.55f, 0.16f, 0.6f, 1f), false),
    /** Fragile mais rapide, spawn en nombre à l'écran grâce à ses faibles PV. */
    SWARMER("Vermine", 0.5f, 0.5f, 0.65f, 1.8f, 8f, new Color(0.85f, 0.62f, 0.15f, 1f), false),
    /** Garde ses distances et tire des projectiles plutôt que d'attaquer au contact. */
    RANGED("Tireur", 0.75f, 0.8f, 0.85f, 0.85f, 11f, new Color(0.2f, 0.55f, 0.9f, 1f), true);

    public final String label;
    public final float hpMult;
    public final float defMult;
    public final float damageMult;
    public final float speedMult;
    public final float size;
    public final Color color;
    public final boolean ranged;

    EnemyType(String label, float hpMult, float defMult, float damageMult, float speedMult,
             float size, Color color, boolean ranged) {
        this.label = label;
        this.hpMult = hpMult;
        this.defMult = defMult;
        this.damageMult = damageMult;
        this.speedMult = speedMult;
        this.size = size;
        this.color = color;
        this.ranged = ranged;
    }
}
