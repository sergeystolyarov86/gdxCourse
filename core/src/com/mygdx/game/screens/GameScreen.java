package com.mygdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
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
import com.mygdx.game.GameSounds;
import com.mygdx.game.Main;
import com.mygdx.game.PhysX;

import java.util.ArrayList;
import java.util.List;


public class GameScreen implements Screen {

    //  private static final float STEP = 1f;
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
    public static List<Body> bodies;
    private final GameSounds gameSounds;

    public GameScreen(Main game) {
        bodies = new ArrayList<>();
        this.game = game;
        shapeRenderer = new ShapeRenderer();
        img = new Texture("game.png");
        startRect = new Rectangle(0, 0, img.getWidth(), img.getHeight());
        batch = new SpriteBatch();
        animation = new Anim("atlas/bob.atlas", "bobRun", Animation.PlayMode.LOOP);

        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.zoom = 0.99f;
        map = new TmxMapLoader().load("map/map.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        bg = new int[1];
        bg[0] = map.getLayers().getIndex("???????? ???????????? 1");
        l1 = new int[2]; //???????????? ?????????? ???????????????????? ???????? ?????? ??????????????????????????

        physX = new PhysX();
        map.getLayers().get("??????????????").getObjects().getByType(RectangleMapObject.class);
        RectangleMapObject tmp = (RectangleMapObject) map.getLayers().get("??????????????").getObjects().get("hero");
        heroRect = tmp.getRectangle();
        body = physX.addObject(tmp);

        Array<RectangleMapObject> objects = map.getLayers().get("??????????????").getObjects().getByType(RectangleMapObject.class);
        for (int i = 0; i < objects.size; i++) {
            physX.addObject(objects.get(i));
        }
        gameSounds = new GameSounds();
        gameSounds.playGameMusic();

    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {

        if (Gdx.input.isKeyPressed(Input.Keys.P)) camera.zoom -= 0.1f;
        if (Gdx.input.isKeyPressed(Input.Keys.M)) camera.zoom += 0.1f;


        ScreenUtils.clear(0, 0, 1, 0);
        camera.position.x = body.getPosition().x * physX.PPM;
        camera.position.y = body.getPosition().y * physX.PPM;
        camera.update();


        animation.setTime(Gdx.graphics.getDeltaTime());
      //   batch.setProjectionMatrix(camera.combined);
        heroRect.x = body.getPosition().x - heroRect.width / 2;
        heroRect.y = body.getPosition().y - heroRect.height / 2;

        float x = Gdx.graphics.getWidth() / 2 - heroRect.getWidth() / 2 / camera.zoom;
        float y = Gdx.graphics.getHeight() / 2 - heroRect.getHeight() / 2 / camera.zoom;

        batch.begin();
        moveBob();
        batch.draw(img, 0, 0); // -45 x
        batch.draw(animation.getFrame(), x, y - 135);     //y 105
        batch.end();
        mapRenderer.setView(camera);
        mapRenderer.render(bg);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            dispose();
            game.setScreen(new MenuScreen(game));
        }

        mapRenderer.render(l1); //?????????????????? ?????????? ?? ?????????????? l1

      physX.step();
      physX.debugDraw(camera);

        for (Body value : bodies) {
            physX.deleteBody(value);
        }
        bodies.clear();

    }

    public void moveBob() {


        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            if (!animation.getFrame().isFlipX()) {
                animation.getFrame().flip(true, false);
            }
            body.applyForceToCenter(new Vector2(-0.3f, 0), true);
        }


        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            if (animation.getFrame().isFlipX()) {
                animation.getFrame().flip(true, false);
            }
            body.applyForceToCenter(new Vector2(0.3f, 0), true);

        }
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE) && physX.myContList.isOnGround()) {
            gameSounds.jumpSound();
            body.applyForceToCenter(new Vector2(0, 15f), true);
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
        gameSounds.disposeGameMusic();
    }
}