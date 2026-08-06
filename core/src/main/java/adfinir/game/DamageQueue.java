package adfinir.game;

import java.util.ArrayDeque;

/**
 * File d'événements de dégâts partagée entre CombatSystem et GameScreen.
 * CombatSystem pousse des événements, GameScreen les consomme pour afficher
 * des nombres flottants au-dessus des entités touchées.
 */
public final class DamageQueue {

    /** [0]=worldX  [1]=worldY  [2]=dégâts  [3]=1 si crit sinon 0 */
    public static final ArrayDeque<float[]> events = new ArrayDeque<>();

    private DamageQueue() {}

    public static void push(float worldX, float worldY, float dmg, boolean crit) {
        events.offer(new float[]{ worldX, worldY, dmg, crit ? 1f : 0f });
    }
}
