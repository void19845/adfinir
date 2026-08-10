package adfinir.game.inventory;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * Générateur procédural d'équipements.
 */
public class ItemGenerator {
    private static final Random rand = new Random();

    public static Weapon generateWeapon() {
        Rarity rarity = getRandomRarity();
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

    /** Sorts disponibles : nom, dégâts de base, vitesse, rayon, élément. */
    private static final Object[][] SPELL_PRESETS = {
        {"Boule de Feu",    15f, 300f, 50f, "FIRE"},
        {"Éclat de Glace",  12f, 260f, 40f, "ICE"},
        {"Foudre",          20f, 420f, 35f, "ELECTRIC"},
        {"Nuage Toxique",    8f, 150f, 70f, "POISON"},
        {"Onde de Choc",    18f, 200f, 60f, "PHYSICAL"},
        {"Lame de Vent",    14f, 500f, 30f, "WIND"},
    };

    public static Capacity generateCapacity() {
        Rarity rarity = getRandomRarity();
        Object[] preset = SPELL_PRESETS[rand.nextInt(SPELL_PRESETS.length)];
        String name = (String) preset[0];
        CapacityEffect base = new CapacityEffect(name, (float) preset[1], (float) preset[2], (float) preset[3], (String) preset[4]);
        Capacity cap = new Capacity(name, rarity, base);

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
        Rarity rarity = getRandomRarity();
        ArmorType type = ArmorType.values()[rand.nextInt(ArmorType.values().length)];
        return new Armor("Armure de " + rarity.name(), rarity, type);
    }

    public static Artifact generateArtifact() {
        Rarity rarity = getRandomRarity();
        return new Artifact("Relique Ancienne", rarity, "SYNERGIE_SANG", (obj) -> {
            // Logique de l'effet passif
        });
    }

    /** Prix boutique d'un objet selon sa rareté. */
    public static int priceFor(Rarity rarity) {
        return Math.round(30 * rarity.statMultiplier);
    }

    private static Rarity getRandomRarity() {
        float r = rand.nextFloat();
        if (r < 0.05f) return Rarity.LEGENDARY;
        if (r < 0.15f) return Rarity.EPIC;
        if (r < 0.4f) return Rarity.RARE;
        return Rarity.COMMON;
    }
}
