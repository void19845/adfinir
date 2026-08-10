package adfinir.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

import java.util.HashSet;
import java.util.Set;

/**
 * État de la compétence Dash (touche Tab) : ruée courte à vitesse élevée
 * dans la direction visée, qui inflige des dégâts aux ennemis traversés.
 * Séparé de CombatComponent (comme CapacityComponent) : une compétence à
 * touche dédiée, avec son propre cooldown, indépendante de l'arme.
 */
public class DashComponent implements Component {
    public float timer = 0f;          // cooldown restant avant le prochain dash possible
    public boolean dashing = false;   // true pendant la ruée active
    public float dashTimeLeft = 0f;   // temps restant de la ruée active
    public float dirX = 1f;
    public float dirY = 0f;

    // Entités déjà touchées pendant la ruée en cours (évite les hits multiples)
    public final Set<Entity> hitEntities = new HashSet<>();

    public boolean canDash() {
        return timer <= 0f && !dashing;
    }

    public void trigger(float dirX, float dirY, float cooldown, float duration) {
        this.dirX = dirX;
        this.dirY = dirY;
        this.dashing = true;
        this.dashTimeLeft = duration;
        this.timer = cooldown;
        hitEntities.clear();
    }
}
