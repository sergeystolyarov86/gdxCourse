package com.mygdx.game.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.mygdx.game.physics.PhysicsWorld;

/** Общее для всех врагов: здоровье, вспышка при попадании, направление взгляда и отрисовка. */
public abstract class Enemy {

    private static final float HIT_FLASH_TIME = 0.1f;

    protected final Body body;
    protected final float widthPx;
    protected final float heightPx;
    /** На сколько пикселей центр тела смещён вправо от центра кадра (спрайт смотрит вправо). */
    private final float spriteShiftX;
    /** {@code true} — спрайт стоит на низу тела; {@code false} — центрирован (летающие). */
    private final boolean groundSprite;

    protected boolean facingRight;
    protected float time;
    private int health;
    private float hitFlash;
    private boolean dead;

    protected Enemy(Body body, float widthPx, float heightPx, int health,
                    float spriteShiftX, boolean groundSprite) {
        this.body = body;
        this.widthPx = widthPx;
        this.heightPx = heightPx;
        this.health = health;
        this.spriteShiftX = spriteShiftX;
        this.groundSprite = groundSprite;
        body.setUserData(this);
        body.setFixedRotation(true);
    }

    public final void update(float delta, EnemyContext context) {
        time += delta;
        hitFlash = Math.max(0f, hitFlash - delta);
        act(delta, context);
    }

    /** Поведение конкретного врага. */
    protected abstract void act(float delta, EnemyContext context);

    protected abstract TextureRegion currentFrame();

    /** Дополнительный сдвиг спрайта (например, дрожь перед рывком). */
    protected float drawJitterX() {
        return 0f;
    }

    /** @return {@code true}, если этим попаданием враг убит (ровно один раз) */
    public boolean hit() {
        if (dead) return false;
        hitFlash = HIT_FLASH_TIME;
        if (--health > 0) return false;
        dead = true;
        return true;
    }

    public boolean isDead() {
        return dead;
    }

    public Body getBody() {
        return body;
    }

    public Vector2 getCenterPx(Vector2 out) {
        Vector2 position = body.getPosition();
        return out.set(PhysicsWorld.toPixels(position.x), PhysicsWorld.toPixels(position.y));
    }

    protected float centerXPx() {
        return PhysicsWorld.toPixels(body.getPosition().x);
    }

    protected float centerYPx() {
        return PhysicsWorld.toPixels(body.getPosition().y);
    }

    protected float bottomPx() {
        return centerYPx() - heightPx / 2f;
    }

    protected int facing() {
        return facingRight ? 1 : -1;
    }

    public void draw(SpriteBatch batch) {
        TextureRegion frame = currentFrame();
        float width = frame.getRegionWidth();
        float height = frame.getRegionHeight();
        float left = centerXPx() - width / 2f - facing() * spriteShiftX + drawJitterX();
        float bottom = groundSprite ? bottomPx() : centerYPx() - height / 2f;

        if (hitFlash > 0f) batch.setColor(1f, 0.35f, 0.35f, 1f);
        batch.draw(frame, left, bottom, width / 2f, height / 2f, width, height,
                facingRight ? 1f : -1f, 1f, 0f);
        batch.setColor(1f, 1f, 1f, 1f);
    }
}
