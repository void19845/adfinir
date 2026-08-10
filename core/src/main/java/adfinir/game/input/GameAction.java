package adfinir.game.input;

import com.badlogic.gdx.Input;

/**
 * Actions du jeu dont la touche est configurable (voir KeyBindings et
 * SettingsOverlay). Les clics souris (attaque au clic gauche, glisser-déposer,
 * clics de menu) et les touches 1-8 de la loot bar restent fixes — seules les
 * actions clavier à touche unique listées ici sont ré-assignables.
 */
public enum GameAction {
    MOVE_UP("Aller en haut", Input.Keys.W),
    MOVE_DOWN("Aller en bas", Input.Keys.S),
    MOVE_LEFT("Aller à gauche", Input.Keys.A),
    MOVE_RIGHT("Aller à droite", Input.Keys.D),
    ATTACK("Attaque (arme)", Input.Keys.SPACE),
    CAPACITY("Sort (capacité)", Input.Keys.Q),
    DASH("Dash", Input.Keys.TAB),
    SWITCH_WEAPON("Changer d'arme", Input.Keys.X),
    PICKUP("Ramasser au sol", Input.Keys.F),
    INVENTORY("Ouvrir l'inventaire", Input.Keys.E),
    SHOP("Ouvrir la boutique", Input.Keys.P),
    STATS("Afficher les stats", Input.Keys.K),
    EXTRACT_SOCKET("Extraire un socket", Input.Keys.R);

    public final String label;
    public final int defaultKey;

    GameAction(String label, int defaultKey) {
        this.label = label;
        this.defaultKey = defaultKey;
    }
}
