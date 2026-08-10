package adfinir.game.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;

import java.util.EnumMap;
import java.util.Map;

/**
 * Touche assignée à chaque GameAction, modifiable via SettingsOverlay et
 * persistée dans les Preferences libGDX (indépendant du fichier de
 * sauvegarde de partie — un réglage, pas une progression).
 *
 * rebind() échange les touches plutôt que de créer un doublon silencieux :
 * si la touche demandée est déjà utilisée par une autre action, cette autre
 * action récupère l'ancienne touche de celle qu'on réassigne.
 */
public final class KeyBindings {

    private static final String PREFS_NAME = "adfinir-keybindings";

    private static Preferences prefs;
    private static final Map<GameAction, Integer> bindings = new EnumMap<>(GameAction.class);
    private static boolean loaded = false;

    private KeyBindings() {}

    private static void ensureLoaded() {
        if (loaded) return;
        prefs = Gdx.app.getPreferences(PREFS_NAME);
        for (GameAction action : GameAction.values()) {
            bindings.put(action, prefs.getInteger(action.name(), action.defaultKey));
        }
        loaded = true;
    }

    public static int get(GameAction action) {
        ensureLoaded();
        return bindings.get(action);
    }

    public static boolean isPressed(GameAction action) {
        return Gdx.input.isKeyPressed(get(action));
    }

    public static boolean isJustPressed(GameAction action) {
        return Gdx.input.isKeyJustPressed(get(action));
    }

    public static void rebind(GameAction action, int newKey) {
        ensureLoaded();
        int oldKey = bindings.get(action);
        if (oldKey == newKey) return;

        for (GameAction other : GameAction.values()) {
            if (other != action && bindings.get(other) == newKey) {
                bindings.put(other, oldKey);
                prefs.putInteger(other.name(), oldKey);
            }
        }
        bindings.put(action, newKey);
        prefs.putInteger(action.name(), newKey);
        prefs.flush();
    }

    public static void resetDefaults() {
        ensureLoaded();
        for (GameAction action : GameAction.values()) {
            bindings.put(action, action.defaultKey);
            prefs.putInteger(action.name(), action.defaultKey);
        }
        prefs.flush();
    }

    public static String keyName(GameAction action) {
        return Input.Keys.toString(get(action));
    }
}
