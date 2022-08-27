package com.mygdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.mygdx.game.screens.MenuScreen;

public class Main extends Game {
    @Override
    public void create() {
         Gdx.graphics.setWindowedMode(800,528);
        setScreen(new MenuScreen(this));
    }

    @Override
    public void render() {super.render();}

    @Override
    public void setScreen(Screen screen) {
        super.setScreen(screen);
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}

