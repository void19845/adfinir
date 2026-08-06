package adfinir.game.items;

public class Item {

    public enum Rarity { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY }
    public enum Type   { WEAPON, ARMOR, ACCESSORY, RESOURCE, ARTIFACT }

    public final String name;
    public final Rarity rarity;
    public final Type   type;
    public final float  atkBonus;
    public final float  defBonus;
    public final int    xpValue;

    public Item(String name, Rarity rarity, Type type, float atkBonus, float defBonus, int xpValue) {
        this.name = name; this.rarity = rarity; this.type = type;
        this.atkBonus = atkBonus; this.defBonus = defBonus; this.xpValue = xpValue;
    }

    // ---- Items prédéfinis ----
    public static final Item RUSTY_SWORD   = new Item("Rusty Sword",   Rarity.COMMON,   Type.WEAPON,   5f,  0f, 0);
    public static final Item IRON_SWORD    = new Item("Iron Sword",    Rarity.UNCOMMON, Type.WEAPON,  12f,  0f, 0);
    public static final Item LEATHER_ARMOR = new Item("Leather Armor", Rarity.COMMON,   Type.ARMOR,    0f,  5f, 0);
    public static final Item IRON_SHIELD   = new Item("Iron Shield",   Rarity.UNCOMMON, Type.ARMOR,    0f, 10f, 0);
    public static final Item BONE          = new Item("Bone",          Rarity.COMMON,   Type.RESOURCE, 0f,  0f, 0);
    public static final Item GEM_FRAGMENT  = new Item("Gem Fragment",  Rarity.RARE,     Type.RESOURCE, 0f,  0f, 0);

    @Override
    public String toString() { return name; }
}
