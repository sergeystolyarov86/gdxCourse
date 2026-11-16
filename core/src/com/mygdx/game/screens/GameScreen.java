package com.mygdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.Main;
import com.mygdx.game.audio.GameAudio;
import com.mygdx.game.entities.Hero;
import com.mygdx.game.level.Level;

/** Игровой экран: ввод, камера и отрисовка. Игровая логика — в {@link Level}. */
public final class GameScreen extends ScreenAdapter implements Level.Listener {

    private static final String MAP = "map/map.tmx";
    private static final String BACKGROUND = "game.png";

    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2f;
    private static final float ZOOM_SPEED = 1f; // в секунду
    /** Сколько секунд показывать «GAME OVER» перед возвратом в меню. */
    private static final float GAME_OVER_DELAY = 2.5f;
    private static final float HUD_MARGIN = 10f;
    /** Запас области отрисовки карты: размер самого крупного тайла. */
    private static final float MAP_OVERSCAN = 96f;
    /** На каком расстоянии от края экрана Боб начинает «толкать» камеру, пиксели. */
    private static final float CAMERA_EDGE_MARGIN = 220f;

    private final Main game;
    private final SpriteBatch batch = new SpriteBatch();
    private final Texture background = new Texture(BACKGROUND);
    private final Texture bulletTexture = createBulletTexture();
    private final OrthographicCamera worldCamera = new OrthographicCamera();
    private final OrthographicCamera screenCamera = new OrthographicCamera();
    private final Viewport worldViewport = new FitViewport(Main.VIRTUAL_WIDTH, Main.VIRTUAL_HEIGHT, worldCamera);
    private final Viewport screenViewport = new FitViewport(Main.VIRTUAL_WIDTH, Main.VIRTUAL_HEIGHT, screenCamera);
    private final Vector2 heroPosition = new Vector2();
    private final Vector2 pointer = new Vector2();
    private final Level level;
    private final OrthogonalTiledMapRenderer mapRenderer;
    private final float levelWidth;
    private final BitmapFont font = new BitmapFont();
    private boolean debugPhysics;
    /** Отсчёт до возврата в меню после смерти; отрицательный — игра идёт. */
    private float gameOverTimer = -1f;
    private String endMessage = "";

    public GameScreen(Main game) {
        this.game = game;
        level = new Level(MAP, this);
        mapRenderer = new OrthogonalTiledMapRenderer(level.getMap(), batch);
        MapProperties mapProperties = level.getMap().getProperties();
        levelWidth = mapProperties.get("width", Integer.class) * mapProperties.get("tilewidth", Integer.class);
        // Стартовая позиция камеры: левый край уровня.
        worldCamera.position.set(Main.VIRTUAL_WIDTH / 2f, Main.VIRTUAL_HEIGHT / 2f, 0f);
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void show() {
        game.getAudio().playMusic(GameAudio.Track.GAME);
    }

    @Override
    public void render(float delta) {
        boolean gameOver = gameOverTimer >= 0f;
        if (gameOver) gameOverTimer -= delta;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || (gameOver && gameOverTimer < 0f)) {
            game.showMenu();
            return; // этот экран уже освобождён
        }

        if (gameOver) {
            level.getHero().move(0);
        } else {
            handleInput(delta);
        }
        level.update(delta);
        followHero();
        draw();
    }

    private void handleInput(float delta) {
        Hero hero = level.getHero();

        int direction = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) direction--;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) direction++;
        hero.move(direction);

        if ((Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.UP))
                && hero.jump()) {
            game.getAudio().play(GameAudio.Effect.JUMP);
        }

        // Прицел: мышью — в курсор; W — вертикально вверх (по дронам); иначе — вперёд.
        boolean mouseFire = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        if (mouseFire) {
            worldViewport.unproject(pointer.set(Gdx.input.getX(), Gdx.input.getY()));
            hero.aimAt(pointer.x, pointer.y);
        } else if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            hero.aimUp();
        } else {
            hero.aimForward();
        }

        // Автоматический огонь, пока зажата клавиша; темп задаёт сам автомат.
        boolean triggerHeld = Gdx.input.isKeyPressed(Input.Keys.F)
                || Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
                || Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        if (triggerHeld && level.fire()) {
            game.getAudio().play(GameAudio.Effect.SHOT);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.P)) worldCamera.zoom -= ZOOM_SPEED * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.M)) worldCamera.zoom += ZOOM_SPEED * delta;
        worldCamera.zoom = MathUtils.clamp(worldCamera.zoom, MIN_ZOOM, MAX_ZOOM);

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) debugPhysics = !debugPhysics;
    }

    /**
     * Камера стоит на месте, пока Боб бегает в средней части экрана, и сдвигается,
     * только когда он подходит к краю. По вертикали камера не двигается вовсе,
     * поэтому при прыжках фон и земля остаются неподвижными.
     */
    private void followHero() {
        level.getHero().getPositionPx(heroPosition);
        float halfWidth = worldCamera.viewportWidth * worldCamera.zoom / 2f;
        float deadZone = Math.max(0f, halfWidth - CAMERA_EDGE_MARGIN);

        float x = worldCamera.position.x;
        if (heroPosition.x > x + deadZone) x = heroPosition.x - deadZone;
        if (heroPosition.x < x - deadZone) x = heroPosition.x + deadZone;
        // Не показываем пустоту за краями уровня.
        x = levelWidth > 2f * halfWidth ? MathUtils.clamp(x, halfWidth, levelWidth - halfWidth) : levelWidth / 2f;

        worldCamera.position.set(x, Main.VIRTUAL_HEIGHT / 2f, 0f);
        worldCamera.update();
    }

    private void draw() {
        ScreenUtils.clear(0f, 0f, 0f, 1f); // цвет полос по краям, если пропорции окна другие

        screenViewport.apply();
        batch.setProjectionMatrix(screenCamera.combined);
        batch.begin();
        batch.draw(background, 0, 0, Main.VIRTUAL_WIDTH, Main.VIRTUAL_HEIGHT);
        batch.end();

        worldViewport.apply();
        // Тайлы декораций крупнее клетки (до 96px) и рисуются вправо-вверх от неё,
        // поэтому расширяем область отсечения влево и вниз, иначе они пропадают у края экрана.
        float viewWidth = worldCamera.viewportWidth * worldCamera.zoom;
        float viewHeight = worldCamera.viewportHeight * worldCamera.zoom;
        mapRenderer.setView(worldCamera.combined,
                worldCamera.position.x - viewWidth / 2f - MAP_OVERSCAN,
                worldCamera.position.y - viewHeight / 2f - MAP_OVERSCAN,
                viewWidth + MAP_OVERSCAN, viewHeight + MAP_OVERSCAN);
        mapRenderer.render();

        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
        level.draw(batch, bulletTexture);
        batch.end();

        drawHud();

        if (debugPhysics) {
            level.getPhysics().debugRender(worldCamera.combined);
        }
    }

    @Override
    public void onJellyfishCollected(int collected, int total) {
        game.getAudio().play(GameAudio.Effect.CATCH);
        Gdx.app.log("Level", "Jellyfish " + collected + "/" + total);
    }

    /** Встроенный шрифт libGDX не содержит кириллицы, поэтому надписи латиницей. */
    private void drawHud() {
        screenViewport.apply();
        batch.setProjectionMatrix(screenCamera.combined);
        batch.begin();
        float top = Main.VIRTUAL_HEIGHT - HUD_MARGIN;
        font.getData().setScale(1.5f);
        font.draw(batch, "HP: " + level.getHero().getHealth() + "/" + Hero.MAX_HEALTH, HUD_MARGIN, top);
        font.draw(batch, "Kills: " + level.getKills(), HUD_MARGIN, top - 25f);
        if (gameOverTimer >= 0f) {
            font.getData().setScale(3f);
            font.draw(batch, endMessage, 0f, Main.VIRTUAL_HEIGHT / 2f + 20f,
                    Main.VIRTUAL_WIDTH, Align.center, false);
        }
        batch.end();
    }

    @Override
    public void onJellyfishShot(int shot, int total) {
        Gdx.app.log("Level", "Jellyfish shot " + shot + "/" + total);
    }

    @Override
    public void onEnemyKilled(int kills) {
        game.getAudio().play(GameAudio.Effect.ENEMY_DOWN);
    }

    @Override
    public void onHeroHurt(int health) {
        game.getAudio().play(GameAudio.Effect.HURT);
    }

    @Override
    public void onHeroDied() {
        endGame("GAME OVER");
    }

    @Override
    public void onLevelComplete() {
        game.getAudio().play(GameAudio.Effect.VICTORY);
        endGame("VICTORY! Kills: " + level.getKills());
    }

    @Override
    public void onEnemyFired() {
        game.getAudio().play(GameAudio.Effect.ENEMY_SHOT);
    }

    @Override
    public void onExplosion() {
        game.getAudio().play(GameAudio.Effect.EXPLOSION);
    }

    private void endGame(String message) {
        if (gameOverTimer >= 0f) return;
        endMessage = message;
        gameOverTimer = GAME_OVER_DELAY;
    }

    /** Пуля — просто жёлто-оранжевый прямоугольник, растягивается при отрисовке. */
    private static Texture createBulletTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 0.8f, 0.2f, 1f);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void resize(int width, int height) {
        worldViewport.update(width, height);
        screenViewport.update(width, height, true);
    }

    @Override
    public void hide() {
        game.getAudio().stopMusic();
    }

    @Override
    public void dispose() {
        mapRenderer.dispose(); // batch общий — рендерер его не освобождает
        level.dispose();
        background.dispose();
        bulletTexture.dispose();
        font.dispose();
        batch.dispose();
    }
}
