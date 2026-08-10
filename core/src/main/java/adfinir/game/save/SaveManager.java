package adfinir.game.save;

import adfinir.game.dungeon.DungeonMap;
import adfinir.game.ecs.components.InventoryComponent;
import adfinir.game.ecs.components.LootBarComponent;
import adfinir.game.ecs.components.PlayerStatsComponent;
import adfinir.game.ecs.components.TransformComponent;
import adfinir.game.inventory.Armor;
import adfinir.game.inventory.ArmorType;
import adfinir.game.inventory.Artifact;
import adfinir.game.inventory.Capacity;
import adfinir.game.inventory.CapacityEffect;
import adfinir.game.inventory.CapacityModifier;
import adfinir.game.inventory.Item;
import adfinir.game.inventory.ItemGenerator;
import adfinir.game.inventory.ItemModifier;
import adfinir.game.inventory.Rarity;
import adfinir.game.inventory.Weapon;
import adfinir.game.inventory.WeaponType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

/**
 * Sauvegarde / chargement de la progression du joueur.
 * Un seul emplacement de sauvegarde (fichier local unique, pas de multi-slots).
 */
public class SaveManager {

    private static final String SAVE_PATH = "save.json";

    public static boolean saveExists() {
        return Gdx.files.local(SAVE_PATH).exists();
    }

    public static void deleteSave() {
        FileHandle f = Gdx.files.local(SAVE_PATH);
        if (f.exists()) f.delete();
    }

    /** Sauvegarde l'étage courant (layout inclus), la position, les PV/Stamina, l'équipement et la loot bar. */
    public static void save(int currentLevel, DungeonMap dungeonMap, TransformComponent transform,
                            PlayerStatsComponent playerStats, InventoryComponent inventory,
                            LootBarComponent lootBar) {
        SaveData data = new SaveData();
        data.currentLevel   = currentLevel;
        data.currentHp      = playerStats.currentHp;
        data.currentStamina = playerStats.currentStamina;
        data.gold           = playerStats.gold;
        data.playerX = transform.x;
        data.playerY = transform.y;

        data.dungeonTiles = extractGrid(dungeonMap);
        data.spawnCol = dungeonMap.spawnCol;
        data.spawnRow = dungeonMap.spawnRow;
        data.exitCol  = dungeonMap.exitCol;
        data.exitRow  = dungeonMap.exitRow;

        data.weapon   = toSave(inventory.weapon);
        data.armor    = toSave(inventory.armor);
        data.capacity = toSave(inventory.capacity);
        data.artifact = toSave(inventory.artifact);

        for (Item item : lootBar.slots) {
            data.lootBar.add(toItemSlotSave(item));
        }

        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        Gdx.files.local(SAVE_PATH).writeString(json.prettyPrint(data), false);
    }

    /** Copie la grille de tiles de la carte (DungeonMap n'expose pas son tableau interne). */
    private static int[][] extractGrid(DungeonMap map) {
        int[][] grid = new int[map.rows][map.cols];
        for (int r = 0; r < map.rows; r++) {
            for (int c = 0; c < map.cols; c++) {
                grid[r][c] = map.getTile(c, r);
            }
        }
        return grid;
    }

    /** Reconstruit le DungeonMap exact à partir de la grille sauvegardée. */
    public static DungeonMap toDungeonMap(SaveData data) {
        return new DungeonMap(data.dungeonTiles, data.spawnCol, data.spawnRow, data.exitCol, data.exitRow);
    }

    /** Retourne les données sauvegardées, ou null si aucune sauvegarde n'existe. */
    public static SaveData load() {
        FileHandle f = Gdx.files.local(SAVE_PATH);
        if (!f.exists()) return null;
        Json json = new Json();
        return json.fromJson(SaveData.class, f);
    }

    /** Reconstruit un InventoryComponent complet à partir des données sauvegardées. */
    public static InventoryComponent toInventory(SaveData data) {
        InventoryComponent inv = new InventoryComponent();
        inv.equipWeapon(fromSave(data.weapon));
        inv.equipArmor(fromSave(data.armor));
        inv.equipCapacity(fromSave(data.capacity));
        inv.equipArtifact(fromSave(data.artifact));
        return inv;
    }

    /** Reconstruit la loot bar (8 slots) à partir des données sauvegardées. */
    public static LootBarComponent toLootBar(SaveData data) {
        LootBarComponent bar = new LootBarComponent();
        if (data.lootBar != null) {
            for (int i = 0; i < data.lootBar.size() && i < LootBarComponent.CAPACITY; i++) {
                bar.slots[i] = fromItemSlotSave(data.lootBar.get(i));
            }
        }
        return bar;
    }

    // ------------------------------------------------------------------
    // Weapon
    // ------------------------------------------------------------------

    private static SaveData.WeaponSave toSave(Weapon w) {
        if (w == null) return null;
        SaveData.WeaponSave s = new SaveData.WeaponSave();
        s.name   = w.name;
        s.rarity = w.rarity.name();
        s.type   = w.type.name();
        for (int i = 0; i < w.getSocketCount(); i++) {
            s.sockets.add(toSocketSave(w.getSocket(i)));
        }
        return s;
    }

    private static Weapon fromSave(SaveData.WeaponSave s) {
        if (s == null) return null;
        Weapon w = new Weapon(s.name, Rarity.valueOf(s.rarity), WeaponType.valueOf(s.type));
        if (s.sockets != null) {
            for (int i = 0; i < s.sockets.size() && i < w.getSocketCount(); i++) {
                w.setSocket(i, fromSocketSave(s.sockets.get(i)));
            }
        }
        return w;
    }

    // ------------------------------------------------------------------
    // Armor
    // ------------------------------------------------------------------

    private static SaveData.ArmorSave toSave(Armor a) {
        if (a == null) return null;
        SaveData.ArmorSave s = new SaveData.ArmorSave();
        s.name   = a.name;
        s.rarity = a.rarity.name();
        s.type   = a.type.name();
        return s;
    }

    private static Armor fromSave(SaveData.ArmorSave s) {
        if (s == null) return null;
        // Le constructeur d'Armor recalcule lui-même les bonus de stats
        // (applyTypeStats()) à partir du type + de la rareté.
        return new Armor(s.name, Rarity.valueOf(s.rarity), ArmorType.valueOf(s.type));
    }

    // ------------------------------------------------------------------
    // Capacity
    // ------------------------------------------------------------------

    private static SaveData.CapacitySave toSave(Capacity c) {
        if (c == null) return null;
        SaveData.CapacitySave s = new SaveData.CapacitySave();
        s.name         = c.name;
        s.rarity       = c.rarity.name();
        s.effectName   = c.mainEffect.name;
        s.effectDamage = c.mainEffect.baseDamage;
        s.effectSpeed  = c.mainEffect.speed;
        s.effectRadius = c.mainEffect.radius;
        s.effectType   = c.mainEffect.type;
        s.effectCooldown = c.mainEffect.cooldown;
        for (CapacityModifier m : c.modifiers) {
            SaveData.ModifierSave ms = new SaveData.ModifierSave();
            ms.type      = m.type.name();
            ms.intensity = m.intensity;
            s.modifiers.add(ms);
        }
        for (int i = 0; i < c.getSocketCount(); i++) {
            s.sockets.add(toSocketSave(c.getSocket(i)));
        }
        return s;
    }

    private static Capacity fromSave(SaveData.CapacitySave s) {
        if (s == null) return null;
        CapacityEffect effect = new CapacityEffect(
            s.effectName, s.effectDamage, s.effectSpeed, s.effectRadius, s.effectType, s.effectCooldown);
        Capacity c = new Capacity(s.name, Rarity.valueOf(s.rarity), effect);
        for (SaveData.ModifierSave ms : s.modifiers) {
            c.addModifier(new CapacityModifier(CapacityModifier.ModType.valueOf(ms.type), ms.intensity));
        }
        if (s.sockets != null) {
            for (int i = 0; i < s.sockets.size() && i < c.getSocketCount(); i++) {
                c.setSocket(i, fromSocketSave(s.sockets.get(i)));
            }
        }
        return c;
    }

    // ------------------------------------------------------------------
    // Sockets (ItemModifier) — partagé Weapon / Capacity
    // ------------------------------------------------------------------

    private static SaveData.SocketSave toSocketSave(ItemModifier mod) {
        SaveData.SocketSave ss = new SaveData.SocketSave();
        if (mod != null) {
            ss.modifierId = mod.modifierId;
            ss.rarity = mod.rarity.name();
        }
        return ss;
    }

    private static ItemModifier fromSocketSave(SaveData.SocketSave ss) {
        if (ss == null || ss.modifierId == null) return null;
        return ItemGenerator.createModifierById(ss.modifierId, Rarity.valueOf(ss.rarity));
    }

    // ------------------------------------------------------------------
    // Artifact
    // ------------------------------------------------------------------

    private static SaveData.ArtifactSave toSave(Artifact a) {
        if (a == null) return null;
        SaveData.ArtifactSave s = new SaveData.ArtifactSave();
        s.name            = a.name;
        s.rarity          = a.rarity.name();
        s.passiveEffectId = a.passiveEffectId;
        return s;
    }

    private static Artifact fromSave(SaveData.ArtifactSave s) {
        if (s == null) return null;
        // La logique de l'effet (Consumer<Object>) n'est pas sérialisable : on la
        // reconstruit via l'ID, ItemGenerator restant la seule source de vérité
        // pour l'association passiveEffectId -> comportement.
        return ItemGenerator.createArtifactById(s.passiveEffectId, s.name, Rarity.valueOf(s.rarity));
    }

    // ------------------------------------------------------------------
    // Loot bar — wrapper générique (un slot peut contenir n'importe lequel
    // des 5 types d'Item, contrairement aux 4 slots d'équipement typés)
    // ------------------------------------------------------------------

    private static SaveData.ItemSlotSave toItemSlotSave(Item item) {
        SaveData.ItemSlotSave s = new SaveData.ItemSlotSave();
        if (item instanceof Weapon) {
            s.kind = "WEAPON";
            s.weapon = toSave((Weapon) item);
        } else if (item instanceof Armor) {
            s.kind = "ARMOR";
            s.armor = toSave((Armor) item);
        } else if (item instanceof Capacity) {
            s.kind = "CAPACITY";
            s.capacity = toSave((Capacity) item);
        } else if (item instanceof Artifact) {
            s.kind = "ARTIFACT";
            s.artifact = toSave((Artifact) item);
        } else if (item instanceof ItemModifier) {
            s.kind = "MODIFIER";
            s.modifier = toSocketSave((ItemModifier) item);
        }
        // item == null => s.kind reste null (slot vide)
        return s;
    }

    private static Item fromItemSlotSave(SaveData.ItemSlotSave s) {
        if (s == null || s.kind == null) return null;
        switch (s.kind) {
            case "WEAPON":   return fromSave(s.weapon);
            case "ARMOR":    return fromSave(s.armor);
            case "CAPACITY": return fromSave(s.capacity);
            case "ARTIFACT": return fromSave(s.artifact);
            case "MODIFIER": return fromSocketSave(s.modifier);
            default:         return null;
        }
    }
}
