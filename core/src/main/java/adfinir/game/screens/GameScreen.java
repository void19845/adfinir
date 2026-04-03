package adfinir.game.screens;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;

import adfinir.game.DamageQueue;
import adfinir.game.GameState;
import adfinir.game.Main;
import adfinir.game.ecs.components.*;
import adfinir.game.ecs.components.RenderComponent.EntityType;
import adfinir.game.ecs.systems.*;
import adfinir.game.items.Artifact;
import adfinir.game.items.Item;
import adfinir.game.items.ItemDatabase;
import adfinir.game.world.WorldMap;
import adfinir.game.world.WorldRenderer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameScreen implements Screen {

    // ── Résolution logique ────────────────────────────────────────────────
    private static final int VIEW_W = 320;
    private static final int VIEW_H = 240;

    // ── Inventaire (barre bas) ────────────────────────────────────────────
    private static final int SLOT_SIZE  = 26;
    private static final int SLOT_GAP   = 4;
    private static final int SLOT_COUNT = 6;

    // ── Minimap ───────────────────────────────────────────────────────────
    private static final int MM_W = 72;
    private static final int MM_H = 54;

    private final Main game;

    private FitViewport        viewport;
    private OrthographicCamera camera;
    private SpriteBatch        batch;
    private ShapeRenderer      sr;
    private BitmapFont         font;
    private BitmapFont         fontBig;
    private final Matrix4      hudMatrix = new Matrix4();

    private WorldMap      worldMap;
    private WorldRenderer worldRenderer;
    private Engine        engine;

    private Entity             player;
    private TransformComponent playerTransform;

    private HealthBarRenderSystem healthBarSystem;

    // Mappers pour minimap / HUD
    private final ComponentMapper<TransformComponent> tmMap  = ComponentMapper.getFor(TransformComponent.class);
    private final ComponentMapper<MobAIComponent>     aimMap = ComponentMapper.getFor(MobAIComponent.class);
    private ImmutableArray<Entity> mobs;

    // ── État UI ───────────────────────────────────────────────────────────
    private int     selectedSlot     = 0;    // slot barre du bas (touches 1-6)
    private float   attackFlash      = 0f;   // flash attaque rapide (doré)
    private float   powerFlash       = 0f;   // flash attaque puissante (rouge)
    private float   damageOverlay    = 0f;   // flash rouge quand joueur touché
    private float   lastPlayerHp     = 100f;

    // ── Inventaire Genshin ────────────────────────────────────────────────
    private int     invTab    = 0;  // 0=Armes 1=Artéfacts 2=Armure 3=Personnage
    private int     invCursor = 0;  // index sélectionné dans la liste du tab courant

    // ── Damage numbers ────────────────────────────────────────────────────
    private static class DmgNum {
        float wx, wy;
        float vy = 28f;
        float life = 0.9f;
        String text;
        boolean crit;
        DmgNum(float x, float y, float dmg, boolean crit) {
            this.wx = x; this.wy = y;
            this.text = crit ? "!" + (int) dmg : String.valueOf((int) dmg);
            this.crit = crit;
        }
    }
    private final List<DmgNum> dmgNums = new ArrayList<>();

    // ── Level-up flash ───────────────────────────────────────────────────
    private float levelUpTimer = 0f;
    private int   lastLevel    = 1;

    public GameScreen(Main game) {
        this.game = game;
    }

    // ── show ──────────────────────────────────────────────────────────────

    @Override
    public void show() {
        camera   = new OrthographicCamera();
        viewport = new FitViewport(VIEW_W, VIEW_H, camera);
        batch    = new SpriteBatch();
        sr       = new ShapeRenderer();
        sr.setAutoShapeType(true);

        font = new BitmapFont();
        font.getData().setScale(0.48f);

        fontBig = new BitmapFont();
        fontBig.getData().setScale(0.85f);

        ItemDatabase.load();

        worldMap      = new WorldMap(220, 170);
        worldRenderer = new WorldRenderer(worldMap);

        engine = new Engine();
        engine.addSystem(new PlayerInputSystem());
        engine.addSystem(new MovementSystem(worldMap));
        engine.addSystem(new MobAISystem(worldMap));
        engine.addSystem(new CombatSystem());
        engine.addSystem(new PushSystem(worldMap));
        engine.addSystem(new RenderSystem(sr));

        healthBarSystem = new HealthBarRenderSystem(sr);
        healthBarSystem.setEngine(engine);

        mobs = engine.getEntitiesFor(
            Family.all(TransformComponent.class, MobAIComponent.class).get()
        );

        GameState.inventoryOpen = false;

        player          = createPlayer(worldMap.getSpawnPixelX(), worldMap.getSpawnPixelY());
        playerTransform = player.getComponent(TransformComponent.class);
        engine.addEntity(player);

        spawnMobs(6);

        HealthComponent hp = player.getComponent(HealthComponent.class);
        if (hp != null) lastPlayerHp = hp.currentHealth;
    }

    // ── Création joueur / mobs ────────────────────────────────────────────

    private Entity createPlayer(float x, float y) {
        Entity e = new Entity();

        TransformComponent t = new TransformComponent();
        t.x = x; t.y = y;

        VelocityComponent vel = new VelocityComponent();

        RenderComponent render = new RenderComponent();
        render.type = EntityType.PLAYER_1;
        render.width = render.height = 16f;

        PlayerInputComponent input = new PlayerInputComponent();
        input.speed     = 90f;
        input.upKey     = Input.Keys.UP;    input.downKey  = Input.Keys.DOWN;
        input.leftKey   = Input.Keys.LEFT;  input.rightKey = Input.Keys.RIGHT;
        input.attackKey = Input.Keys.SPACE;

        HealthComponent hp = new HealthComponent();
        hp.maxHealth = hp.currentHealth = 100f;

        StatsComponent    stats  = new StatsComponent();
        CombatComponent   combat = new CombatComponent();
        combat.attackRange    = 24f;
        combat.knockbackForce = 160f;
        InventoryComponent inv = new InventoryComponent();

        e.add(t); e.add(vel);    e.add(render);
        e.add(input); e.add(hp); e.add(stats);
        e.add(combat); e.add(inv);
        return e;
    }

    private void spawnMobs(int count) {
        float sx = worldMap.getSpawnPixelX(), sy = worldMap.getSpawnPixelY();
        int placed = 0;
        Random rng = new Random(42);
        while (placed < count) {
            float px = rng.nextFloat() * worldMap.getPixelWidth();
            float py = rng.nextFloat() * worldMap.getPixelHeight();
            float dx = px - sx, dy = py - sy;
            if (dx * dx + dy * dy < 250f * 250f) continue;
            if (worldMap.isSolid(px, py)) continue;
            engine.addEntity(createMob(px, py));
            placed++;
        }
    }

    private Entity createMob(float x, float y) {
        Entity e = new Entity();
        TransformComponent t = new TransformComponent(); t.x = x; t.y = y;
        VelocityComponent vel = new VelocityComponent();
        RenderComponent render = new RenderComponent();
        render.type = EntityType.MOB; render.width = render.height = 14f;
        HealthComponent hp = new HealthComponent(); hp.maxHealth = hp.currentHealth = 40f;
        MobAIComponent ai = new MobAIComponent();
        ai.patrolTargetX = x; ai.patrolTargetY = y;
        ai.aggroRange = 100f; ai.speed = 38f;
        LootDropComponent loot = new LootDropComponent(
            30,
            new Item[]{ Item.RUSTY_SWORD, Item.LEATHER_ARMOR, Item.BONE },
            new float[]{ 0.20f, 0.15f, 0.70f }
        );
        e.add(t); e.add(vel); e.add(render); e.add(hp); e.add(ai);
        e.add(new MobTagComponent()); e.add(loot);
        return e;
    }

    // ── render ────────────────────────────────────────────────────────────

    @Override
    public void render(float delta) {
        handleInput();
        updateLogic(delta);

        // Caméra
        float camX = MathUtils.clamp(playerTransform.x, VIEW_W / 2f, worldMap.getPixelWidth()  - VIEW_W / 2f);
        float camY = MathUtils.clamp(playerTransform.y, VIEW_H / 2f, worldMap.getPixelHeight() - VIEW_H / 2f);
        camera.position.x += (camX - camera.position.x) * 8f * delta;
        camera.position.y += (camY - camera.position.y) * 8f * delta;
        camera.update();

        Gdx.gl.glClearColor(0.04f, 0.10f, 0.02f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Rectangle view = new Rectangle(
            camera.position.x - VIEW_W / 2f,
            camera.position.y - VIEW_H / 2f,
            VIEW_W, VIEW_H
        );

        int sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        hudMatrix.setToOrtho2D(0, 0, sw, sh);

        // ── Passe 1 : monde + entités + barres de vie ─────────────────────
        sr.setProjectionMatrix(camera.combined);
        sr.begin(ShapeRenderer.ShapeType.Filled);

        worldRenderer.render(sr, view);

        // Flash attaque rapide (arc doré devant le joueur)
        if (attackFlash > 0) {
            float fx = playerTransform.facingX, fy = playerTransform.facingY;
            if (fx == 0 && fy == 0) fy = -1f;
            sr.setColor(1f, 0.85f, 0.15f, 1f);
            sr.circle(
                playerTransform.x + fx * 14f,
                playerTransform.y + fy * 14f,
                10f + (1f - attackFlash / 0.18f) * 4f, 10
            );
            sr.setColor(1f, 0.95f, 0.5f, 1f);
            sr.circle(playerTransform.x, playerTransform.y, 9f + (1f - attackFlash / 0.18f) * 3f, 10);
        }

        // Flash attaque puissante (grande onde rouge/orange)
        if (powerFlash > 0) {
            float p  = powerFlash / 0.30f;        // 1→0
            float fx = playerTransform.facingX, fy = playerTransform.facingY;
            if (fx == 0 && fy == 0) fy = -1f;
            // Onde extérieure orange
            sr.setColor(1f, 0.45f, 0.05f, 1f);
            sr.circle(
                playerTransform.x + fx * 20f,
                playerTransform.y + fy * 20f,
                22f + (1f - p) * 8f, 14
            );
            // Anneau intérieur rouge
            sr.setColor(0.90f, 0.10f, 0.10f, 1f);
            sr.circle(
                playerTransform.x + fx * 18f,
                playerTransform.y + fy * 18f,
                14f + (1f - p) * 5f, 12
            );
            // Éclat central blanc
            sr.setColor(1f, 0.80f, 0.50f, 1f);
            sr.circle(playerTransform.x, playerTransform.y, 11f + (1f - p) * 4f, 10);
        }

        engine.update(delta);
        healthBarSystem.update(delta);

        // Flash dégâts joueur
        if (damageOverlay > 0) {
            sr.setColor(0.9f, 0.05f, 0.05f, 1f);
            sr.circle(playerTransform.x, playerTransform.y, 10f, 10);
        }

        sr.end();

        // ── Passe 2 : damage numbers (texte en coords monde) ─────────────
        if (!dmgNums.isEmpty()) {
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            for (DmgNum dn : dmgNums) {
                if (dn.crit) {
                    fontBig.setColor(1f, 0.85f, 0.1f, Math.min(1f, dn.life / 0.3f));
                    fontBig.draw(batch, dn.text, dn.wx - 6f, dn.wy);
                } else {
                    font.setColor(0.95f, 0.95f, 0.95f, Math.min(1f, dn.life / 0.3f));
                    font.draw(batch, dn.text, dn.wx - 4f, dn.wy);
                }
            }
            batch.end();
            font.setColor(1f, 1f, 1f, 1f);
            fontBig.setColor(1f, 1f, 1f, 1f);
        }

        // ── Passe 3 : HUD formes (coords écran) ──────────────────────────
        sr.setProjectionMatrix(hudMatrix);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        drawHudShapes(sr, sw, sh);
        sr.end();

        // ── Passe 4 : HUD texte (coords écran) ───────────────────────────
        batch.setProjectionMatrix(hudMatrix);
        batch.begin();
        drawHudText(batch, sw, sh);
        batch.end();

        // Game over
        HealthComponent p1hp = player.getComponent(HealthComponent.class);
        if (p1hp != null && p1hp.currentHealth <= 0) {
            StatsComponent st = player.getComponent(StatsComponent.class);
            game.setScreen(new GameOverScreen(game, st != null ? st.level : 1));
            dispose();
        }
    }

    // ── Logique frame ─────────────────────────────────────────────────────

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (GameState.inventoryOpen) {
                GameState.inventoryOpen = false;
            } else {
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
            return;
        }

        // Toggle inventaire (E ou Tab)
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)
         || Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            GameState.inventoryOpen = !GameState.inventoryOpen;
            if (GameState.inventoryOpen) { invTab = 0; invCursor = 0; }
        }

        if (GameState.inventoryOpen) {
            handleInventoryInput();
            return;
        }

        // Sélection slot barre du bas (1 à 6)
        for (int k = 0; k < SLOT_COUNT; k++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + k)) selectedSlot = k;
        }

        // Déclencher flash attaque rapide
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
         || Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            attackFlash = 0.18f;
        }
        // Déclencher flash attaque puissante
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            powerFlash = 0.30f;
        }
    }

    private void handleInventoryInput() {
        InventoryComponent inv = player.getComponent(InventoryComponent.class);

        // Changer d'onglet (gauche/droite)
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            invTab = (invTab + 3) % 4;
            invCursor = 0;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            invTab = (invTab + 1) % 4;
            invCursor = 0;
        }

        // Naviguer dans la liste (haut/bas)
        int listSize = getTabListSize(inv);
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            if (invCursor > 0) invCursor--;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            if (invCursor < listSize - 1) invCursor++;
        }

        // Équiper (ENTRÉE)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) && inv != null && listSize > 0) {
            equipSelected(inv);
        }
    }

    private int getTabListSize(InventoryComponent inv) {
        if (inv == null) return 0;
        switch (invTab) {
            case 0: return (int) inv.items.stream().filter(i -> i.type == Item.Type.WEAPON).count();
            case 1: return inv.artifacts.size();
            case 2: return (int) inv.items.stream().filter(i -> i.type == Item.Type.ARMOR).count();
            default: return 0;
        }
    }

    private void equipSelected(InventoryComponent inv) {
        switch (invTab) {
            case 0: {
                List<Item> weapons = new ArrayList<>();
                for (Item it : inv.items) if (it.type == Item.Type.WEAPON) weapons.add(it);
                if (invCursor < weapons.size()) inv.equippedWeapon = weapons.get(invCursor);
                break;
            }
            case 1: {
                if (invCursor < inv.artifacts.size()) {
                    Artifact art = inv.artifacts.get(invCursor);
                    inv.equipArtifact(art);
                }
                break;
            }
            case 2: {
                List<Item> armors = new ArrayList<>();
                for (Item it : inv.items) if (it.type == Item.Type.ARMOR) armors.add(it);
                if (invCursor < armors.size()) inv.equippedArmor = armors.get(invCursor);
                break;
            }
        }
    }

    private void updateLogic(float delta) {
        if (attackFlash  > 0) attackFlash  -= delta;
        if (powerFlash   > 0) powerFlash   -= delta;
        if (damageOverlay > 0) damageOverlay -= delta;
        if (levelUpTimer  > 0) levelUpTimer  -= delta;

        // Détecter dégâts reçus par le joueur
        HealthComponent hp = player.getComponent(HealthComponent.class);
        if (hp != null) {
            if (hp.currentHealth < lastPlayerHp) damageOverlay = 0.35f;
            lastPlayerHp = hp.currentHealth;
        }

        // Détecter level up
        StatsComponent st = player.getComponent(StatsComponent.class);
        if (st != null && st.level > lastLevel) {
            lastLevel    = st.level;
            levelUpTimer = 2.5f;
        }

        // Drainer la file de damage numbers
        float[] ev;
        while ((ev = DamageQueue.events.poll()) != null) {
            dmgNums.add(new DmgNum(ev[0], ev[1], ev[2], ev[3] == 1f));
        }

        Iterator<DmgNum> it = dmgNums.iterator();
        while (it.hasNext()) {
            DmgNum dn = it.next();
            dn.wy  += dn.vy * delta;
            dn.life -= delta;
            if (dn.life <= 0) it.remove();
        }
    }

    // ── HUD — formes ─────────────────────────────────────────────────────

    private void drawHudShapes(ShapeRenderer sr, int sw, int sh) {
        HealthComponent    hp    = player.getComponent(HealthComponent.class);
        StatsComponent     stats = player.getComponent(StatsComponent.class);
        InventoryComponent inv   = player.getComponent(InventoryComponent.class);

        // ── Panneau stats (haut-gauche) ───────────────────────────────────
        int panX = 6, panY = sh - 72;
        sr.setColor(0.04f, 0.04f, 0.07f, 1f);
        sr.rect(panX - 2, panY - 2, 138, 68);
        setColor(sr, 0.55f, 0.48f, 0.18f);
        sr.rect(panX - 3, panY - 3, 140, 1);
        sr.rect(panX - 3, panY + 64, 140, 1);
        sr.rect(panX - 3, panY - 3, 1, 68);
        sr.rect(panX + 136, panY - 3, 1, 68);

        // Badge niveau
        setColor(sr, 0.50f, 0.42f, 0.12f);
        sr.rect(panX, panY + 50, 26, 12);
        setColor(sr, 0.68f, 0.58f, 0.18f);
        sr.rect(panX, panY + 61, 26, 1);

        // Barre HP
        if (hp != null) {
            float pct = hp.currentHealth / hp.maxHealth;
            drawBar(sr, panX, panY + 34, 128, 10, pct, hpColor(pct), 0.12f, 0.04f, 0.04f, 0.45f, 0.40f, 0.18f);
        }

        // Barre XP
        if (stats != null && stats.xpToNextLevel > 0) {
            float xpPct = (float) stats.xp / stats.xpToNextLevel;
            drawBar(sr, panX, panY + 20, 128, 6, xpPct,
                    new float[]{0.85f, 0.72f, 0.10f},
                    0.10f, 0.09f, 0.03f,
                    0.45f, 0.38f, 0.10f);
        }

        // ── Inventaire — barre du bas ─────────────────────────────────────
        int totalW = SLOT_COUNT * SLOT_SIZE + (SLOT_COUNT - 1) * SLOT_GAP;
        int slotX  = sw / 2 - totalW / 2;
        int slotY  = 5;

        sr.setColor(0.04f, 0.04f, 0.07f, 1f);
        sr.rect(slotX - 5, slotY - 4, totalW + 10, SLOT_SIZE + 12);
        setColor(sr, 0.45f, 0.38f, 0.14f);
        sr.rect(slotX - 5, slotY + SLOT_SIZE + 7, totalW + 10, 1);

        for (int i = 0; i < SLOT_COUNT; i++) {
            int sx = slotX + i * (SLOT_SIZE + SLOT_GAP);
            drawSlot(sr, sx, slotY, SLOT_SIZE, getSlotItem(inv, i), i == selectedSlot);
        }

        // ── Minimap (haut-droite) ─────────────────────────────────────────
        int mmX = sw - MM_W - 8;
        int mmY = sh - MM_H - 8;
        drawMinimap(sr, mmX, mmY);

        // ── Level-up notification ─────────────────────────────────────────
        if (levelUpTimer > 0) {
            sr.setColor(0.85f, 0.72f, 0.10f, 1f);
            sr.rect(sw / 2f - 60, sh / 2f + 20, 120, 22);
            setColor(sr, 0.55f, 0.48f, 0.12f);
            sr.rect(sw / 2f - 61, sh / 2f + 19, 122, 1);
            sr.rect(sw / 2f - 61, sh / 2f + 42, 122, 1);
        }

        // ── Panneau inventaire Genshin (touche E) ─────────────────────────
        if (GameState.inventoryOpen) drawInventoryPanel(sr, inv, sw, sh);
    }

    /** Dessine une barre (fond + remplissage + bordure). */
    private void drawBar(ShapeRenderer sr, int x, int y, int w, int h, float pct,
                         float[] fillCol,
                         float bgR, float bgG, float bgB,
                         float borR, float borG, float borB) {
        sr.setColor(bgR, bgG, bgB, 1f);
        sr.rect(x, y, w, h);
        sr.setColor(fillCol[0], fillCol[1], fillCol[2], 1f);
        sr.rect(x, y, (int)(w * Math.max(0f, pct)), h);
        sr.setColor(fillCol[0] * 1.3f, fillCol[1] * 1.3f, fillCol[2] * 1.3f, 1f);
        sr.rect(x, y + h - 2, (int)(w * Math.max(0f, pct)), 2);
        sr.setColor(borR, borG, borB, 1f);
        sr.rect(x - 1, y - 1, w + 2, 1);
        sr.rect(x - 1, y + h,  w + 2, 1);
        sr.rect(x - 1, y - 1, 1, h + 2);
        sr.rect(x + w,  y - 1, 1, h + 2);
    }

    private float[] hpColor(float pct) {
        if (pct > 0.55f) return new float[]{ 0.15f, 0.80f, 0.20f };
        if (pct > 0.28f) return new float[]{ 0.88f, 0.72f, 0.08f };
        return new float[]{ 0.88f, 0.12f, 0.08f };
    }

    private void drawSlot(ShapeRenderer sr, int sx, int sy, int size, Item item, boolean selected) {
        sr.setColor(0.08f, 0.08f, 0.12f, 1f);
        sr.rect(sx, sy, size, size);

        if (item != null) {
            float[] rc = rarityColor(item.rarity);
            sr.setColor(rc[0] * 0.22f, rc[1] * 0.22f, rc[2] * 0.22f, 1f);
            sr.rect(sx, sy, size, size);
            sr.setColor(rc[0], rc[1], rc[2], 1f);
            sr.rect(sx + 1, sy + 1, size - 2, 3);
            drawItemIcon(sr, sx, sy, size, item);
        }

        if (selected) {
            setColor(sr, 0.92f, 0.80f, 0.22f);
            sr.rect(sx - 2, sy - 2, size + 4, 2);
            sr.rect(sx - 2, sy + size, size + 4, 2);
            sr.rect(sx - 2, sy - 2, 2, size + 4);
            sr.rect(sx + size, sy - 2, 2, size + 4);
        } else {
            setColor(sr, 0.32f, 0.30f, 0.22f);
            sr.rect(sx - 1, sy - 1, size + 2, 1);
            sr.rect(sx - 1, sy + size, size + 2, 1);
            sr.rect(sx - 1, sy - 1, 1, size + 2);
            sr.rect(sx + size, sy - 1, 1, size + 2);
        }
    }

    private void drawItemIcon(ShapeRenderer sr, int sx, int sy, int size, Item item) {
        float cx = sx + size / 2f, cy = sy + size / 2f;
        switch (item.type) {
            case WEAPON:
                setColor(sr, 0.75f, 0.75f, 0.82f);
                sr.rect(cx - 1.5f, cy - 8f, 3f, 14f);
                setColor(sr, 0.65f, 0.52f, 0.18f);
                sr.rect(cx - 6f, cy, 12f, 2.5f);
                sr.rect(cx - 1.5f, cy - 11f, 3f, 3f);
                break;
            case ARMOR:
                setColor(sr, 0.50f, 0.50f, 0.60f);
                sr.rect(cx - 5f, cy - 5f, 10f, 10f);
                setColor(sr, 0.30f, 0.30f, 0.40f);
                sr.rect(cx - 4f, cy - 4f, 8f, 8f);
                setColor(sr, 0.65f, 0.60f, 0.18f);
                sr.rect(cx - 1f, cy - 5f, 2f, 10f);
                sr.rect(cx - 4f, cy - 1f, 8f, 2f);
                break;
            case RESOURCE:
                setColor(sr, 0.60f, 0.55f, 0.40f);
                sr.circle(cx, cy, 5f, 8);
                setColor(sr, 0.80f, 0.76f, 0.55f);
                sr.circle(cx - 1f, cy + 1f, 2f, 6);
                break;
            default:
                setColor(sr, 0.50f, 0.50f, 0.50f);
                sr.circle(cx, cy, 5f, 8);
        }
    }

    // ── Minimap ───────────────────────────────────────────────────────────

    private void drawMinimap(ShapeRenderer sr, int mmX, int mmY) {
        sr.setColor(0.17f, 0.30f, 0.09f, 1f);
        sr.rect(mmX, mmY, MM_W, MM_H);

        int pCol = (int)(playerTransform.x / WorldMap.TILE_SIZE);
        int pRow = (int)(playerTransform.y / WorldMap.TILE_SIZE);
        int hw   = MM_W / 2, hh = MM_H / 2;

        for (int dy = -hh; dy < hh; dy++) {
            for (int dx = -hw; dx < hw; dx++) {
                int tile = worldMap.getTile(pCol + dx, pRow + dy);
                int px   = mmX + dx + hw;
                int py   = mmY + dy + hh;
                if (tile == WorldMap.TILE_TREE) {
                    sr.setColor(0.08f, 0.16f, 0.04f, 1f);
                    sr.rect(px, py, 1, 1);
                } else if (tile == WorldMap.TILE_WATER) {
                    sr.setColor(0.22f, 0.45f, 0.85f, 1f);
                    sr.rect(px, py, 1, 1);
                } else if (tile == WorldMap.TILE_PATH) {
                    sr.setColor(0.60f, 0.50f, 0.30f, 1f);
                    sr.rect(px, py, 1, 1);
                } else if (tile == WorldMap.TILE_ROCK) {
                    sr.setColor(0.40f, 0.38f, 0.36f, 1f);
                    sr.rect(px, py, 1, 1);
                }
            }
        }

        for (Entity mob : mobs) {
            MobAIComponent ai = aimMap.get(mob);
            if (ai == null || ai.state == MobAIComponent.State.DEAD) continue;
            TransformComponent mp = tmMap.get(mob);
            int mdx = (int)(mp.x / WorldMap.TILE_SIZE) - pCol;
            int mdy = (int)(mp.y / WorldMap.TILE_SIZE) - pRow;
            if (Math.abs(mdx) < hw && Math.abs(mdy) < hh) {
                sr.setColor(0.90f, 0.12f, 0.12f, 1f);
                sr.rect(mmX + mdx + hw - 1, mmY + mdy + hh - 1, 3, 3);
            }
        }

        sr.setColor(0.30f, 0.30f, 0.30f, 1f);
        sr.rect(mmX + hw - 2, mmY + hh - 2, 5, 5);
        sr.setColor(1f, 1f, 1f, 1f);
        sr.rect(mmX + hw - 1, mmY + hh - 1, 3, 3);

        setColor(sr, 0.55f, 0.48f, 0.18f);
        sr.rect(mmX - 1, mmY - 1,     MM_W + 2, 1);
        sr.rect(mmX - 1, mmY + MM_H,  MM_W + 2, 1);
        sr.rect(mmX - 1, mmY,         1, MM_H);
        sr.rect(mmX + MM_W, mmY,      1, MM_H);
    }

    // ── Panneau inventaire Genshin — formes ───────────────────────────────

    private static final int INV_W = 300, INV_H = 200;
    private static final String[] TAB_LABELS = { "Armes", "Artéfacts", "Armure", "Personnage" };
    private static final int TAB_H = 18;
    private static final int LEFT_W = 100;

    private void drawInventoryPanel(ShapeRenderer sr, InventoryComponent inv, int sw, int sh) {
        int px = sw / 2 - INV_W / 2;
        int py = sh / 2 - INV_H / 2;

        // Fond principal
        sr.setColor(0.06f, 0.05f, 0.10f, 1f);
        sr.rect(px, py, INV_W, INV_H);

        // Bordure dorée
        setColor(sr, 0.55f, 0.48f, 0.18f);
        sr.rect(px - 1, py - 1,         INV_W + 2, 1);
        sr.rect(px - 1, py + INV_H,     INV_W + 2, 1);
        sr.rect(px - 1, py - 1,         1, INV_H + 2);
        sr.rect(px + INV_W, py - 1,     1, INV_H + 2);

        // Onglets
        int tabW = INV_W / 4;
        for (int i = 0; i < 4; i++) {
            int tx = px + i * tabW;
            int ty = py + INV_H - TAB_H;
            if (i == invTab) {
                setColor(sr, 0.50f, 0.42f, 0.12f);
                sr.rect(tx, ty, tabW, TAB_H);
                setColor(sr, 0.85f, 0.72f, 0.25f);
                sr.rect(tx, ty + TAB_H - 2, tabW, 2);
            } else {
                sr.setColor(0.12f, 0.10f, 0.18f, 1f);
                sr.rect(tx, ty, tabW, TAB_H);
            }
            // Séparateur vertical
            setColor(sr, 0.35f, 0.30f, 0.12f);
            sr.rect(tx + tabW - 1, ty, 1, TAB_H);
        }

        // Ligne sous les onglets
        setColor(sr, 0.55f, 0.48f, 0.18f);
        sr.rect(px, py + INV_H - TAB_H - 1, INV_W, 1);

        int contentY = py;
        int contentH = INV_H - TAB_H - 1;

        // Séparateur vertical gauche/droite
        setColor(sr, 0.25f, 0.22f, 0.10f);
        sr.rect(px + LEFT_W, contentY, 1, contentH);

        // ── Partie gauche : équipé ─────────────────────────────────────────
        if (inv != null) {
            switch (invTab) {
                case 0: drawEquippedWeaponShape(sr, px, contentY, inv); break;
                case 1: drawEquippedArtifactsShape(sr, px, contentY, inv); break;
                case 2: drawEquippedArmorShape(sr, px, contentY, inv); break;
                case 3: /* stats — texte only */ break;
            }
        }

        // ── Partie droite : liste items ────────────────────────────────────
        if (inv != null && invTab < 3) {
            drawItemListShape(sr, px + LEFT_W + 3, contentY, INV_W - LEFT_W - 4, contentH, inv);
        }
    }

    private void drawEquippedWeaponShape(ShapeRenderer sr, int px, int py, InventoryComponent inv) {
        int bx = px + 8, by = py + 100;
        drawBigSlot(sr, bx, by, 40, inv.equippedWeapon);
    }

    private void drawEquippedArmorShape(ShapeRenderer sr, int px, int py, InventoryComponent inv) {
        int bx = px + 8, by = py + 100;
        drawBigSlot(sr, bx, by, 40, inv.equippedArmor);
    }

    private void drawEquippedArtifactsShape(ShapeRenderer sr, int px, int py, InventoryComponent inv) {
        Artifact.Slot[] slots = Artifact.Slot.values();
        for (int i = 0; i < slots.length; i++) {
            int bx = px + 6 + (i % 2) * 44;
            int by = py + 120 - (i / 2) * 38;
            Artifact art = inv.getEquippedArtifact(slots[i]);
            drawArtifactSlot(sr, bx, by, 36, art);
        }
    }

    private void drawBigSlot(ShapeRenderer sr, int x, int y, int size, Item item) {
        sr.setColor(0.10f, 0.09f, 0.15f, 1f);
        sr.rect(x, y, size, size);
        if (item != null) {
            float[] rc = rarityColor(item.rarity);
            sr.setColor(rc[0] * 0.20f, rc[1] * 0.20f, rc[2] * 0.20f, 1f);
            sr.rect(x, y, size, size);
            sr.setColor(rc[0], rc[1], rc[2], 1f);
            sr.rect(x + 1, y + 1, size - 2, 3);
            drawItemIcon(sr, x, y, size, item);
        }
        setColor(sr, 0.55f, 0.48f, 0.18f);
        sr.rect(x - 1, y - 1, size + 2, 1);
        sr.rect(x - 1, y + size, size + 2, 1);
        sr.rect(x - 1, y - 1, 1, size + 2);
        sr.rect(x + size, y - 1, 1, size + 2);
    }

    private void drawArtifactSlot(ShapeRenderer sr, int x, int y, int size, Artifact art) {
        sr.setColor(0.10f, 0.09f, 0.15f, 1f);
        sr.rect(x, y, size, size);
        if (art != null) {
            float[] rc = rarityColor(art.rarity);
            sr.setColor(rc[0] * 0.20f, rc[1] * 0.20f, rc[2] * 0.20f, 1f);
            sr.rect(x, y, size, size);
            sr.setColor(rc[0], rc[1], rc[2], 1f);
            sr.rect(x + 1, y + 1, size - 2, 3);
            // Icône artéfact : losange coloré
            float cx = x + size / 2f, cy = y + size / 2f;
            sr.setColor(rc[0], rc[1], rc[2], 1f);
            sr.triangle(cx, cy + 8f, cx - 6f, cy, cx + 6f, cy);
            sr.triangle(cx - 6f, cy, cx + 6f, cy, cx, cy - 8f);
        }
        setColor(sr, 0.40f, 0.35f, 0.15f);
        sr.rect(x - 1, y - 1, size + 2, 1);
        sr.rect(x - 1, y + size, size + 2, 1);
        sr.rect(x - 1, y - 1, 1, size + 2);
        sr.rect(x + size, y - 1, 1, size + 2);
    }

    private void drawItemListShape(ShapeRenderer sr, int lx, int ly, int lw, int lh,
                                    InventoryComponent inv) {
        List<?> list = getTabList(inv);
        int rowH = 20;
        int maxVisible = lh / rowH;

        for (int i = 0; i < Math.min(list.size(), maxVisible); i++) {
            int ry = ly + lh - (i + 1) * rowH;
            boolean selected = (i == invCursor);

            // Fond ligne
            if (selected) {
                setColor(sr, 0.30f, 0.25f, 0.08f);
            } else {
                sr.setColor(i % 2 == 0 ? 0.10f : 0.08f, i % 2 == 0 ? 0.09f : 0.07f, 0.14f, 1f);
            }
            sr.rect(lx, ry, lw, rowH - 1);

            // Barre rareté à gauche
            Object obj = list.get(i);
            float[] rc = getObjectRarityColor(obj);
            sr.setColor(rc[0], rc[1], rc[2], 1f);
            sr.rect(lx, ry, 3, rowH - 1);

            // Curseur doré sur gauche si sélectionné
            if (selected) {
                setColor(sr, 0.92f, 0.80f, 0.22f);
                sr.rect(lx, ry, lw, 1);
                sr.rect(lx, ry + rowH - 1, lw, 1);
            }
        }
    }

    private List<?> getTabList(InventoryComponent inv) {
        List<Object> result = new ArrayList<>();
        if (inv == null) return result;
        switch (invTab) {
            case 0:
                for (Item it : inv.items) if (it.type == Item.Type.WEAPON) result.add(it);
                break;
            case 1:
                result.addAll(inv.artifacts);
                break;
            case 2:
                for (Item it : inv.items) if (it.type == Item.Type.ARMOR) result.add(it);
                break;
        }
        return result;
    }

    private float[] getObjectRarityColor(Object obj) {
        if (obj instanceof Item)     return rarityColor(((Item) obj).rarity);
        if (obj instanceof Artifact) return rarityColor(((Artifact) obj).rarity);
        return new float[]{ 0.65f, 0.65f, 0.65f };
    }

    // ── HUD — texte ───────────────────────────────────────────────────────

    private void drawHudText(SpriteBatch batch, int sw, int sh) {
        HealthComponent    hp    = player.getComponent(HealthComponent.class);
        StatsComponent     stats = player.getComponent(StatsComponent.class);
        InventoryComponent inv   = player.getComponent(InventoryComponent.class);

        if (stats != null) {
            font.setColor(0.95f, 0.88f, 0.40f, 1f);
            font.draw(batch, "Lv", 8, sh - 10);
            fontBig.setColor(1f, 0.95f, 0.50f, 1f);
            fontBig.draw(batch, String.valueOf(stats.level), 18, sh - 8);
        }

        if (hp != null) {
            font.setColor(0.90f, 0.90f, 0.90f, 1f);
            font.draw(batch, (int) hp.currentHealth + "/" + (int) hp.maxHealth, 8 + 130, sh - 30);
        }

        if (stats != null) {
            font.setColor(0.80f, 0.68f, 0.20f, 1f);
            font.draw(batch, "XP " + stats.xp + "/" + stats.xpToNextLevel, 8 + 130, sh - 44);
        }

        if (stats != null) {
            float atk = stats.atk + (inv != null ? inv.getWeaponAtkBonus() + inv.getArtifactAtk() : 0f);
            float def = stats.def + (inv != null ? inv.getArmorDefBonus()  + inv.getArtifactDef()  : 0f);
            font.setColor(0.80f, 0.40f, 0.40f, 1f);
            font.draw(batch, "ATK " + (int) atk, 8, sh - 56);
            font.setColor(0.40f, 0.65f, 0.80f, 1f);
            font.draw(batch, "DEF " + (int) def, 52, sh - 56);
        }

        // Labels slots barre du bas
        int totalW = SLOT_COUNT * SLOT_SIZE + (SLOT_COUNT - 1) * SLOT_GAP;
        int slotX  = sw / 2 - totalW / 2;
        String[] labels = { "W", "A", "1", "2", "3", "4" };
        for (int i = 0; i < SLOT_COUNT; i++) {
            int sx = slotX + i * (SLOT_SIZE + SLOT_GAP);
            Item item = getSlotItem(inv, i);
            font.setColor(0.55f, 0.50f, 0.35f, 1f);
            font.draw(batch, labels[i], sx + 2, 5 + SLOT_SIZE + 3);
            if (item != null) {
                float[] rc = rarityColor(item.rarity);
                font.setColor(rc[0], rc[1], rc[2], 1f);
                font.draw(batch,
                    item.name.length() > 4 ? item.name.substring(0, 4) : item.name,
                    sx + 2, 5 + 14);
            }
        }

        // Touches bas
        font.setColor(0.45f, 0.42f, 0.30f, 1f);
        font.draw(batch, "[ZQSD] Dépl  [Espace/F] Attaque  [R] Puissant  [E] Inventaire  [Échap] Menu",
                  6, 14);

        // Minimap label
        font.setColor(0.55f, 0.50f, 0.30f, 1f);
        font.draw(batch, "CARTE", sw - MM_W - 8, sh - 2);

        // Level-up notification
        if (levelUpTimer > 0) {
            fontBig.setColor(0.08f, 0.05f, 0.02f, 1f);
            fontBig.draw(batch, "LEVEL UP !  Lv " + lastLevel, sw / 2f - 52, sh / 2f + 36);
        }

        if (GameState.inventoryOpen) drawInventoryText(batch, inv, sw, sh);
    }

    // ── Panneau inventaire Genshin — texte ────────────────────────────────

    private void drawInventoryText(SpriteBatch batch, InventoryComponent inv, int sw, int sh) {
        int px = sw / 2 - INV_W / 2;
        int py = sh / 2 - INV_H / 2;

        // Titre
        fontBig.setColor(0.95f, 0.85f, 0.30f, 1f);
        fontBig.draw(batch, "INVENTAIRE", px + INV_W / 2f - 36, py + INV_H - 2);

        // Onglets
        int tabW = INV_W / 4;
        for (int i = 0; i < 4; i++) {
            int tx = px + i * tabW + 4;
            int ty = py + INV_H - TAB_H + 5;
            if (i == invTab) {
                font.setColor(1f, 0.92f, 0.40f, 1f);
            } else {
                font.setColor(0.55f, 0.50f, 0.38f, 1f);
            }
            font.draw(batch, TAB_LABELS[i], tx, ty);
        }

        int contentY = py;
        int contentH = INV_H - TAB_H - 1;

        if (inv == null) return;

        // ── Partie gauche ──────────────────────────────────────────────────
        switch (invTab) {
            case 0: drawEquippedWeaponText(batch, px, contentY, inv); break;
            case 1: drawEquippedArtifactsText(batch, px, contentY, inv); break;
            case 2: drawEquippedArmorText(batch, px, contentY, inv); break;
            case 3: drawStatsText(batch, px, contentY, inv); break;
        }

        // ── Partie droite : liste items ────────────────────────────────────
        if (invTab < 3) {
            drawItemListText(batch, px + LEFT_W + 3, contentY, INV_W - LEFT_W - 4, contentH, inv);
        }

        // Aide en bas
        font.setColor(0.40f, 0.38f, 0.28f, 1f);
        font.draw(batch, "[←→] Onglet  [↑↓] Sélect  [Entrée] Équiper  [E] Fermer",
                  px + 4, py + 11);
    }

    private void drawEquippedWeaponText(SpriteBatch batch, int px, int py, InventoryComponent inv) {
        font.setColor(0.70f, 0.65f, 0.40f, 1f);
        font.draw(batch, "ÉQUIPÉ", px + 8, py + 152);

        if (inv.equippedWeapon != null) {
            Item w = inv.equippedWeapon;
            float[] rc = rarityColor(w.rarity);
            font.setColor(rc[0], rc[1], rc[2], 1f);
            font.draw(batch, w.name.length() > 12 ? w.name.substring(0, 12) : w.name, px + 8, py + 138);
            font.setColor(0.85f, 0.38f, 0.38f, 1f);
            font.draw(batch, "ATK +" + (int) w.atkBonus, px + 8, py + 126);
        } else {
            font.setColor(0.40f, 0.38f, 0.30f, 1f);
            font.draw(batch, "(vide)", px + 8, py + 138);
        }
    }

    private void drawEquippedArmorText(SpriteBatch batch, int px, int py, InventoryComponent inv) {
        font.setColor(0.70f, 0.65f, 0.40f, 1f);
        font.draw(batch, "ÉQUIPÉ", px + 8, py + 152);

        if (inv.equippedArmor != null) {
            Item a = inv.equippedArmor;
            float[] rc = rarityColor(a.rarity);
            font.setColor(rc[0], rc[1], rc[2], 1f);
            font.draw(batch, a.name.length() > 12 ? a.name.substring(0, 12) : a.name, px + 8, py + 138);
            font.setColor(0.38f, 0.60f, 0.85f, 1f);
            font.draw(batch, "DEF +" + (int) a.defBonus, px + 8, py + 126);
        } else {
            font.setColor(0.40f, 0.38f, 0.30f, 1f);
            font.draw(batch, "(vide)", px + 8, py + 138);
        }
    }

    private void drawEquippedArtifactsText(SpriteBatch batch, int px, int py, InventoryComponent inv) {
        font.setColor(0.70f, 0.65f, 0.40f, 1f);
        font.draw(batch, "ARTÉFACTS", px + 6, py + 165);

        Artifact.Slot[] slots = Artifact.Slot.values();
        for (int i = 0; i < slots.length; i++) {
            int bx = px + 6 + (i % 2) * 44;
            int by = py + 120 - (i / 2) * 38;
            Artifact art = inv.getEquippedArtifact(slots[i]);
            // Slot label
            font.setColor(0.55f, 0.50f, 0.38f, 1f);
            font.draw(batch, slots[i].label, bx, by + 40);
            if (art != null) {
                float[] rc = rarityColor(art.rarity);
                font.setColor(rc[0], rc[1], rc[2], 1f);
                String abbr = art.name.length() > 7 ? art.name.substring(0, 7) : art.name;
                font.draw(batch, abbr, bx, by + 30);
            }
        }
    }

    private void drawStatsText(SpriteBatch batch, int px, int py, InventoryComponent inv) {
        StatsComponent stats = player.getComponent(StatsComponent.class);
        HealthComponent hp   = player.getComponent(HealthComponent.class);
        if (stats == null) return;

        fontBig.setColor(0.88f, 0.78f, 0.28f, 1f);
        fontBig.draw(batch, "PERSONNAGE", px + 10, py + 170);

        float atk  = stats.atk  + inv.getWeaponAtkBonus()  + inv.getArtifactAtk();
        float def  = stats.def  + inv.getArmorDefBonus()   + inv.getArtifactDef();
        float cr   = stats.critRate + inv.getArtifactCritRate();
        float cd   = stats.critDmg  + inv.getArtifactCritDmg();

        int lx = px + 10, y = py + 152;
        int gap = 14;
        font.setColor(0.90f, 0.90f, 0.90f, 1f);
        font.draw(batch, "Niveau : " + stats.level, lx, y);
        if (hp != null) { font.draw(batch, "PV : " + (int)hp.currentHealth + "/" + (int)hp.maxHealth, lx, y -= gap); }
        font.setColor(0.85f, 0.38f, 0.38f, 1f);
        font.draw(batch, "ATK     : " + (int) atk, lx, y -= gap);
        font.setColor(0.38f, 0.60f, 0.85f, 1f);
        font.draw(batch, "DEF     : " + (int) def, lx, y -= gap);
        font.setColor(0.90f, 0.85f, 0.20f, 1f);
        font.draw(batch, "Taux c. : " + (int)(cr * 100) + "%", lx, y -= gap);
        font.draw(batch, "Dég. c. : " + (int)(cd * 100) + "%", lx, y -= gap);
        font.setColor(0.60f, 0.40f, 0.85f, 1f);
        font.draw(batch, "Artéfacts équipés : " + inv.equippedArtifactCount() + "/5", lx, y -= gap);
        font.setColor(0.70f, 0.65f, 0.40f, 1f);
        font.draw(batch, "XP : " + stats.xp + "/" + stats.xpToNextLevel, lx, y -= gap);
    }

    private void drawItemListText(SpriteBatch batch, int lx, int ly, int lw, int lh,
                                   InventoryComponent inv) {
        List<?> list = getTabList(inv);
        int rowH = 20;
        int maxVisible = lh / rowH;

        if (list.isEmpty()) {
            font.setColor(0.45f, 0.42f, 0.30f, 1f);
            font.draw(batch, "(vide)", lx + 8, ly + lh - 10);
            return;
        }

        for (int i = 0; i < Math.min(list.size(), maxVisible); i++) {
            int ry = ly + lh - (i + 1) * rowH;
            Object obj = list.get(i);
            float[] rc = getObjectRarityColor(obj);
            boolean sel = (i == invCursor);

            font.setColor(rc[0], rc[1], rc[2], 1f);
            String name = getObjectName(obj);
            font.draw(batch, (sel ? "> " : "  ") + name, lx + 6, ry + rowH - 5);

            String stat = getObjectStatLine(obj);
            if (!stat.isEmpty()) {
                font.setColor(0.65f, 0.65f, 0.65f, 1f);
                font.draw(batch, stat, lx + 6, ry + 5);
            }
        }
    }

    private String getObjectName(Object obj) {
        if (obj instanceof Item) {
            String n = ((Item) obj).name;
            return n.length() > 14 ? n.substring(0, 14) : n;
        }
        if (obj instanceof Artifact) {
            String n = ((Artifact) obj).name;
            return n.length() > 14 ? n.substring(0, 14) : n;
        }
        return "?";
    }

    private String getObjectStatLine(Object obj) {
        if (obj instanceof Item) {
            Item it = (Item) obj;
            if (it.atkBonus > 0) return "ATK+" + (int) it.atkBonus;
            if (it.defBonus > 0) return "DEF+" + (int) it.defBonus;
        }
        if (obj instanceof Artifact) {
            Artifact art = (Artifact) obj;
            StringBuilder sb = new StringBuilder(art.mainStat.label + "+" + (int) art.mainValue);
            if (art.critRateBonus > 0) sb.append(" CR+").append((int)(art.critRateBonus * 100)).append("%");
            if (art.critDmgBonus  > 0) sb.append(" CD+").append((int)(art.critDmgBonus  * 100)).append("%");
            return sb.toString();
        }
        return "";
    }

    // ── Utilitaires ───────────────────────────────────────────────────────

    private Item getSlotItem(InventoryComponent inv, int i) {
        if (inv == null) return null;
        if (i == 0) return inv.equippedWeapon;
        if (i == 1) return inv.equippedArmor;
        int bi = i - 2;
        return bi < inv.items.size() ? inv.items.get(bi) : null;
    }

    private float[] rarityColor(Item.Rarity r) {
        switch (r) {
            case UNCOMMON:  return new float[]{ 0.22f, 0.82f, 0.22f };
            case RARE:      return new float[]{ 0.22f, 0.58f, 1.00f };
            case EPIC:      return new float[]{ 0.68f, 0.22f, 0.92f };
            case LEGENDARY: return new float[]{ 1.00f, 0.62f, 0.10f };
            default:        return new float[]{ 0.65f, 0.65f, 0.65f };
        }
    }

    private void setColor(ShapeRenderer sr, float r, float g, float b) {
        sr.setColor(r, g, b, 1f);
    }

    // ── Cycle de vie ──────────────────────────────────────────────────────

    @Override
    public void resize(int w, int h) {
        viewport.update(w, h, true);
        hudMatrix.setToOrtho2D(0, 0, w, h);
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        batch.dispose();
        sr.dispose();
        font.dispose();
        fontBig.dispose();
        worldRenderer.dispose();
    }
}
