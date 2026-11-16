package com.mygdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.Main;
import com.mygdx.game.audio.GameAudio;

public final class MenuScreen extends ScreenAdapter {

    private static final String BACKGROUND = "startScreen.png";

    private final Main game;
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final Viewport viewport = new FitViewport(Main.VIRTUAL_WIDTH, Main.VIRTUAL_HEIGHT);
    private final Texture background = new Texture(BACKGROUND);
    private final Rectangle startButton;
    private final Vector2 pointer = new Vector2();

    public MenuScreen(Main game) {
        this.game = game;
        startButton = new Rectangle(0, 0, Main.VIRTUAL_WIDTH, Main.VIRTUAL_HEIGHT);
    }

    @Override
    public void show() {
        game.getAudio().playMusic(GameAudio.Track.MENU);
    }

    @Override
    public void render(float delta) {
        if (isStartRequested()) {
            game.startGame();
            return; // этот экран уже освобождён
        }

        ScreenUtils.clear(Color.YELLOW);
        viewport.apply();

        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        batch.draw(background, 0, 0, Main.VIRTUAL_WIDTH, Main.VIRTUAL_HEIGHT);
        batch.end();

        shapes.setProjectionMatrix(viewport.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.BLACK);
        shapes.rect(startButton.x, startButton.y, startButton.width, startButton.height);
        shapes.end();
    }

    private boolean isStartRequested() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            return true;
        }
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            viewport.unproject(pointer.set(Gdx.input.getX(), Gdx.input.getY()));
            return startButton.contains(pointer);
        }
        return false;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void hide() {
        game.getAudio().stopMusic();
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        background.dispose();
    }
}
