package com.mygdx.game.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Единая точка доступа к звуку. Все ресурсы загружаются один раз
 * при создании и освобождаются в {@link #dispose()}.
 */
public final class GameAudio implements Disposable {

    public enum Track {
        MENU("music/intro.mp3", 1f),
        GAME("music/gameMusic.mp3", 0.03f);

        private final String path;
        private final float volume;

        Track(String path, float volume) {
            this.path = path;
            this.volume = volume;
        }
    }

    public enum Effect {
        JUMP("music/jump.mp3", 0.1f, 0.8f),
        CATCH("music/catch.mp3", 0.5f, 1f),
        SHOT("music/shot.wav", 0.25f, 1f),
        HURT("music/oops.mp3", 0.6f, 1f),
        ENEMY_DOWN("music/boom.wav", 0.35f, 1.2f),
        ENEMY_SHOT("music/shot.wav", 0.15f, 0.6f),
        EXPLOSION("music/boom.wav", 0.4f, 1f),
        VICTORY("music/catch.mp3", 0.7f, 1.2f);

        private final String path;
        private final float volume;
        private final float pitch;

        Effect(String path, float volume, float pitch) {
            this.path = path;
            this.volume = volume;
            this.pitch = pitch;
        }
    }

    private final Map<Track, Music> tracks = new EnumMap<>(Track.class);
    private final Map<Effect, Sound> effects = new EnumMap<>(Effect.class);
    private Music current;

    public GameAudio() {
        for (Track track : Track.values()) {
            Music music = Gdx.audio.newMusic(Gdx.files.internal(track.path));
            music.setLooping(true);
            music.setVolume(track.volume);
            tracks.put(track, music);
        }
        for (Effect effect : Effect.values()) {
            effects.put(effect, Gdx.audio.newSound(Gdx.files.internal(effect.path)));
        }
    }

    public void playMusic(Track track) {
        Music next = tracks.get(track);
        if (next == current && current.isPlaying()) return;
        stopMusic();
        current = next;
        current.play();
    }

    public void stopMusic() {
        if (current != null) {
            current.stop();
            current = null;
        }
    }

    public void play(Effect effect) {
        effects.get(effect).play(effect.volume, effect.pitch, 0f);
    }

    @Override
    public void dispose() {
        stopMusic();
        for (Music music : tracks.values()) music.dispose();
        for (Sound sound : effects.values()) sound.dispose();
        tracks.clear();
        effects.clear();
    }
}
