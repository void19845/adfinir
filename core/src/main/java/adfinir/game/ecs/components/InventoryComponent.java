package adfinir.game.ecs.components;

import adfinir.game.items.Artifact;
import adfinir.game.items.Item;
import com.badlogic.ashley.core.Component;

import java.util.ArrayList;
import java.util.List;

public class InventoryComponent implements Component {

    // ── Items ─────────────────────────────────────────────────────────────
    public final List<Item> items = new ArrayList<>();
    public Item equippedWeapon = null;
    public Item equippedArmor  = null;

    // ── Artéfacts (style Genshin — 5 emplacements) ────────────────────────
    public Artifact slotFlower  = null;
    public Artifact slotFeather = null;
    public Artifact slotClock   = null;
    public Artifact slotCup     = null;
    public Artifact slotCrown   = null;
    public final List<Artifact> artifacts = new ArrayList<>();

    // ── Items ─────────────────────────────────────────────────────────────

    public void addItem(Item item) { items.add(item); }

    /** Ajoute l'item et l'équipe automatiquement si le slot est libre. */
    public void autoEquip(Item item) {
        addItem(item);
        if (item.type == Item.Type.WEAPON && equippedWeapon == null) equippedWeapon = item;
        else if (item.type == Item.Type.ARMOR && equippedArmor == null) equippedArmor = item;
    }

    /** Équipe manuellement (remplace l'existant). */
    public void equip(Item item) {
        if (item.type == Item.Type.WEAPON) equippedWeapon = item;
        else if (item.type == Item.Type.ARMOR) equippedArmor = item;
    }

    // ── Artéfacts ─────────────────────────────────────────────────────────

    /** Ajoute un artéfact dans le sac. */
    public void addArtifact(Artifact art) { artifacts.add(art); }

    /** Équipe un artéfact dans le bon emplacement (remplace l'existant). */
    public void equipArtifact(Artifact art) {
        switch (art.slot) {
            case FLOWER:  slotFlower  = art; break;
            case FEATHER: slotFeather = art; break;
            case CLOCK:   slotClock   = art; break;
            case CUP:     slotCup     = art; break;
            case CROWN:   slotCrown   = art; break;
        }
    }

    /** Retourne l'artéfact équipé dans un emplacement donné. */
    public Artifact getEquippedArtifact(Artifact.Slot slot) {
        switch (slot) {
            case FLOWER:  return slotFlower;
            case FEATHER: return slotFeather;
            case CLOCK:   return slotClock;
            case CUP:     return slotCup;
            case CROWN:   return slotCrown;
        }
        return null;
    }

    // ── Bonus calculés ────────────────────────────────────────────────────

    public float getWeaponAtkBonus() {
        return equippedWeapon != null ? equippedWeapon.atkBonus : 0f;
    }

    public float getArmorDefBonus() {
        return equippedArmor != null ? equippedArmor.defBonus : 0f;
    }

    public float getArtifactAtk() {
        float v = 0f;
        for (Artifact a : equippedArtifacts()) if (a != null) v += a.atkBonus;
        return v;
    }

    public float getArtifactDef() {
        float v = 0f;
        for (Artifact a : equippedArtifacts()) if (a != null) v += a.defBonus;
        return v;
    }

    public float getArtifactCritRate() {
        float v = 0f;
        for (Artifact a : equippedArtifacts()) if (a != null) v += a.critRateBonus;
        return v;
    }

    public float getArtifactCritDmg() {
        float v = 0f;
        for (Artifact a : equippedArtifacts()) if (a != null) v += a.critDmgBonus;
        return v;
    }

    /** Nombre d'artéfacts équipés. */
    public int equippedArtifactCount() {
        int n = 0;
        for (Artifact.Slot s : Artifact.Slot.values())
            if (getEquippedArtifact(s) != null) n++;
        return n;
    }

    private Artifact[] equippedArtifacts() {
        return new Artifact[]{ slotFlower, slotFeather, slotClock, slotCup, slotCrown };
    }
}
