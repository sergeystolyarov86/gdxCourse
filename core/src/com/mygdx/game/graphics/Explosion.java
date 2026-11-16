package com.mygdx.game.graphics;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Одноразовая анимация взрыва в точке мира. */
public final class Explosion {

    private final Animation<TextureRegion> animation;
    private final float centerX;
    private final float centerY;
    private final float scale;
    private float time;

    public Explosion(Animation<TextureRegion> animation, float centerX, float centerY, float scale) {
        this.animation = animation;
        this.centerX = centerX;
        this.centerY = centerY;
        this.scale = scale;
    }

    /** @return {@code true}, когда анимация доиграна и взрыв можно убрать */
    public boolean update(float delta) {
        time += delta;
        return animation.isAnimationFinished(time);
    }

    public void draw(SpriteBatch batch) {
        TextureRegion frame = animation.getKeyFrame(time);
        float width = frame.getRegionWidth() * scale;
        float height = frame.getRegionHeight() * scale;
        batch.draw(frame, centerX - width / 2f, centerY - height / 2f, width, height);
    }
}
