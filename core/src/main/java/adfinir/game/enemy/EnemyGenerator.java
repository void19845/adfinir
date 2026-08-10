package adfinir.game.enemy;

import adfinir.game.combat.AttackGeometry;
import adfinir.game.ecs.components.CombatComponent;
import adfinir.game.ecs.components.EnemyAIComponent;
import adfinir.game.ecs.components.EnemyStatsComponent;
import adfinir.game.inventory.ItemGenerator;
import adfinir.game.inventory.Rarity;
import adfinir.game.inventory.Weapon;
import adfinir.game.inventory.WeaponType;
import com.badlogic.gdx.math.MathUtils;

/**
 * Configure un ennemi fraîchement spawné : type (taille), stats et arme,
 * mis à l'échelle selon le niveau du donjon courant.
 */
public class EnemyGenerator {

    private static final float LEVEL_GROWTH = 0.15f; // +15% par niveau au-delà du niveau 1

    /** Attribue un type, des stats et une arme à l'ennemi, mis à l'échelle par le niveau. */
    public static void configureEnemy(EnemyStatsComponent stats, CombatComponent combat, EnemyAIComponent ai, int level) {
        int rarityRoll = MathUtils.random(100);
        EnemyStatsComponent.EnemyType type;
        float sizeMult;
        float defMult;

        if (rarityRoll < 5) {
            type = EnemyStatsComponent.EnemyType.LARGE;
            sizeMult = 2.0f;
            defMult  = 1.5f;
        } else if (rarityRoll < 30) {
            type = EnemyStatsComponent.EnemyType.MEDIUM;
            sizeMult = 1.0f;
            defMult  = 1.0f;
        } else {
            type = EnemyStatsComponent.EnemyType.SMALL;
            sizeMult = 0.5f;
            defMult  = 0.75f;
        }

        float levelMult = 1f + LEVEL_GROWTH * (level - 1);

        stats.type = type;
        stats.maxHp = 50f * sizeMult * levelMult;
        stats.currentHp = stats.maxHp;
        stats.def = 2f * defMult * levelMult;
        stats.atk = 8f * sizeMult * levelMult;
        stats.isDead = false;

        WeaponType weaponType = WeaponType.values()[MathUtils.random(WeaponType.values().length - 1)];
        Weapon weapon = ItemGenerator.createWeaponOfType(weaponType, pickWeaponRarity(level));
        combat.weapon = weapon;

        ai.attackRange = AttackGeometry.computeRange(weapon, 0);
    }

    /** Rareté de l'arme ennemie : de meilleures chances aux niveaux élevés, plafonnées, jamais MYTHICAL. */
    private static Rarity pickWeaponRarity(int level) {
        float legendaryChance = Math.min(0.05f + 0.01f * level, 0.20f);
        float epicChance      = Math.min(0.10f + 0.02f * level, 0.35f);
        float r = MathUtils.random();
        if (r < legendaryChance) return Rarity.LEGENDARY;
        if (r < legendaryChance + epicChance) return Rarity.EPIC;
        if (r < legendaryChance + epicChance + 0.35f) return Rarity.RARE;
        return Rarity.COMMON;
    }
}
