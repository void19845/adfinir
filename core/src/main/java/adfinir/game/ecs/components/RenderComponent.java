package adfinir.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Pool;

public class RenderComponent implements Component, Pool.Poolable {
    /** Largeur et hauteur en pixels pour le rendu. */
    public float width  = 16f;
    public float height = 16f;
    /** Couleur utilisée pour le rendu de remplacement (avant d'avoir des sprites). */
    public Color color  = new Color(Color.WHITE);

    @Override
    public void reset() {
        width  = 16f;
        height = 16f;
        color.set(Color.WHITE);
    }
}
