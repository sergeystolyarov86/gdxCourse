package com.mygdx.game.entities;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.mygdx.game.graphics.EnemyArt;
import com.mygdx.game.physics.PhysicsWorld;

/** Боевой дрон: висит над героем, покачиваясь из стороны в сторону, и сбрасывает бомбы. Сквозь стены пролетает. */
public final class Drone extends Enemy {

    public static final float WIDTH_PX = 44f;
    public static final float HEIGHT_PX = 20f;

    private static final int HEALTH = 2;
    private static final float MAX_SPEED = 2f;
    /** Насколько резко дрон догоняет точку зависания (1/с). */
    private static final float STEERING = 2f;
    private static final float HOVER_HEIGHT_PX = 190f;
    private static final float SWAY_PX = 70f;
    private static final float SWAY_FREQUENCY = 1.3f;

    private static final float BOMB_INTERVAL = 2f;
    private static final float BOMB_AIM_PX = 30f;
    private static final float BOMB_DROP_Y_PX = 14f;

    private final EnemyArt art;
    private final Vector2 heroPosition = new Vector2();
    private final Vector2 velocity = new Vector2();
    private final float swayPhase = MathUtils.random(MathUtils.PI2);
    private float bombCooldown = BOMB_INTERVAL / 2f;

    public Drone(Body body, EnemyArt art) {
        super(body, WIDTH_PX, HEIGHT_PX, HEALTH, 0f, false);
        this.art = art;
        body.setGravityScale(0f);
    }

    @Override
    protected void act(float delta, EnemyContext context) {
        bombCooldown = Math.max(0f, bombCooldown - delta);
        context.getHeroPositionPx(heroPosition);

        float targetX = heroPosition.x + SWAY_PX * MathUtils.sin(time * SWAY_FREQUENCY + swayPhase);
        float targetY = heroPosition.y + HOVER_HEIGHT_PX;
        velocity.set(targetX - centerXPx(), targetY - centerYPx())
                .scl(STEERING / PhysicsWorld.PPM)
                .limit(MAX_SPEED);
        body.setLinearVelocity(velocity);
        facingRight = heroPosition.x > centerXPx();

        if (bombCooldown <= 0f && Math.abs(heroPosition.x - centerXPx()) < BOMB_AIM_PX) {
            bombCooldown = BOMB_INTERVAL;
            context.dropBomb(centerXPx(), centerYPx() - BOMB_DROP_Y_PX);
        }
    }

    @Override
    protected TextureRegion currentFrame() {
        return art.droneFly.getKeyFrame(time);
    }
}
