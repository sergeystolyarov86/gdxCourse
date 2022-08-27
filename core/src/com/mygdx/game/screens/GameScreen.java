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
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import com.mygdx.game.Anim;
import com.mygdx.game.Main;


public class GameScreen implements Screen {

    int moveX;
    Anim animation;
    boolean isInFrame = true;
    boolean lookRight = true;
    private final Main game;
    private final SpriteBatch batch;
    private final Texture img;
   private final ShapeRenderer shapeRenderer;
   private final Rectangle startRect;
   private OrthographicCamera camera;
   private TiledMap map;
   private OrthogonalTiledMapRenderer mapRenderer;
    public GameScreen(Main game) {
        this.game = game;
        shapeRenderer =new ShapeRenderer();
        img = new Texture("gameScene.jpg");
        startRect = new Rectangle(0, 0, img.getWidth(), img.getHeight());
        batch = new SpriteBatch();
        animation = new Anim("atlas/bob.atlas", "bobRun", Animation.PlayMode.LOOP);
        camera = new OrthographicCamera(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
//        TmxMapLoader tm = new TmxMapLoader();
//        map = tm.load("map/map.tmx");
        map = new TmxMapLoader().load("map/карта1.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);

    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
       ScreenUtils.clear(0, 0, 1, 0);
       camera.update();
      //  float x = Gdx.input.getX() - animation.getFrame().getRegionWidth();
     //   float y = Gdx.graphics.getHeight() - Gdx.input.getY() - animation.getFrame().getRegionHeight();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.rect(startRect.x, startRect.y, startRect.width, startRect.height);
        shapeRenderer.end();
        animation.setTime(Gdx.graphics.getDeltaTime());
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        runMan();
        batch.draw(img,0,0);
        batch.draw(animation.getFrame(), moveX, 105);
        batch.end();
        mapRenderer.setView(camera);
        mapRenderer.render();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY)) {
            dispose();
            game.setScreen(new MenuScreen(game));
        }
    }
    public void runMan() {
        if (!animation.getFrame().isFlipX() && !lookRight) animation.getFrame().flip(true, false);
        if (animation.getFrame().isFlipX() && lookRight) animation.getFrame().flip(true, false);

      //  int finish = Gdx.graphics.getWidth() - animation.getFrame().getRegionWidth();
        int finish = img.getWidth() - animation.getFrame().getRegionWidth();
        if (moveX == finish) {
            lookRight = false;
            isInFrame = false;
        }
        if (moveX == 0 && animation.getFrame().isFlipX()) {
            lookRight = true;
            isInFrame = true;
        }
        if (isInFrame) {
            moveX++;
        } else {
            moveX--;
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