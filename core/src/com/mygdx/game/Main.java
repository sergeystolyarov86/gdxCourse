package com.mygdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.Screen;
import com.mygdx.game.audio.GameAudio;
import com.mygdx.game.screens.GameScreen;
import com.mygdx.game.screens.MenuScreen;

/** Точка входа: владеет общими ресурсами и переключает экраны. */
public class Main extends Game {

    /** Виртуальное разрешение: под него нарисованы фоны, при любом размере окна картинка масштабируется. */
    public static final float VIRTUAL_WIDTH = 800f;
    public static final float VIRTUAL_HEIGHT = 526f;

    private GameAudio audio;

    @Override
    public void create() {
        Gdx.app.log("GL", Gdx.gl.glGetString(GL20.GL_RENDERER));
        audio = new GameAudio();
        showMenu();
    }

    public void showMenu() {
        switchTo(new MenuScreen(this));
    }

    public void startGame() {
        switchTo(new GameScreen(this));
    }

    public GameAudio getAudio() {
        return audio;
    }

    /** Освобождает предыдущий экран. Вызывающий должен сразу выйти из своего render(). */
    private void switchTo(Screen next) {
        Screen previous = getScreen();
        setScreen(next);
        if (previous != null) {
            previous.dispose();
        }
    }

    @Override
    public void dispose() {
        Screen current = getScreen();
        super.dispose(); // вызывает hide() у текущего экрана
        if (current != null) {
            current.dispose();
        }
        audio.dispose();
    }
}
