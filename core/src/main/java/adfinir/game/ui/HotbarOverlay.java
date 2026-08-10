package adfinir.game.ui;

import adfinir.game.ecs.components.CombatComponent;
import adfinir.game.ecs.components.InventoryComponent;
import adfinir.game.inventory.Capacity;
import adfinir.game.inventory.Item;
import adfinir.game.inventory.Weapon;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;

/**
 * Barre d'action permanente en bas de l'écran (façon LoL/Minecraft) :
 * l'arme équipée (clic gauche) suivie des 4 sorts (touches 1-4), avec cooldown visuel.
 */
public class HotbarOverlay implements Disposable {

    private static final float SLOT_SIZE = 40f;
    private static final float SLOT_GAP  = 6f;
    private static final float MARGIN_BOTTOM = 14f;

    private final SpriteBatch   batch;
    private final BitmapFont    font;
    private final ShapeRenderer shapes;

    private InventoryComponent inventory;
    private CombatComponent combat;

    public HotbarOverlay() {
        batch  = new SpriteBatch();
        font   = new BitmapFont();
        shapes = new ShapeRenderer();
    }

    public void update(InventoryComponent inventory, CombatComponent combat) {
        this.inventory = inventory;
        this.combat = combat;
    }

    public void draw(int screenW) {
        if (inventory == null || combat == null) return;

        int slotCount = 1 + InventoryComponent.SPELL_SLOTS;
        float totalW = slotCount * SLOT_SIZE + (slotCount - 1) * SLOT_GAP;
        float startX = (screenW - totalW) / 2f;
        float y = MARGIN_BOTTOM;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Fonds + bordures
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.6f);
        for (int i = 0; i < slotCount; i++) {
            shapes.rect(startX + i * (SLOT_SIZE + SLOT_GAP), y, SLOT_SIZE, SLOT_SIZE);
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Cooldown : voile sombre montant proportionnel au temps restant
        drawCooldownOverlay(startX, y, weaponCooldownRatio());
        for (int i = 0; i < InventoryComponent.SPELL_SLOTS; i++) {
            float slotX = startX + (i + 1) * (SLOT_SIZE + SLOT_GAP);
            drawCooldownOverlay(slotX, y, spellCooldownRatio(i));
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(RarityColors.get(inventory.weapon != null ? inventory.weapon.rarity : adfinir.game.inventory.Rarity.COMMON));
        shapes.rect(startX, y, SLOT_SIZE, SLOT_SIZE);
        for (int i = 0; i < InventoryComponent.SPELL_SLOTS; i++) {
            float slotX = startX + (i + 1) * (SLOT_SIZE + SLOT_GAP);
            Capacity spell = inventory.spells[i];
            shapes.setColor(spell != null ? RarityColors.get(spell.rarity) : Color.DARK_GRAY);
            shapes.rect(slotX, y, SLOT_SIZE, SLOT_SIZE);
        }
        shapes.end();

        // Étiquettes des touches + noms courts
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Clic G", startX + 2f, y + SLOT_SIZE + 12f);
        drawItemLabel(inventory.weapon != null ? inventory.weapon.name : "-", startX, y);

        for (int i = 0; i < InventoryComponent.SPELL_SLOTS; i++) {
            float slotX = startX + (i + 1) * (SLOT_SIZE + SLOT_GAP);
            font.setColor(Color.WHITE);
            font.draw(batch, String.valueOf(i + 1), slotX + 2f, y + SLOT_SIZE + 12f);
            Capacity spell = inventory.spells[i];
            drawItemLabel(spell != null ? spell.mainEffect.name : "-", slotX, y);
        }
        batch.end();

        // Survol : tooltip avec le nom complet
        float mouseX = UiInput.mouseX();
        float mouseY = UiInput.mouseY();
        Item hovered = null;
        if (new Rectangle(startX, y, SLOT_SIZE, SLOT_SIZE).contains(mouseX, mouseY)) {
            hovered = inventory.weapon;
        }
        for (int i = 0; i < InventoryComponent.SPELL_SLOTS; i++) {
            float slotX = startX + (i + 1) * (SLOT_SIZE + SLOT_GAP);
            if (new Rectangle(slotX, y, SLOT_SIZE, SLOT_SIZE).contains(mouseX, mouseY)) {
                hovered = inventory.spells[i];
            }
        }
        if (hovered != null) {
            Tooltip.draw(shapes, batch, font, mouseX, mouseY, hovered.name + " (" + hovered.rarity.name() + ")");
        }
    }

    private void drawCooldownOverlay(float x, float y, float ratio) {
        if (ratio <= 0f) return;
        shapes.setColor(0f, 0f, 0f, 0.75f);
        shapes.rect(x, y, SLOT_SIZE, SLOT_SIZE * ratio);
    }

    private void drawItemLabel(String name, float x, float y) {
        font.setColor(Color.LIGHT_GRAY);
        String label = name.length() > 8 ? name.substring(0, 7) + "." : name;
        font.draw(batch, label, x + 2f, y + 12f);
    }

    private float weaponCooldownRatio() {
        if (combat.weapon == null || combat.timer <= 0f) return 0f;
        float maxCd = combat.weapon.getModifiedCooldown(combat.activeComboIndex);
        return maxCd > 0f ? Math.min(1f, combat.timer / maxCd) : 0f;
    }

    private float spellCooldownRatio(int slot) {
        Capacity spell = inventory.spells[slot];
        if (spell == null || combat.spellTimers[slot] <= 0f) return 0f;
        // On ne connaît pas le cooldown max exact ici sans recalculer les modificateurs ;
        // 3s est la base de CapacityBurst.resolve() — approximation suffisante pour l'affichage.
        return Math.min(1f, combat.spellTimers[slot] / 3f);
    }

    public void resize(int w, int h) {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, w, h);
        shapes.getProjectionMatrix().setToOrtho2D(0, 0, w, h);
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        shapes.dispose();
    }
}
