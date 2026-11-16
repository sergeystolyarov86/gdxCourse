package com.mygdx.game.graphics;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * Покадровая анимация, владеющая своим источником текстур (атлас или спрайт-лист).
 */
public final class SpriteAnimation implements Disposable {

    private final Animation<TextureRegion> animation;
    private final Disposable textureSource;
    private float stateTime;

    private SpriteAnimation(Animation<TextureRegion> animation, Disposable textureSource) {
        this.animation = animation;
        this.textureSource = textureSource;
    }

    public static SpriteAnimation fromAtlas(String atlasPath, String regionName,
                                            float frameDuration, Animation.PlayMode playMode) {
        TextureAtlas atlas = new TextureAtlas(atlasPath);
        Array<TextureAtlas.AtlasRegion> regions = atlas.findRegions(regionName);
        if (regions.size == 0) {
            atlas.dispose();
            throw new IllegalArgumentException("Region '" + regionName + "' not found in " + atlasPath);
        }
        return new SpriteAnimation(new Animation<TextureRegion>(frameDuration, regions, playMode), atlas);
    }

    public static SpriteAnimation fromSpriteSheet(String texturePath, int columns, int rows,
                                                  float frameDuration, Animation.PlayMode playMode) {
        Texture texture = new Texture(texturePath);
        TextureRegion[][] grid = TextureRegion.split(texture,
                texture.getWidth() / columns, texture.getHeight() / rows);

        Array<TextureRegion> frames = new Array<>(columns * rows);
        for (TextureRegion[] row : grid) {
            frames.addAll(row);
        }
        return new SpriteAnimation(new Animation<TextureRegion>(frameDuration, frames, playMode), texture);
    }

    public void update(float delta) {
        stateTime += delta;
    }

    public void reset() {
        stateTime = 0f;
    }

    public TextureRegion getFrame() {
        return animation.getKeyFrame(stateTime);
    }

    /** Кадр на заданный момент — для анимации, общей у нескольких объектов со своим временем. */
    public TextureRegion getFrame(float time) {
        return animation.getKeyFrame(time);
    }

    public boolean isFinished() {
        return animation.isAnimationFinished(stateTime);
    }

    @Override
    public void dispose() {
        textureSource.dispose();
    }
}
