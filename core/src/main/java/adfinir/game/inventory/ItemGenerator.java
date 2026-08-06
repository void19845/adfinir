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

    public static Capacity generateCapacity() {
        Rarity rarity = getRandomRarity();
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

    private static Rarity getRandomRarity() {
        float r = rand.nextFloat();
        if (r < 0.05f) return Rarity.LEGENDARY;
        if (r < 0.15f) return Rarity.EPIC;
        if (r < 0.4f) return Rarity.RARE;
        return Rarity.COMMON;
    }
}
