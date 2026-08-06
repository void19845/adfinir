package adfinir.game.ecs.components;

import com.badlogic.ashley.core.Component;

public class MobAIComponent implements Component {

    public enum State { PATROL, AGGRO, ATTACK, DEAD }

    public State state        = State.PATROL;
    public float aggroRange   = 80f;
    public float attackRange  = 14f;
    public float speed        = 35f;
    public float attackDamage = 8f;

    public float attackCooldown  = 1.5f;
    public float currentCooldown = 0f;

    // Patrol
    public float patrolTargetX = 0f;
    public float patrolTargetY = 0f;
    public float patrolTimer   = 0f;

    // Knockback reçu
    public float knockVx    = 0f;
    public float knockVy    = 0f;
    public float knockTimer = 0f;
}
