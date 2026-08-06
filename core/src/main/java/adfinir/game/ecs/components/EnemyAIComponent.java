package adfinir.game.ecs.components;

import com.badlogic.ashley.core.Component;

/**
 * Composant gérant l'IA simple d'un ennemi.
 */
public class EnemyAIComponent implements Component {
    public float changeDirectionTimer = 0f;
    public float moveDuration = 2f; // Change de direction toutes les 2 secondes
    public float speed = 40f;
}
