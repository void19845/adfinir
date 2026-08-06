package adfinir.game;

/** État global partagé entre les systèmes ECS et les écrans. */
public final class GameState {
    /** Vrai quand l'inventaire est ouvert — désactive le mouvement du joueur. */
    public static boolean inventoryOpen = false;

    private GameState() {}
}
