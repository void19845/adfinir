package adfinir.game.ecs.systems;

import adfinir.game.DamageQueue;
import adfinir.game.ecs.components.*;
import adfinir.game.items.Artifact;
import adfinir.game.items.Item;
import adfinir.game.items.ItemDatabase;
import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Gère les attaques du joueur sur les mobs.
 *
 * Deux modes :
 *  - Attaque rapide  (Space/F) : dégâts normaux, courte portée
 *  - Attaque puissante (R)     : 2× dégâts, portée large, frappe plusieurs mobs
 */
public class CombatSystem extends IteratingSystem {

    private static final Random RNG = new Random();

    private final ComponentMapper<TransformComponent> tm  = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<CombatComponent>    cm  = ComponentMapper.getFor(CombatComponent.class);
    private final ComponentMapper<StatsComponent>     sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<InventoryComponent> im  = ComponentMapper.getFor(InventoryComponent.class);
    private final ComponentMapper<HealthComponent>    hm  = ComponentMapper.getFor(HealthComponent.class);
    private final ComponentMapper<MobAIComponent>     aim = ComponentMapper.getFor(MobAIComponent.class);
    private final ComponentMapper<LootDropComponent>  lm  = ComponentMapper.getFor(LootDropComponent.class);

    private ImmutableArray<Entity> mobs;
    private final List<Entity> toRemove = new ArrayList<>();

    public CombatSystem() {
        super(Family.all(TransformComponent.class, CombatComponent.class, StatsComponent.class).get(), 5);
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        mobs = engine.getEntitiesFor(
            Family.all(TransformComponent.class, MobAIComponent.class, HealthComponent.class).get()
        );
    }

    @Override
    public void update(float deltaTime) {
        toRemove.clear();
        super.update(deltaTime);
        for (Entity e : toRemove) getEngine().removeEntity(e);
    }

    @Override
    protected void processEntity(Entity attacker, float dt) {
        CombatComponent    combat = cm.get(attacker);
        TransformComponent pos    = tm.get(attacker);
        StatsComponent     stats  = sm.get(attacker);
        InventoryComponent inv    = im.get(attacker);

        // Mise à jour des cooldowns
        if (combat.currentCooldown  > 0) { combat.currentCooldown  -= dt; combat.wantsToAttack    = false; }
        if (combat.powerCurrentCD   > 0) { combat.powerCurrentCD   -= dt; combat.wantsPowerAttack  = false; }

        float fx = (pos.facingX == 0 && pos.facingY == 0) ? 0f  : pos.facingX;
        float fy = (pos.facingX == 0 && pos.facingY == 0) ? -1f : pos.facingY;

        // ── Attaque rapide ─────────────────────────────────────────────
        if (combat.wantsToAttack && combat.currentCooldown <= 0) {
            combat.wantsToAttack   = false;
            combat.currentCooldown = combat.attackCooldown;
            float atk = effectiveAtk(stats, inv);
            float cx  = pos.x + fx * (combat.attackRange * 0.5f);
            float cy  = pos.y + fy * (combat.attackRange * 0.5f);
            strikeInZone(cx, cy, combat.attackRange, atk, combat.knockbackForce,
                         false, stats, inv, attacker);
        }

        // ── Attaque puissante ──────────────────────────────────────────
        if (combat.wantsPowerAttack && combat.powerCurrentCD <= 0) {
            combat.wantsPowerAttack = false;
            combat.powerCurrentCD   = combat.powerCooldown;
            float atk = effectiveAtk(stats, inv) * 2.2f;
            float cx  = pos.x + fx * (combat.powerRange * 0.4f);
            float cy  = pos.y + fy * (combat.powerRange * 0.4f);
            strikeInZone(cx, cy, combat.powerRange, atk, combat.knockbackForce * 1.6f,
                         true, stats, inv, attacker);
        }
    }

    // ── Frappe dans une zone ───────────────────────────────────────────────

    private void strikeInZone(float cx, float cy, float range, float atk, float knockback,
                               boolean isPower, StatsComponent stats, InventoryComponent inv,
                               Entity attacker) {
        for (Entity mob : mobs) {
            MobAIComponent ai = aim.get(mob);
            if (ai == null || ai.state == MobAIComponent.State.DEAD) continue;

            TransformComponent mpos = tm.get(mob);
            float dx = mpos.x - cx, dy = mpos.y - cy;
            float d  = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > range) continue;

            HealthComponent mobHp = hm.get(mob);
            if (mobHp == null) continue;

            // Dégâts (critique inclus)
            float critRate = stats.critRate + (inv != null ? inv.getArtifactCritRate() : 0f);
            float critDmg  = stats.critDmg  + (inv != null ? inv.getArtifactCritDmg()  : 0f);
            boolean crit   = RNG.nextFloat() < critRate;
            float dmg      = Math.max(1f, atk);
            if (crit) dmg *= critDmg;

            mobHp.currentHealth = Math.max(0f, mobHp.currentHealth - dmg);

            // Damage number flottant
            DamageQueue.push(mpos.x, mpos.y + 10f, dmg, crit);

            // Knockback
            float klen = d > 0.01f ? d : 1f;
            ai.knockVx    = (dx / klen) * knockback;
            ai.knockVy    = (dy / klen) * knockback;
            ai.knockTimer = isPower ? 0.28f : 0.18f;

            // Mort
            if (mobHp.currentHealth <= 0) {
                grantLootAndXp(attacker, mob, stats, inv);
                toRemove.add(mob);
            }
        }
    }

    // ── Loot & XP ─────────────────────────────────────────────────────────

    private void grantLootAndXp(Entity attacker, Entity mob,
                                  StatsComponent stats, InventoryComponent inv) {
        LootDropComponent loot = lm.get(mob);
        if (loot == null) return;

        stats.xp += loot.xpReward;
        while (stats.tryLevelUp()) { /* chain */ }

        if (inv == null) return;
        Object drop = ItemDatabase.randomDrop(RNG, loot.dropChance);
        if (drop instanceof Item)     inv.autoEquip((Item) drop);
        else if (drop instanceof Artifact) {
            inv.addArtifact((Artifact) drop);
            // Auto-équiper si le slot est libre
            Artifact art = (Artifact) drop;
            if (inv.getEquippedArtifact(art.slot) == null) inv.equipArtifact(art);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private float effectiveAtk(StatsComponent stats, InventoryComponent inv) {
        float atk = stats.atk;
        if (inv != null) atk += inv.getWeaponAtkBonus() + inv.getArtifactAtk();
        return atk;
    }
}
