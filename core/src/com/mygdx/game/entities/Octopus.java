package com.mygdx.game.entities;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.mygdx.game.graphics.EnemyArt;

/** Осьминог-штурмовик: идёт на героя, а подойдя на дистанцию выстрела, останавливается и бьёт из гарпуна. */
public final class Octopus extends Enemy {

    public static final float WIDTH_PX = 28f;
    public static final float HEIGHT_PX = 56f;

    private static final int HEALTH = 3;
    private static final float WALK_SPEED = 1.1f;
    private static final float JUMP_SPEED = 4.5f;
    private static final float STUCK_TIME = 0.25f;

    private static final float FIRE_RANGE_X_PX = 340f;
    private static final float FIRE_RANGE_Y_PX = 70f;
    private static final float FIRE_INTERVAL = 2.2f;
    private static final float FIRST_SHOT_DELAY = 0.8f;
    private static final float SHOOT_POSE_TIME = 0.25f;
    /** Дуло гарпуна относительно центра тела по X и низа тела по Y, пиксели. */
    private static final float MUZZLE_X = 28f;
    private static final float MUZZLE_Y = 28f;

    private final EnemyArt art;
    private final Vector2 heroPosition = new Vector2();
    private float stuckTime;
    private float fireCooldown = FIRST_SHOT_DELAY;
    private float shootPose;

    public Octopus(Body body, EnemyArt art) {
        super(body, WIDTH_PX, HEIGHT_PX, HEALTH, -4f, true);
        this.art = art;
    }

    @Override
    protected void act(float delta, EnemyContext context) {
        fireCooldown = Math.max(0f, fireCooldown - delta);
        shootPose = Math.max(0f, shootPose - delta);

        context.getHeroPositionPx(heroPosition);
        float dx = heroPosition.x - centerXPx();
        float dy = heroPosition.y - centerYPx();
        facingRight = dx > 0f;
        Vector2 velocity = body.getLinearVelocity();

        boolean inRange = Math.abs(dx) < FIRE_RANGE_X_PX && Math.abs(dy) < FIRE_RANGE_Y_PX;
        if (inRange) {
            body.setLinearVelocity(0f, velocity.y); // встаёт и целится
            stuckTime = 0f;
            if (fireCooldown <= 0f) {
                fireCooldown = FIRE_INTERVAL;
                shootPose = SHOOT_POSE_TIME;
                context.fireHarpoon(centerXPx() + facing() * MUZZLE_X, bottomPx() + MUZZLE_Y, facing());
            }
            return;
        }

        body.setLinearVelocity(facing() * WALK_SPEED, velocity.y);
        // Упёрся в препятствие: стоит и не падает — прыгаем.
        boolean blocked = Math.abs(velocity.x) < WALK_SPEED * 0.2f && Math.abs(velocity.y) < 0.01f;
        stuckTime = blocked ? stuckTime + delta : 0f;
        if (stuckTime >= STUCK_TIME) {
            body.setLinearVelocity(facing() * WALK_SPEED, JUMP_SPEED);
            stuckTime = 0f;
        }
    }

    @Override
    protected TextureRegion currentFrame() {
        if (shootPose > 0f) return art.octopusShoot;
        boolean walking = Math.abs(body.getLinearVelocity().x) > 0.1f;
        return art.octopusWalk.getKeyFrame(walking ? time : 0f);
    }
}
