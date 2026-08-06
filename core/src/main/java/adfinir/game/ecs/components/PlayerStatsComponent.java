package adfinir.game.ecs.components;

import adfinir.game.player.StatSheet;
import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool;

/**
 * Composant Ashley qui porte l'état dynamique du joueur :
 *  - HP actuels (ne se régénèrent que par potions)
 *  - Stamina actuelle (régénération automatique avec le temps)
 *  - La StatSheet qui contient toutes les stats calculées
 */
public class PlayerStatsComponent implements Component, Pool.Poolable {

    public StatSheet stats = new StatSheet();

    // --- État dynamique ---
    public float currentHp;
    public float currentStamina;

    public boolean isDead = false;

    public PlayerStatsComponent() {
        reset();
    }

    @Override
    public void reset() {
        stats     = new StatSheet();
        currentHp      = stats.maxHp();
        currentStamina = stats.maxStamina();
        isDead         = false;
    }

    // ---------------------------------------------------------------
    // Délégations vers StatSheet (évite stats.stats.xxx() partout)
    // ---------------------------------------------------------------

    public float maxHp()        { return stats.maxHp(); }
    public float atk()          { return stats.atk(); }
    public float mag()          { return stats.mag(); }
    public float def()          { return stats.def(); }
    public float spd()          { return stats.spd(); }
    public float maxStamina()   { return stats.maxStamina(); }
    public float staminaRegen() { return stats.staminaRegen(); }

    // ---------------------------------------------------------------
    // HP — pas de régénération naturelle
    // ---------------------------------------------------------------

    /** Inflige des dégâts en tenant compte de la DEF. Retourne les dégâts réels subis. */
    public float takeDamage(float rawDamage) {
        float dmg = Math.max(1f, rawDamage - stats.def()); // DEF réduit, minimum 1
        currentHp = Math.max(0f, currentHp - dmg);
        if (currentHp <= 0f) isDead = true;
        return dmg;
    }

    /** Soigne le joueur (potion). Ne dépasse pas le max. */
    public void heal(float amount) {
        currentHp = Math.min(stats.maxHp(), currentHp + amount);
        isDead    = false;
    }

    /** Ratio HP pour la barre de vie (0.0 → 1.0). */
    public float hpRatio() {
        return (stats.maxHp() > 0f) ? currentHp / stats.maxHp() : 0f;
    }

    // ---------------------------------------------------------------
    // Stamina — régénération automatique
    // ---------------------------------------------------------------

    /**
     * Met à jour la stamina. Appelé à chaque frame par StatsSystem.
     * La stamina se régénère au rythme de stats.staminaRegen() par seconde.
     */
    public void updateStamina(float deltaTime) {
        if (currentStamina < stats.maxStamina()) {
            currentStamina = Math.min(
                stats.maxStamina(),
                currentStamina + stats.staminaRegen() * deltaTime
            );
        }
    }

    /**
     * Consomme de la stamina. Retourne true si la dépense a pu être faite.
     * Retourne false si la stamina est insuffisante (action impossible).
     */
    public boolean consumeStamina(float amount) {
        if (currentStamina < amount) return false;
        currentStamina = Math.max(0f, currentStamina - amount);
        return true;
    }

    /** Ratio Stamina pour la barre d'endurance (0.0 → 1.0). */
    public float staminaRatio() {
        return (stats.maxStamina() > 0f) ? currentStamina / stats.maxStamina() : 0f;
    }
}
