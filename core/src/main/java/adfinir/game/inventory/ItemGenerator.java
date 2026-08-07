package adfinir.game.inventory;

import java.util.Random;

/**
 * Générateur procédural d'équipements.
 */
public class ItemGenerator {
    private static final Random rand = new Random();

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

        // Générer un nombre de combos basé sur la rareté
        int combos = 1 + rand.nextInt(3);
        for (int i = 0; i < combos; i++) {
            weapon.addAttack(new WeaponAttack(
                "Attaque " + (i + 1),
                10f * rarity.statMultiplier,
                20f * rarity.statMultiplier,
                0.5f + (rand.nextFloat() * 0.5f),
                0.15f,
                0.1f,
                null,
                30f
            ));
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
        CapacityEffect base = new CapacityEffect("Boule de Feu", 15f, 300f, 50f, "FIRE");
        Capacity cap = new Capacity("Sceptre Arcanique", rarity, base);

        // Ajouter des modificateurs selon la rareté
        int modCount = rarity.bonusPropertyCount;
        for (int i = 0; i < modCount; i++) {
            cap.addModifier(new CapacityModifier(
                CapacityModifier.ModType.values()[rand.nextInt(CapacityModifier.ModType.values().length)],
                1.0f
            ));
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
