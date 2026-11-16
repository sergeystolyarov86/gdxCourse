package com.mygdx.game.physics;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;

/**
 * Обёртка над Box2D {@link World}: фиксированный шаг симуляции,
 * создание тел из прямоугольников в пикселях и отладочная отрисовка.
 * Снаружи всё задаётся в пикселях, внутри — в метрах.
 */
public final class PhysicsWorld implements Disposable {

    /** Пикселей в одном метре. */
    public static final float PPM = 100f;

    private static final Vector2 GRAVITY = new Vector2(0f, -9.81f);
    private static final float TIME_STEP = 1f / 60f;
    private static final float MAX_FRAME_TIME = 0.25f;
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 2;
    /** Категория столкновений врагов; у остальных фикстур категория по умолчанию (0x0001). */
    private static final short CATEGORY_ENEMY = 0x0002;

    private final World world;
    private final Box2DDebugRenderer debugRenderer = new Box2DDebugRenderer();
    private final Matrix4 debugMatrix = new Matrix4();
    private float accumulator;

    public PhysicsWorld(ContactHandler contactHandler) {
        world = new World(GRAVITY, true);
        world.setContactListener(new GameContactListener(contactHandler));
    }

    public static float toMeters(float pixels) {
        return pixels / PPM;
    }

    public static float toPixels(float meters) {
        return meters * PPM;
    }

    /** Шаг с фиксированным dt: физика не зависит от FPS. */
    public void update(float delta) {
        accumulator += Math.min(delta, MAX_FRAME_TIME);
        while (accumulator >= TIME_STEP) {
            world.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
            accumulator -= TIME_STEP;
        }
    }

    /** Создаёт тело-прямоугольник; {@code boundsPx} — прямоугольник в пикселях мира. */
    public Body createBox(BodyDef.BodyType bodyType, Rectangle boundsPx,
                          FixtureType fixtureType, boolean sensor, float friction) {
        BodyDef def = new BodyDef();
        def.type = bodyType;
        def.position.set(
                toMeters(boundsPx.x + boundsPx.width / 2f),
                toMeters(boundsPx.y + boundsPx.height / 2f));

        Body body = world.createBody(def);
        attachBox(body, boundsPx.width, boundsPx.height, 0f, 0f, fixtureType, sensor, friction);
        return body;
    }

    /**
     * Создаёт пулю: динамическое тело-сенсор без гравитации с постоянной скоростью.
     * Динамическое — потому что статические тела (медузы, стены) не дают контактов с кинематическими.
     */
    public Body createBullet(float centerXPx, float centerYPx, float widthPx, float heightPx,
                             float velocityXMeters, float velocityYMeters) {
        return createProjectile(centerXPx, centerYPx, widthPx, heightPx,
                velocityXMeters, velocityYMeters, 0f, FixtureType.BULLET);
    }

    /** Снаряд-сенсор с начальной скоростью (м/с); {@code gravityScale} 0 — летит по прямой. */
    public Body createProjectile(float centerXPx, float centerYPx, float widthPx, float heightPx,
                                 float velocityXMeters, float velocityYMeters, float gravityScale,
                                 FixtureType fixtureType) {
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;
        def.position.set(toMeters(centerXPx), toMeters(centerYPx));
        def.gravityScale = gravityScale;
        def.fixedRotation = true;
        def.bullet = true;
        def.linearVelocity.set(velocityXMeters, velocityYMeters);

        Body body = world.createBody(def);
        attachBox(body, widthPx, heightPx, 0f, 0f, fixtureType, true, 0f);
        return body;
    }

    /** Добавляет к телу прямоугольный сенсор со смещением относительно центра (в пикселях). */
    public void addSensor(Body body, float widthPx, float heightPx,
                          float offsetXPx, float offsetYPx, FixtureType fixtureType) {
        attachBox(body, widthPx, heightPx, offsetXPx, offsetYPx, fixtureType, true, 0f);
    }

    private void attachBox(Body body, float widthPx, float heightPx, float offsetXPx, float offsetYPx,
                           FixtureType fixtureType, boolean sensor, float friction) {
        PolygonShape shape = new PolygonShape();
        try {
            shape.setAsBox(toMeters(widthPx / 2f), toMeters(heightPx / 2f),
                    new Vector2(toMeters(offsetXPx), toMeters(offsetYPx)), 0f);

            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = shape;
            fixtureDef.density = 1f;
            fixtureDef.friction = friction;
            fixtureDef.restitution = 0f;
            fixtureDef.isSensor = sensor;
            if (fixtureType == FixtureType.ENEMY) {
                // Враги не сталкиваются друг с другом, иначе толпа застревает и лезет друг на друга.
                fixtureDef.filter.categoryBits = CATEGORY_ENEMY;
                fixtureDef.filter.maskBits = (short) ~CATEGORY_ENEMY;
            }

            body.createFixture(fixtureDef).setUserData(fixtureType);
        } finally {
            shape.dispose();
        }
    }

    /** Нельзя вызывать во время {@link #update(float)} (то есть из обработчиков контактов). */
    public void destroyBody(Body body) {
        world.destroyBody(body);
    }

    /** @param pixelProjection матрица камеры в пикселях; масштабируется в метры. */
    public void debugRender(Matrix4 pixelProjection) {
        debugMatrix.set(pixelProjection).scl(PPM);
        debugRenderer.render(world, debugMatrix);
    }

    @Override
    public void dispose() {
        world.dispose();
        debugRenderer.dispose();
    }
}
