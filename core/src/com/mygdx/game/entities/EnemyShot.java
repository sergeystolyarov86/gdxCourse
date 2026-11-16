package com.mygdx.game.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.mygdx.game.physics.PhysicsWorld;

/** Вражеский снаряд: гарпун летит по прямой, бомба падает и взрывается. */
public final class EnemyShot {

    public enum Kind {
        HARPOON(20f, 4f),
        BOMB(10f, 14f);

        public final float widthPx;
        public final float heightPx;

        Kind(float widthPx, float heightPx) {
            this.widthPx = widthPx;
            this.heightPx = heightPx;
        }
    }

    private static final float LIFETIME = 4f;

    private final Body body;
    private final Kind kind;
    private final TextureRegion region;
    private float age;
    private boolean dead;
    private boolean hitPlayer;

    public EnemyShot(Body body, Kind kind, TextureRegion region) {
        this.body = body;
        this.kind = kind;
        this.region = region;
        body.setUserData(this);
    }

    public void update(float delta) {
        age += delta;
        if (age >= LIFETIME) dead = true;
    }

    /** Вызывается из обработчика контактов — только помечает, удаление позже. */
    public void onHit(boolean player) {
        if (dead) return;
        dead = true;
        hitPlayer = player;
    }

    public boolean isDead() {
        return dead;
    }

    public boolean hasHitPlayer() {
        return hitPlayer;
    }

    public Kind getKind() {
        return kind;
    }

    public Body getBody() {
        return body;
    }

    public Vector2 getCenterPx(Vector2 out) {
        Vector2 position = body.getPosition();
        return out.set(PhysicsWorld.toPixels(position.x), PhysicsWorld.toPixels(position.y));
    }

    public void draw(SpriteBatch batch) {
        Vector2 position = body.getPosition();
        float width = region.getRegionWidth();
        float height = region.getRegionHeight();
        boolean flip = body.getLinearVelocity().x < 0f;
        batch.draw(region,
                PhysicsWorld.toPixels(position.x) - width / 2f,
                PhysicsWorld.toPixels(position.y) - height / 2f,
                width / 2f, height / 2f, width, height,
                flip ? -1f : 1f, 1f, 0f);
    }
}
