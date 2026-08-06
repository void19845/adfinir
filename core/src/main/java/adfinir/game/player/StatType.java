package adfinir.game.player;

/**
 * Enumère tous les types de stats du personnage.
 * Utilisé comme clé dans les maps de stats et les bonus d'items.
 */
public enum StatType {
    MAX_HP,       // Points de vie maximum
    ATK,          // Attaque physique
    MAG,          // Attaque magique
    DEF,          // Défense (réduit les dégâts reçus)
    SPD,          // Vitesse de déplacement
    MAX_STAMINA,  // Stamina maximum
    STAMINA_REGEN // Vitesse de régénération de la stamina (par seconde)
}
