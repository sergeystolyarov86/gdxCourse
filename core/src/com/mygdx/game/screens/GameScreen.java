package com.mygdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import com.mygdx.game.Anim;
import com.mygdx.game.Main;


public class GameScreen implements Screen {

    private static final float STEP = 1f;
    int moveX;
    Anim animation;
    private boolean lookRight=true;
    private final Main game;
    private final SpriteBatch batch;
    private final Texture img;
    private final ShapeRenderer shapeRenderer;
    private final Rectangle startRect;
    private OrthographicCamera camera;
    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private Rectangle mapSize;

    public GameScreen(Main game) {
        this.game = game;
        shapeRenderer = new ShapeRenderer();
        img = new Texture("gameScene.png");
        startRect = new Rectangle(0, 0, img.getWidth(), img.getHeight());
        batch = new SpriteBatch();
        animation = new Anim("atlas/bob.atlas", "bobRun", Animation.PlayMode.LOOP);
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        map = new TmxMapLoader().load("map/map.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);


        map.getLayers().get("объекты").getObjects().getByType(RectangleMapObject.class);
        RectangleMapObject tmp = (RectangleMapObject) map.getLayers().get("объекты").getObjects().get("cam");
        camera.position.x = tmp.getRectangle().x;
        camera.position.y = tmp.getRectangle().y;
        tmp = (RectangleMapObject) map.getLayers().get("объекты").getObjects().get("border");
        mapSize = tmp.getRectangle();

    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        // if (Gdx.input.isKeyPressed(Input.Keys.LEFT) && mapSize.x < camera.position.x - 1) camera.position.x -= STEP;
        //   if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) && mapSize.x + mapSize.width > camera.position.x + 1)
        //     camera.position.x += STEP;

        if (Gdx.input.isKeyPressed(Input.Keys.P)) camera.zoom -= 0.1f;
        if (Gdx.input.isKeyPressed(Input.Keys.M)) camera.zoom += 0.1f;

        ScreenUtils.clear(0, 0, 1, 0);
        camera.update();
        //  float x = Gdx.input.getX() - animation.getFrame().getRegionWidth();
        //   float y = Gdx.graphics.getHeight() - Gdx.input.getY() - animation.getFrame().getRegionHeight();

        animation.setTime(Gdx.graphics.getDeltaTime());
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        moveBob();
        batch.draw(img, 0, 0);
        batch.draw(animation.getFrame(), moveX, 105);
        batch.end();
        mapRenderer.setView(camera);
        mapRenderer.render();
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            dispose();
            game.setScreen(new MenuScreen(game));
        }
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.rect(mapSize.x, mapSize.y, mapSize.width, mapSize.height);
        shapeRenderer.end();
    }

    public void moveBob() {
        int finish = img.getWidth() - animation.getFrame().getRegionWidth();

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) ) {
            if (!animation.getFrame().isFlipX() ) {
                animation.getFrame().flip(true, false);

            }
            if (mapSize.x < camera.position.x - 1) {
                camera.position.x -= STEP;
            }
            if (moveX != 0) {
                moveX--;
            }
        }

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            if (animation.getFrame().isFlipX() ) {
                animation.getFrame().flip(true, false);

            }
            if (mapSize.x + mapSize.width > camera.position.x + 1) {
                camera.position.x += STEP;
            }
            if (moveX <= finish) {
                moveX++;
            }
        }

    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        this.batch.dispose();
        this.img.dispose();
        this.shapeRenderer.dispose();
        animation.dispose();
    }
}