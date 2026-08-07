package adfinir.game.save;

import java.util.ArrayList;
import java.util.List;

/**
 * Structure de données brute pour la sauvegarde (sérialisée en JSON via
 * com.badlogic.gdx.utils.Json).
 *
 * Ne contient volontairement que des types primitifs / String / listes —
 * jamais d'objets du domaine (Weapon, Armor, Capacity, Artifact) car :
 *  - leurs champs sont `final` (pas de constructeur vide exploitable par Json) ;
 *  - Artifact.effectLogic est un Consumer<Object> (lambda), non sérialisable.
 *
 * La reconstruction des objets réels se fait dans SaveManager, en particulier
 * via ItemGenerator.createArtifactById(...) pour les artefacts.
 *
 * Le layout du donjon (dungeonTiles + spawn/exit) et la position du joueur
 * sont sauvegardés tels quels : DungeonGenerator n'étant pas seedé, on ne peut
 * pas régénérer le même étage à partir d'un simple numéro de niveau — il faut
 * conserver la grille exacte pour que playerX/playerY restent valides.
 *
 * Limite connue : les ennemis et le loot déjà ramassé/vaincu ne sont PAS
 * sauvegardés. Au chargement, GameScreen respawn ennemis + loot sur toutes
 * les tiles TILE_LOOT du layout restauré, comme à l'entrée d'un étage neuf —
 * un objet déjà ramassé avant la sauvegarde réapparaîtra donc.
 */
public class SaveData {

    public int currentLevel = 1;

    public float currentHp;
    public float currentStamina;

    public float playerX;
    public float playerY;

    /** Grille de tiles du donjon (grid[row][col]), copie exacte de DungeonMap. */
    public int[][] dungeonTiles;
    public int spawnCol;
    public int spawnRow;
    public int exitCol;
    public int exitRow;

    public WeaponSave   weapon;
    public ArmorSave    armor;
    public CapacitySave capacity;
    public ArtifactSave artifact;

    public static class WeaponSave {
        public String name;
        public String rarity;   // Rarity.name()
        public String type;     // WeaponType.name()
        public List<AttackSave> combos = new ArrayList<>();
    }

    public static class AttackSave {
        public String name;
        public float minDamage;
        public float maxDamage;
        public float cooldown;
        public float duration;
        public float knockback;
        public String element;  // nullable
        public float areaOfEffect;
    }

    public static class ArmorSave {
        public String name;
        public String rarity;   // Rarity.name()
        public String type;     // ArmorType.name()
    }

    public static class CapacitySave {
        public String name;
        public String rarity;   // Rarity.name()
        public String effectName;
        public float effectDamage;
        public float effectSpeed;
        public float effectRadius;
        public String effectType;
        public List<ModifierSave> modifiers = new ArrayList<>();
    }

    public static class ModifierSave {
        public String type;     // CapacityModifier.ModType.name()
        public float intensity;
    }

    public static class ArtifactSave {
        public String name;
        public String rarity;           // Rarity.name()
        public String passiveEffectId;  // reconstruit via ItemGenerator.createArtifactById
    }
}
