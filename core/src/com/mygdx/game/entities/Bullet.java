package com.mygdx.game.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.mygdx.game.physics.PhysicsWorld;

/** Пуля: летит по прямой, исчезает при попадании или по истечении времени жизни. */
public final class Bullet {

    public static final float WIDTH_PX = 8f;
    public static final float HEIGHT_PX = 3f;

    /** Время жизни, с — чтобы промахи не копились в мире. */
    private static final float LIFETIME = 1.5f;

    private final Body body;
    private float age;
    private boolean dead;

    public Bullet(Body body) {
        this.body = body;
        body.setUserData(this);
    }

    /** @return {@code true}, если пуля отжила своё и её пора удалить */
    public boolean update(float delta) {
        age += delta;
        return age >= LIFETIME;
    }

    /** @return {@code true} только при первом вызове — защищает от двойного удаления. */
    public boolean markDead() {
        if (dead) return false;
        dead = true;
        return true;
    }

    public boolean isDead() {
        return dead;
    }

    public Body getBody() {
        return body;
    }

    /** Пуля рисуется вытянутой вдоль направления полёта. */
    public void draw(SpriteBatch batch, Texture texture) {
        Vector2 position = body.getPosition();
        float rotation = body.getLinearVelocity().angleDeg();
        batch.draw(texture,
                PhysicsWorld.toPixels(position.x) - WIDTH_PX / 2f,
                PhysicsWorld.toPixels(position.y) - HEIGHT_PX / 2f,
                WIDTH_PX / 2f, HEIGHT_PX / 2f, WIDTH_PX, HEIGHT_PX, 1f, 1f, rotation,
                0, 0, texture.getWidth(), texture.getHeight(), false, false);
    }
}
