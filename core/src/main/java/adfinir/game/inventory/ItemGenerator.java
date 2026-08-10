package adfinir.game.inventory;

import adfinir.game.player.AttackShape;
import java.util.Random;

/**
 * Générateur procédural d'équipements.
 */
public class ItemGenerator {
    private static final Random rand = new Random();

    /** Chance qu'un socket vide (au-delà du socket garanti) soit pré-équipé d'un mod. */
    private static final float SOCKET_PREFILL_CHANCE = 0.2f;

    private static final String[] WEAPON_MOD_IDS = { "COMBO_BRUTAL", "COMBO_RAFALE" };
    private static final String[] CAPACITY_MOD_IDS = { "EFFECT_GLACE", "EFFECT_FOUDRE" };

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
        CapacityEffect base = new CapacityEffect("Boule de Feu", 15f, 300f, 12f, "FIRE", 0.8f);
        Capacity cap = new Capacity("Sceptre Arcanique", rarity, base);

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
        return createArtifactById("SYNERGIE_SANG", "Relique Ancienne", rarity);
    }

    /**
     * Reconstruit un Artifact à partir de son passiveEffectId.
     * Seule source de vérité pour l'association id -> logique d'effet (lambda) :
     * utilisée à la fois par la génération procédurale et par SaveManager lors
     * du chargement d'une sauvegarde (la lambda elle-même n'est pas sérialisable).
     */
    public static Artifact createArtifactById(String passiveEffectId, String name, Rarity rarity) {
        switch (passiveEffectId) {
            case "SYNERGIE_SANG":
            default:
                return new Artifact(name, rarity, passiveEffectId, (obj) -> {
                    // Logique de l'effet passif
                });
        }
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
            case "EFFECT_GLACE":
                return new CapacityEffectMod(modifierId, "Éclat de Glace", rarity,
                    new CapacityEffect("Éclat de Glace", 12f * rarity.statMultiplier, 250f, 10f, "ICE", 0.6f),
                    null);
            case "EFFECT_FOUDRE":
                return new CapacityEffectMod(modifierId, "Décharge", rarity,
                    null,
                    new CapacityModifier(CapacityModifier.ModType.RICOCHET, 1.0f));
            default:
                return null;
        }
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
