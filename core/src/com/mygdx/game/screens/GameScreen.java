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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.mygdx.game.Anim;
import com.mygdx.game.Main;
import com.mygdx.game.PhysX;


public class GameScreen implements Screen {

    private static final float STEP = 1f;
    Anim animation;
    private boolean lookRight = true;
    private final Main game;
    private final SpriteBatch batch;
    private final Texture img;
    private final ShapeRenderer shapeRenderer;
    private final Rectangle startRect;
    private OrthographicCamera camera;
    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private final int[] bg;
    private final int[] l1;
    private PhysX physX;
    private Body body;
    private final Rectangle heroRect;

    public GameScreen(Main game) {
        this.game = game;
        shapeRenderer = new ShapeRenderer();
        img = new Texture("gameScene.png");
        startRect = new Rectangle(0, 0, img.getWidth(), img.getHeight());
        batch = new SpriteBatch();
        animation = new Anim("atlas/bob.atlas", "bobRun", Animation.PlayMode.LOOP);
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.zoom = 0.99f;
        map = new TmxMapLoader().load("map/map.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        bg = new int[1];
        bg[0] = map.getLayers().getIndex("Слой тайлов 1");
        l1 = new int[2]; //дальше можно закидывать слои при необходимости

        physX = new PhysX();
        map.getLayers().get("объекты").getObjects().getByType(RectangleMapObject.class);
        RectangleMapObject tmp = (RectangleMapObject) map.getLayers().get("сеттинг").getObjects().get("hero");
        heroRect = tmp.getRectangle();
        body = physX.addObject(tmp);

        Array<RectangleMapObject> objects = map.getLayers().get("объекты").getObjects().getByType(RectangleMapObject.class);
        for (int i = 0; i < objects.size; i++) {
            physX.addObject(objects.get(i));
        }
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {

        if (Gdx.input.isKeyPressed(Input.Keys.P)) camera.zoom -= 0.1f;
        if (Gdx.input.isKeyPressed(Input.Keys.M)) camera.zoom += 0.1f;



        ScreenUtils.clear(0, 0, 1, 0);
        camera.position.x = body.getPosition().x;
        camera.position.y = body.getPosition().y+130;
        camera.update();
        //  float x = Gdx.input.getX() - animation.getFrame().getRegionWidth();
        //   float y = Gdx.graphics.getHeight() - Gdx.input.getY() - animation.getFrame().getRegionHeight();

        animation.setTime(Gdx.graphics.getDeltaTime());
        batch.setProjectionMatrix(camera.combined);
        heroRect.x = body.getPosition().x - heroRect.width / 2 - 35;
        heroRect.y = body.getPosition().y - heroRect.height / 2;

        batch.begin();
        moveBob();
        batch.draw(img, -450, 0);
        batch.draw(animation.getFrame(), heroRect.x, heroRect.y);     //y 105
        batch.end();
        mapRenderer.setView(camera);
        mapRenderer.render(bg);
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            dispose();
            game.setScreen(new MenuScreen(game));
        }


        mapRenderer.render(l1); //отрисовка слоев в массиве l1

        physX.step();
        physX.debugDraw(camera);

    }

    public void moveBob() {

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            if (!animation.getFrame().isFlipX()) {
                animation.getFrame().flip(true, false);
            }
            body.applyForceToCenter(new Vector2(-100000,0),true);
            }


        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            if (animation.getFrame().isFlipX()) {
                animation.getFrame().flip(true, false);
            }
            body.applyForceToCenter(new Vector2(100000,0),true);

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