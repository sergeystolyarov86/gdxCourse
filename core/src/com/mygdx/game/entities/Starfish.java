package com.mygdx.game.entities;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.mygdx.game.graphics.EnemyArt;

/**
 * Морская звезда-громила: медленно идёт, а вблизи замирает, трясётся от злости
 * и бросается на героя тараном. Живучая.
 */
public final class Starfish extends Enemy {

    public static final float WIDTH_PX = 40f;
    public static final float HEIGHT_PX = 52f;

    private static final int HEALTH = 6;
    private static final float WALK_SPEED = 0.8f;
    private static final float CHARGE_SPEED = 4f;
    private static final float JUMP_SPEED = 4.5f;
    private static final float STUCK_TIME = 0.3f;

    private static final float CHARGE_RANGE_X_PX = 280f;
    private static final float CHARGE_RANGE_Y_PX = 60f;
    private static final float WINDUP_TIME = 0.45f;
    private static final float CHARGE_TIME = 0.7f;
    private static final float RECOVER_TIME = 0.8f;
    private static final float WINDUP_SHAKE_PX = 1.5f;

    private enum State { WALK, WINDUP, CHARGE, RECOVER }

    private final EnemyArt art;
    private final Vector2 heroPosition = new Vector2();
    private State state = State.WALK;
    private float stateTime;
    private float stuckTime;

    public Starfish(Body body, EnemyArt art) {
        super(body, WIDTH_PX, HEIGHT_PX, HEALTH, 0f, true);
        this.art = art;
    }

    @Override
    protected void act(float delta, EnemyContext context) {
        stateTime += delta;
        Vector2 velocity = body.getLinearVelocity();
        context.getHeroPositionPx(heroPosition);
        float dx = heroPosition.x - centerXPx();
        float dy = heroPosition.y - centerYPx();

        switch (state) {
            case WALK:
                facingRight = dx > 0f;
                body.setLinearVelocity(facing() * WALK_SPEED, velocity.y);
                jumpIfStuck(delta, velocity);
                if (Math.abs(dx) < CHARGE_RANGE_X_PX && Math.abs(dy) < CHARGE_RANGE_Y_PX) {
                    enter(State.WINDUP);
                }
                break;
            case WINDUP:
                body.setLinearVelocity(0f, velocity.y);
                if (stateTime >= WINDUP_TIME) enter(State.CHARGE);
                break;
            case CHARGE:
                // Направление зафиксировано в момент разгона — от тарана можно увернуться прыжком.
                body.setLinearVelocity(facing() * CHARGE_SPEED, velocity.y);
                if (stateTime >= CHARGE_TIME) enter(State.RECOVER);
                break;
            case RECOVER:
                body.setLinearVelocity(0f, velocity.y);
                if (stateTime >= RECOVER_TIME) enter(State.WALK);
                break;
        }
    }

    private void enter(State next) {
        state = next;
        stateTime = 0f;
        stuckTime = 0f;
    }

    private void jumpIfStuck(float delta, Vector2 velocity) {
        boolean blocked = Math.abs(velocity.x) < WALK_SPEED * 0.2f && Math.abs(velocity.y) < 0.01f;
        stuckTime = blocked ? stuckTime + delta : 0f;
        if (stuckTime >= STUCK_TIME) {
            body.setLinearVelocity(facing() * WALK_SPEED, JUMP_SPEED);
            stuckTime = 0f;
        }
    }

    @Override
    protected float drawJitterX() {
        return state == State.WINDUP ? MathUtils.random(-WINDUP_SHAKE_PX, WINDUP_SHAKE_PX) : 0f;
    }

    @Override
    protected TextureRegion currentFrame() {
        switch (state) {
            case CHARGE:
                return art.starfishWalk.getKeyFrame(time * 3f); // быстро перебирает ногами
            case WALK:
                return art.starfishWalk.getKeyFrame(time);
            default:
                return art.starfishWalk.getKeyFrame(0f);
        }
    }
}
