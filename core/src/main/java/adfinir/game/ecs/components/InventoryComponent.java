package adfinir.game.ecs.components;

import adfinir.game.inventory.*;
import adfinir.game.player.StatSheet;
import com.badlogic.ashley.core.Component;

/**
 * Composant gérant l'équipement du joueur.
 * Gère 4 slots : Weapon, Capacity, Armor, Artifact.
 */
public class InventoryComponent implements Component {
    public Weapon weapon;
    public Capacity capacity;
    public Armor armor;
    public Artifact artifact;

    /**
     * Recalcule les bonus de stats sur la StatSheet du joueur en fonction de l'équipement actuel.
     */
    public void updateStats(StatSheet stats) {
        stats.clearBonus();

        // Armor bonus
        if (armor != null) {
            stats.applyBonusMap(armor.getStatBonuses());
        }

        // Artifact bonus
        if (artifact != null) {
            stats.applyBonusMap(artifact.getStatBonuses());
        }

        // Bonus des mods (WeaponComboMod / CapacityEffectMod) socketés dans
        // l'arme et la capacité actives — un mod peut porter des bonus de
        // stats en plus de son effet de combat (combo / effet de capacité).
        if (weapon != null) {
            for (int i = 0; i < weapon.getSocketCount(); i++) {
                ItemModifier mod = weapon.getSocket(i);
                if (mod != null) stats.applyBonusMap(mod.getStatBonuses());
            }
        }
        if (capacity != null) {
            for (int i = 0; i < capacity.getSocketCount(); i++) {
                ItemModifier mod = capacity.getSocket(i);
                if (mod != null) stats.applyBonusMap(mod.getStatBonuses());
            }
        }
    }

    public void equipWeapon(Weapon newWeapon) {
        this.weapon = newWeapon;
    }

    public void equipCapacity(Capacity newCapacity) {
        this.capacity = newCapacity;
    }

    public void equipArmor(Armor newArmor) {
        this.armor = newArmor;
    }

    public void equipArtifact(Artifact newArtifact) {
        this.artifact = newArtifact;
    }

    /**
     * Équipe l'item du slot {@code index} de la barre de loot. L'ancien équipement
     * du même type repart dans ce même slot de la barre (échange).
     * Ne fait rien si le slot est vide.
     *
     * @return true si l'arme a changé (le CombatComponent doit être resynchronisé).
     */
    public boolean equipFromBar(LootBarComponent bar, int index) {
        if (bar == null || index < 0 || index >= LootBarComponent.CAPACITY) return false;
        Item incoming = bar.slots[index];
        if (incoming == null) return false;

        boolean weaponChanged = false;
        if (incoming instanceof Weapon) {
            bar.slots[index] = this.weapon;
            this.weapon = (Weapon) incoming;
            weaponChanged = true;
        } else if (incoming instanceof Capacity) {
            bar.slots[index] = this.capacity;
            this.capacity = (Capacity) incoming;
        } else if (incoming instanceof Armor) {
            bar.slots[index] = this.armor;
            this.armor = (Armor) incoming;
        } else if (incoming instanceof Artifact) {
            bar.slots[index] = this.artifact;
            this.artifact = (Artifact) incoming;
        } else {
            return false;
        }

        bar.selectedIndex = index;
        return weaponChanged;
    }

    /**
     * Comme {@link #equipFromBar}, mais resynchronise aussi CombatComponent.weapon
     * (si l'arme a changé) et recalcule la StatSheet. Point d'entrée unique utilisé
     * par la sélection clavier (1-8) et le clic souris sur la barre.
     */
    public void equipFromBarAndSync(LootBarComponent bar, int index, CombatComponent combat, StatSheet stats) {
        boolean weaponChanged = equipFromBar(bar, index);
        if (weaponChanged && combat != null) {
            combat.weapon = this.weapon;
        }
        if (stats != null) {
            updateStats(stats);
        }
    }

    // ------------------------------------------------------------------
    // Sockets — extraction / implantation / échange de ItemModifier
    // ------------------------------------------------------------------
    //
    // Weapon.getActiveCombo() / Capacity.getActiveEffect() / getActiveModifiers()
    // lisent les sockets à la volée (pas de cache), donc CombatComponent/CombatSystem
    // reflètent automatiquement tout changement de socket dès l'appel suivant :
    // aucune resynchronisation manuelle du combat n'est nécessaire ici, seule
    // StatSheet doit être recalculée (bonus de stats portés par certains mods).

    private boolean hasSocket(Item item, int index) {
        if (item instanceof Weapon) return index >= 0 && index < ((Weapon) item).getSocketCount();
        if (item instanceof Capacity) return index >= 0 && index < ((Capacity) item).getSocketCount();
        return false;
    }

    private ItemModifier peekSocket(Item item, int index) {
        if (item instanceof Weapon) return ((Weapon) item).getSocket(index);
        if (item instanceof Capacity) return ((Capacity) item).getSocket(index);
        return null;
    }

    private ItemModifier removeSocket(Item item, int index) {
        if (item instanceof Weapon) return ((Weapon) item).setSocket(index, null);
        if (item instanceof Capacity) return ((Capacity) item).setSocket(index, null);
        return null;
    }

    private void putSocket(Item item, int index, ItemModifier mod) {
        if (item instanceof Weapon) ((Weapon) item).setSocket(index, mod);
        else if (item instanceof Capacity) ((Capacity) item).setSocket(index, mod);
    }

    private boolean isCompatible(ItemModifier mod, Item host) {
        return mod != null && mod.isCompatibleWith(host);
    }

    /**
     * Retire le mod du socket {@code socketIndex} de {@code source} et le place
     * dans le premier slot libre de la loot bar. Si la barre est pleine,
     * l'extraction est annulée (le mod reste dans son socket) — cf. règle de
     * design §5 du prompt : "sinon annule".
     *
     * @return true si l'extraction a eu lieu.
     */
    public boolean extractToLootBar(Item source, int socketIndex, LootBarComponent bar) {
        if (bar == null || !hasSocket(source, socketIndex)) return false;
        ItemModifier mod = peekSocket(source, socketIndex);
        if (mod == null) return false; // socket déjà vide

        int slot = bar.firstEmptySlot();
        if (slot == -1) return false; // barre pleine : annulé, rien ne bouge

        removeSocket(source, socketIndex);
        bar.slots[slot] = mod;
        return true;
    }

    /**
     * Implante le mod du slot {@code lootBarIndex} de la loot bar dans le socket
     * {@code socketIndex} de {@code target}. Si le socket est déjà occupé, les
     * deux sont échangés (l'ancien mod repart dans ce même slot de la barre).
     * Vérifie la compatibilité de type (WeaponComboMod -> Weapon uniquement,
     * CapacityEffectMod -> Capacity uniquement) : ne fait rien si incompatible.
     *
     * @return true si l'implantation a eu lieu.
     */
    public boolean implantFromLootBar(Item target, int socketIndex, LootBarComponent bar, int lootBarIndex) {
        if (bar == null || lootBarIndex < 0 || lootBarIndex >= LootBarComponent.CAPACITY) return false;
        if (!hasSocket(target, socketIndex)) return false;

        Item incoming = bar.slots[lootBarIndex];
        if (!(incoming instanceof ItemModifier)) return false;
        ItemModifier mod = (ItemModifier) incoming;
        if (!isCompatible(mod, target)) return false;

        ItemModifier previous = removeSocket(target, socketIndex);
        putSocket(target, socketIndex, mod);
        bar.slots[lootBarIndex] = previous; // échange, ou null si le socket était vide

        return true;
    }

    /**
     * Échange deux mods entre deux sockets d'items équipés (armeC arme,
     * capacitéC capacité). Un des deux sockets peut être vide : dans ce cas
     * l'opération équivaut à un déplacement. Règle de compatibilité stricte
     * par défaut : itemA et itemB doivent être du même type concret exact.
     *
     * @return true si l'échange a eu lieu.
     */
    public boolean swapSockets(Item itemA, int idxA, Item itemB, int idxB) {
        if (itemA == null || itemB == null) return false;
        if (itemA.getClass() != itemB.getClass()) return false;
        if (!hasSocket(itemA, idxA) || !hasSocket(itemB, idxB)) return false;
        if (itemA == itemB && idxA == idxB) return false; // rien à échanger avec soi-même

        ItemModifier modA = peekSocket(itemA, idxA);
        ItemModifier modB = peekSocket(itemB, idxB);

        // Même type concret => même hôte compatible pour les deux, mais on
        // vérifie explicitement au cas où un mod incompatible aurait été
        // forcé dans un socket par un autre chemin de code.
        if (modA != null && !isCompatible(modA, itemB)) return false;
        if (modB != null && !isCompatible(modB, itemA)) return false;

        removeSocket(itemA, idxA);
        removeSocket(itemB, idxB);
        putSocket(itemA, idxA, modB);
        putSocket(itemB, idxB, modA);
        return true;
    }

    public boolean extractToLootBarAndSync(Item source, int socketIndex, LootBarComponent bar, StatSheet stats) {
        boolean ok = extractToLootBar(source, socketIndex, bar);
        if (ok && stats != null) updateStats(stats);
        return ok;
    }

    public boolean implantFromLootBarAndSync(Item target, int socketIndex, LootBarComponent bar,
                                             int lootBarIndex, StatSheet stats) {
        boolean ok = implantFromLootBar(target, socketIndex, bar, lootBarIndex);
        if (ok && stats != null) updateStats(stats);
        return ok;
    }

    public boolean swapSocketsAndSync(Item itemA, int idxA, Item itemB, int idxB, StatSheet stats) {
        boolean ok = swapSockets(itemA, idxA, itemB, idxB);
        if (ok && stats != null) updateStats(stats);
        return ok;
    }
}
