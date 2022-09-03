package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

public class GameSounds {
    private Music gameMusic;
    private Music introMusic;
    private Sound jump;
    private Sound catchJellyFish;

    public void playGameMusic() {
        gameMusic = Gdx.audio.newMusic(Gdx.files.internal("music/gameMusic.mp3"));
        gameMusic.setLooping(true);
        gameMusic.setVolume(0.03f);
        gameMusic.play();
    }

    public void playIntroMusic() {
        introMusic = Gdx.audio.newMusic(Gdx.files.internal("music/intro.mp3"));
        introMusic.setLooping(true);
        introMusic.play();
    }

    public void jumpSound() {
        jump = Gdx.audio.newSound(Gdx.files.internal("music/jump.mp3"));
        jump.play(0.1f,0.8f,0);
    }
    public void jellyFishSound() {
        catchJellyFish = Gdx.audio.newSound(Gdx.files.internal("music/catch.mp3"));
        catchJellyFish.play(0.5f,1,0);
    }

    public void disposeGameMusic() {
        gameMusic.dispose();
    }

    public void disposeIntroMusic() {
        introMusic.dispose();
    }


}
