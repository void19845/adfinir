package adfinir.game.ecs.components;

import com.badlogic.ashley.core.Component;

/**
 * Composant gérant l'IA d'un ennemi.
 */
public class EnemyAIComponent implements Component {
    public enum State { IDLE, PURSUING, ATTACKING }
    public State state = State.IDLE;

    public float changeDirectionTimer = 0f;
    public float moveDuration = 2f;
    public float speed = 40f;

    public float detectionRange = 100f;
    public float pursuitSpeed = 40f;

    // Portée à laquelle l'ennemi s'arrête de poursuivre pour attaquer (calculée depuis son arme)
    public float attackRange = 20f;
}
