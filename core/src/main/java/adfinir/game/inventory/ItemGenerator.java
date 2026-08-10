package adfinir.game.inventory;

import adfinir.game.player.AttackShape;
import adfinir.game.player.StatType;
import java.util.Random;

/**
 * Générateur procédural d'équipements.
 */
public class ItemGenerator {
    private static final Random rand = new Random();

    /** Chance qu'un socket vide (au-delà du socket garanti) soit pré-équipé d'un mod. */
    private static final float SOCKET_PREFILL_CHANCE = 0.2f;

    private static final String[] WEAPON_MOD_IDS = {
        "COMBO_BRUTAL", "COMBO_RAFALE", "COMBO_ESTOC", "COMBO_TOURBILLON"
    };
    private static final String[] CAPACITY_MOD_IDS = {
        "EFFECT_GLACE", "EFFECT_FOUDRE", "EFFECT_POISON", "EFFECT_VENT",
        "MOD_EXPLOSIF", "MOD_REBOND", "MOD_DUPLICATION", "MOD_CELERITE", "MOD_TRAJECTOIRE"
    };
    private static final String[] ARTIFACT_IDS = {
        "SYNERGIE_SANG", "COEUR_DE_FER", "BOTTES_AGILES", "FOCUS_ARCANIQUE"
    };

    /** name, base effect name, element, dégâts de base, vitesse, rayon, cooldown. */
    private static final Object[][] CAPACITY_BASE_PROFILES = {
        { "Sceptre de Flammes",  "Boule de Feu",    "FIRE",     15f, 300f, 12f, 0.8f },
        { "Bâton de Glace",      "Éclat Glacé",     "ICE",      12f, 260f, 11f, 0.75f },
        { "Fiole Toxique",       "Nuée Toxique",    "POISON",   9f,  220f, 14f, 0.9f },
        { "Plume du Vent",       "Lame de Vent",    "WIND",     8f,  380f, 8f,  0.55f },
    };

    public static Weapon generateWeapon() {
        return generateWeapon(1f);
    }

    public static Weapon generateWeapon(float threatFactor) {
        Rarity rarity = getRandomRarity(threatFactor);
        WeaponType type = WeaponType.values()[rand.nextInt(WeaponType.values().length)];
        return createWeaponOfType(type, rarity);
    }

    public static Weapon createWeaponOfType(WeaponType type, Rarity rarity) {
        String name = type.name() + " de " + rarity.name().toLowerCase();
        Weapon weapon = new Weapon(name, rarity, type);

        // Le combo entier vient des sockets (WeaponComboMod) — plus d'attaque
        // baked-in sur l'arme. Le socket 0 est garanti rempli pour qu'une
        // arme ait toujours au moins une attaque (utile en particulier pour
        // COMMON, qui n'a qu'un seul socket).
        for (int i = 0; i < weapon.getSocketCount(); i++) {
            boolean mustFill = (i == 0);
            if (mustFill || rand.nextFloat() < SOCKET_PREFILL_CHANCE) {
                String id = WEAPON_MOD_IDS[rand.nextInt(WEAPON_MOD_IDS.length)];
                weapon.setSocket(i, createModifierById(id, rarity));
            }
        }
        return weapon;
    }

    public static Weapon createWeaponOfType(WeaponType type) {
        return createWeaponOfType(type, Rarity.COMMON);
    }

    public static Capacity generateCapacity() {
        return generateCapacity(1f);
    }

    public static Capacity generateCapacity(float threatFactor) {
        Rarity rarity = getRandomRarity(threatFactor);
        Object[] profile = CAPACITY_BASE_PROFILES[rand.nextInt(CAPACITY_BASE_PROFILES.length)];
        CapacityEffect base = new CapacityEffect((String) profile[1],
            (Float) profile[3], (Float) profile[4], (Float) profile[5], (String) profile[2], (Float) profile[6]);
        Capacity cap = new Capacity((String) profile[0], rarity, base);

        // Ajouter des modificateurs selon la rareté
        int modCount = rarity.bonusPropertyCount;
        for (int i = 0; i < modCount; i++) {
            cap.addModifier(new CapacityModifier(
                CapacityModifier.ModType.values()[rand.nextInt(CapacityModifier.ModType.values().length)],
                1.0f
            ));
        }

        for (int i = 0; i < cap.getSocketCount(); i++) {
            if (rand.nextFloat() < SOCKET_PREFILL_CHANCE) {
                String id = CAPACITY_MOD_IDS[rand.nextInt(CAPACITY_MOD_IDS.length)];
                cap.setSocket(i, createModifierById(id, rarity));
            }
        }
        return cap;
    }

    public static Armor generateArmor() {
        return generateArmor(1f);
    }

    public static Armor generateArmor(float threatFactor) {
        Rarity rarity = getRandomRarity(threatFactor);
        ArmorType type = ArmorType.values()[rand.nextInt(ArmorType.values().length)];
        return new Armor("Armure de " + rarity.name(), rarity, type);
    }

    public static Artifact generateArtifact() {
        return generateArtifact(1f);
    }

    public static Artifact generateArtifact(float threatFactor) {
        Rarity rarity = getRandomRarity(threatFactor);
        String id = ARTIFACT_IDS[rand.nextInt(ARTIFACT_IDS.length)];
        return createArtifactById(id, artifactNameFor(id), rarity);
    }

    private static String artifactNameFor(String passiveEffectId) {
        switch (passiveEffectId) {
            case "COEUR_DE_FER":     return "Cœur de Fer";
            case "BOTTES_AGILES":    return "Bottes Agiles";
            case "FOCUS_ARCANIQUE":  return "Focus Arcanique";
            case "SYNERGIE_SANG":
            default:                 return "Relique Ancienne";
        }
    }

    /**
     * Reconstruit un Artifact à partir de son passiveEffectId : bonus de stats
     * (via Item.statBonuses, comme Armor) + une lambda d'effet passif — cette
     * dernière n'est qu'un point d'extension pour l'instant (pas encore
     * branchée sur un déclencheur de gameplay), donc pas sérialisée. Seule
     * source de vérité pour l'association id -> définition : utilisée à la
     * fois par la génération procédurale et par SaveManager lors du
     * chargement d'une sauvegarde.
     */
    public static Artifact createArtifactById(String passiveEffectId, String name, Rarity rarity) {
        Artifact artifact = new Artifact(name, rarity, passiveEffectId, (obj) -> {
            // Point d'extension pour un futur effet passif déclenché en jeu (ex: on-hit, on-kill).
        });
        float mult = rarity.statMultiplier;
        switch (passiveEffectId) {
            case "COEUR_DE_FER":
                artifact.getStatBonuses().put(StatType.MAX_HP, 30f * mult);
                artifact.getStatBonuses().put(StatType.DEF, 4f * mult);
                break;
            case "BOTTES_AGILES":
                artifact.getStatBonuses().put(StatType.SPD, 18f * mult);
                artifact.getStatBonuses().put(StatType.MAX_STAMINA, 15f * mult);
                break;
            case "FOCUS_ARCANIQUE":
                artifact.getStatBonuses().put(StatType.MAG, 6f * mult);
                artifact.getStatBonuses().put(StatType.STAMINA_REGEN, 1.5f * mult);
                break;
            case "SYNERGIE_SANG":
            default:
                artifact.getStatBonuses().put(StatType.ATK, 5f * mult);
                break;
        }
        return artifact;
    }

    /**
     * Génère un ItemModifier isolé (loot autonome, ex: apparaît sur une tile
     * TILE_LOOT). Choisit aléatoirement entre un mod d'arme et un mod de
     * capacité.
     */
    public static ItemModifier generateModifier(float threatFactor) {
        Rarity rarity = getRandomRarity(threatFactor);
        boolean weaponMod = rand.nextBoolean();
        String[] pool = weaponMod ? WEAPON_MOD_IDS : CAPACITY_MOD_IDS;
        String id = pool[rand.nextInt(pool.length)];
        return createModifierById(id, rarity);
    }

    /**
     * Reconstruit un ItemModifier à partir de son modifierId. Seule source de
     * vérité pour l'association id -> définition (WeaponAttack / CapacityEffect
     * / CapacityModifier concrets), utilisée à la fois par la génération
     * procédurale et par SaveManager lors du chargement d'une sauvegarde.
     */
    public static ItemModifier createModifierById(String modifierId, Rarity rarity) {
        switch (modifierId) {
            case "COMBO_BRUTAL":
                return new WeaponComboMod(modifierId, "Combo Brutal", rarity, java.util.Arrays.asList(
                    new WeaponAttack("Coup Brutal",
                        15f * rarity.statMultiplier, 25f * rarity.statMultiplier,
                        0.6f, 0.2f, 0.15f, null, 35f, AttackShape.ARC)
                ));
            case "COMBO_RAFALE":
                return new WeaponComboMod(modifierId, "Combo Rafale", rarity, java.util.Arrays.asList(
                    new WeaponAttack("Frappe Rapide",
                        5f * rarity.statMultiplier, 10f * rarity.statMultiplier,
                        0.25f, 0.1f, 0.05f, null, 20f, AttackShape.CONE)
                ));
            case "COMBO_ESTOC":
                return new WeaponComboMod(modifierId, "Combo Estoc", rarity, java.util.Arrays.asList(
                    new WeaponAttack("Estoc",
                        20f * rarity.statMultiplier, 32f * rarity.statMultiplier,
                        0.9f, 0.3f, 0.1f, null, 45f, AttackShape.RECTANGLE)
                ));
            case "COMBO_TOURBILLON":
                return new WeaponComboMod(modifierId, "Combo Tourbillon", rarity, java.util.Arrays.asList(
                    new WeaponAttack("Tourbillon",
                        10f * rarity.statMultiplier, 16f * rarity.statMultiplier,
                        0.45f, 0.25f, 0.3f, null, 40f, AttackShape.ARC)
                ));
            case "EFFECT_GLACE":
                return new CapacityEffectMod(modifierId, "Éclat de Glace", rarity,
                    new CapacityEffect("Éclat de Glace", 12f * rarity.statMultiplier, 250f, 10f, "ICE", 0.6f),
                    null);
            case "EFFECT_FOUDRE":
                return new CapacityEffectMod(modifierId, "Décharge", rarity,
                    null,
                    new CapacityModifier(CapacityModifier.ModType.RICOCHET, 1.0f));
            case "EFFECT_POISON":
                return new CapacityEffectMod(modifierId, "Nuée Toxique", rarity,
                    new CapacityEffect("Nuée Toxique", 9f * rarity.statMultiplier, 220f, 14f, "POISON", 0.9f),
                    null);
            case "EFFECT_VENT":
                return new CapacityEffectMod(modifierId, "Lame de Vent", rarity,
                    new CapacityEffect("Lame de Vent", 8f * rarity.statMultiplier, 380f, 8f, "WIND", 0.55f),
                    null);
            case "MOD_EXPLOSIF":
                return new CapacityEffectMod(modifierId, "Charge Explosive", rarity,
                    null,
                    new CapacityModifier(CapacityModifier.ModType.EXPLOSION, 1.0f));
            case "MOD_REBOND":
                return new CapacityEffectMod(modifierId, "Rebond", rarity,
                    null,
                    new CapacityModifier(CapacityModifier.ModType.BOUNCE, 1.0f));
            case "MOD_DUPLICATION":
                return new CapacityEffectMod(modifierId, "Duplication", rarity,
                    null,
                    new CapacityModifier(CapacityModifier.ModType.DUPLICATE, 1.0f));
            case "MOD_CELERITE":
                return new CapacityEffectMod(modifierId, "Célérité", rarity,
                    null,
                    new CapacityModifier(CapacityModifier.ModType.SPEED_UP, 1.0f));
            case "MOD_TRAJECTOIRE":
                return new CapacityEffectMod(modifierId, "Trajectoire Courbe", rarity,
                    null,
                    new CapacityModifier(CapacityModifier.ModType.ARC, 1.0f));
            default:
                return null;
        }
    }

    /** Prix boutique d'un objet selon sa rareté. */
    public static int priceFor(Rarity rarity) {
        return Math.round(30 * rarity.statMultiplier);
    }

    private static Rarity getRandomRarity() {
        return getRandomRarity(1f);
    }

    /**
     * Tirage de rareté pondéré. threatFactor == 1 correspond à la distribution
     * d'origine ; au-dessus de 1, le poids se déplace vers les raretés hautes
     * (loot de meilleure qualité sur les étages profonds / plus menaçants).
     */
    public static Rarity getRandomRarity(float threatFactor) {
        float shift = Math.max(0f, threatFactor - 1f);

        float wCommon    = Math.max(5f, 60f - shift * 30f);
        float wRare      = 25f + shift * 10f;
        float wEpic      = 10f + shift * 12f;
        float wLegendary = 5f  + shift * 8f;
        float wMythical  = shift * 4f;

        float total = wCommon + wRare + wEpic + wLegendary + wMythical;
        float r = rand.nextFloat() * total;

        if ((r -= wCommon) < 0)    return Rarity.COMMON;
        if ((r -= wRare) < 0)      return Rarity.RARE;
        if ((r -= wEpic) < 0)      return Rarity.EPIC;
        if ((r -= wLegendary) < 0) return Rarity.LEGENDARY;
        return Rarity.MYTHICAL;
    }
}
