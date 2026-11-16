package com.mygdx.game.graphics;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * Вся графика врагов и эффектов, загружается один раз на уровень.
 * Все спрайты нарисованы смотрящими вправо.
 */
public final class EnemyArt implements Disposable {

    public final Animation<TextureRegion> octopusWalk;
    public final TextureRegion octopusShoot;
    public final Animation<TextureRegion> starfishWalk;
    public final Animation<TextureRegion> droneFly;
    public final Animation<TextureRegion> explosion;
    public final TextureRegion harpoon;
    public final TextureRegion bomb;

    private final Array<Texture> textures = new Array<>();

    public EnemyArt() {
        TextureRegion[] octopus = frames("enemies/octopus.png", 5);
        octopusWalk = loop(0.15f, octopus[0], octopus[1], octopus[2], octopus[3]);
        octopusShoot = octopus[4];
        starfishWalk = loop(0.14f, frames("enemies/starfish.png", 4));
        droneFly = loop(0.05f, frames("enemies/drone.png", 4));
        explosion = new Animation<TextureRegion>(0.08f, frames("enemies/explosion.png", 4));
        harpoon = frames("enemies/harpoon.png", 1)[0];
        bomb = frames("enemies/bomb.png", 1)[0];
    }

    private TextureRegion[] frames(String path, int count) {
        Texture texture = new Texture(path);
        textures.add(texture);
        return TextureRegion.split(texture, texture.getWidth() / count, texture.getHeight())[0];
    }

    private static Animation<TextureRegion> loop(float frameDuration, TextureRegion... frames) {
        Animation<TextureRegion> animation = new Animation<TextureRegion>(frameDuration, frames);
        animation.setPlayMode(Animation.PlayMode.LOOP);
        return animation;
    }

    @Override
    public void dispose() {
        for (Texture texture : textures) texture.dispose();
        textures.clear();
    }
}
