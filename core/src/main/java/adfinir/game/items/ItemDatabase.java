package adfinir.game.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

public final class ItemDatabase {

    private static final List<Item>     weapons   = new ArrayList<>();
    private static final List<Item>     armors    = new ArrayList<>();
    private static final List<Artifact> artifacts = new ArrayList<>();
    private static boolean loaded = false;

    private ItemDatabase() {}

    // ── Chargement ─────────────────────────────────────────────────────

    public static void load() {
        if (loaded) return;
        loaded = true;
        loadDefaults();
        try {
            String json = Gdx.files.internal("items.json").readString("UTF-8");
            parseJson(json);
            Gdx.app.log("ItemDatabase", "Chargé : " + weapons.size()
                + " armes, " + armors.size() + " armures, " + artifacts.size() + " artéfacts");
        } catch (Exception e) {
            Gdx.app.log("ItemDatabase", "items.json introuvable, defaults utilisés : " + e.getMessage());
        }
    }

    private static void loadDefaults() {
        weapons.add(Item.RUSTY_SWORD);
        weapons.add(Item.IRON_SWORD);
        armors.add(Item.LEATHER_ARMOR);
        armors.add(Item.IRON_SHIELD);
        artifacts.add(Artifact.ADVENTURER_FLOWER);
        artifacts.add(Artifact.HERO_FEATHER);
        artifacts.add(Artifact.SWIFT_CLOCK);
    }

    private static void parseJson(String json) {
        JsonValue root = new JsonReader().parse(json);
        parseWeapons(root.get("weapons"));
        parseArmors(root.get("armors"));
        parseArtifacts(root.get("artifacts"));
    }

    private static void parseWeapons(JsonValue arr) {
        if (arr == null) return;
        for (JsonValue w = arr.child; w != null; w = w.next) {
            try {
                Item item = new Item(
                    w.getString("name", "Arme"),
                    rarity(w.getString("rarity", "COMMON")),
                    Item.Type.WEAPON,
                    w.getFloat("atkBonus", 0f),
                    w.getFloat("defBonus", 0f),
                    w.getInt("xpValue", 0)
                );
                weapons.add(item);
            } catch (Exception e) {
                Gdx.app.log("ItemDB", "Arme ignorée : " + e.getMessage());
            }
        }
    }

    private static void parseArmors(JsonValue arr) {
        if (arr == null) return;
        for (JsonValue a = arr.child; a != null; a = a.next) {
            try {
                Item item = new Item(
                    a.getString("name", "Armure"),
                    rarity(a.getString("rarity", "COMMON")),
                    Item.Type.ARMOR,
                    a.getFloat("atkBonus", 0f),
                    a.getFloat("defBonus", 0f),
                    a.getInt("xpValue", 0)
                );
                armors.add(item);
            } catch (Exception e) {
                Gdx.app.log("ItemDB", "Armure ignorée : " + e.getMessage());
            }
        }
    }

    private static void parseArtifacts(JsonValue arr) {
        if (arr == null) return;
        for (JsonValue a = arr.child; a != null; a = a.next) {
            try {
                Artifact art = new Artifact(
                    a.getString("name", "Artéfact"),
                    rarity(a.getString("rarity", "COMMON")),
                    Artifact.Slot.valueOf(a.getString("slot", "FLOWER")),
                    Artifact.MainStat.valueOf(a.getString("mainStat", "HP")),
                    a.getFloat("mainValue", 0f),
                    a.getFloat("atkBonus", 0f),
                    a.getFloat("defBonus", 0f),
                    a.getFloat("critRateBonus", 0f),
                    a.getFloat("critDmgBonus", 0f)
                );
                artifacts.add(art);
            } catch (Exception e) {
                Gdx.app.log("ItemDB", "Artéfact ignoré : " + e.getMessage());
            }
        }
    }

    // ── Drops aléatoires ──────────────────────────────────────────────

    /**
     * Renvoie un Item ou un Artifact aléatoire (ou null si pas de drop).
     * @param rng        générateur aléatoire
     * @param dropChance probabilité de drop [0-1]
     */
    public static Object randomDrop(Random rng, float dropChance) {
        if (rng.nextFloat() > dropChance) return null;

        // Répartition : 40% arme, 35% armure, 25% artéfact
        float r = rng.nextFloat();
        if (r < 0.40f && !weapons.isEmpty())
            return weapons.get(rng.nextInt(weapons.size()));
        if (r < 0.75f && !armors.isEmpty())
            return armors.get(rng.nextInt(armors.size()));
        if (!artifacts.isEmpty())
            return artifacts.get(rng.nextInt(artifacts.size()));
        // Fallback
        if (!weapons.isEmpty()) return weapons.get(rng.nextInt(weapons.size()));
        return null;
    }

    // ── Accesseurs (lecture seule) ────────────────────────────────────

    public static List<Item>     getWeapons()   { return weapons;   }
    public static List<Item>     getArmors()    { return armors;    }
    public static List<Artifact> getArtifacts() { return artifacts; }

    // ── Helper ────────────────────────────────────────────────────────

    private static Item.Rarity rarity(String s) {
        try { return Item.Rarity.valueOf(s.toUpperCase()); }
        catch (Exception e) { return Item.Rarity.COMMON; }
    }
}
