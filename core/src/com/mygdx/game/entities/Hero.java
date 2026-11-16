package com.mygdx.game.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Disposable;
import com.mygdx.game.graphics.SpriteAnimation;
import com.mygdx.game.physics.PhysicsWorld;

/** Управляемый игроком персонаж: движение, прыжок, стрельба, состояние «на земле» и отрисовка. */
public final class Hero implements Disposable {

    /** Скорость бега, м/с. */
    private static final float RUN_SPEED = 2.5f;
    /** Начальная вертикальная скорость прыжка, м/с (высота ≈ v²/2g ≈ 1 м). */
    private static final float JUMP_SPEED = 4.5f;

    /** Пауза между выстрелами, с (≈ 600 выстр./мин, как у АК). */
    private static final float FIRE_INTERVAL = 0.1f;
    /** Положение автомата относительно центра героя и низа спрайта, пиксели (для взгляда вправо). */
    private static final float WEAPON_OFFSET_X = -10f;
    private static final float WEAPON_OFFSET_Y = 26f;
    /** Дульный срез относительно левого нижнего угла текстуры автомата. */
    private static final float MUZZLE_X = 48f;
    private static final float MUZZLE_Y = 9f;
    /** Точка поворота автомата (рукоятка) относительно левого нижнего угла текстуры. */
    private static final float GRIP_X = 15f;
    private static final float GRIP_Y = 8f;
    /** Пределы угла прицела относительно горизонтали, градусы: вниз немного, вверх — вертикально. */
    private static final float MIN_AIM_DEG = -30f;
    private static final float MAX_AIM_DEG = 90f;
    /** Откат автомата при выстреле, пиксели, и время его возврата, с. */
    private static final float RECOIL_PX = 3f;
    private static final float RECOIL_TIME = 0.06f;

    public static final int MAX_HEALTH = 555;
    /** Неуязвимость после удара, с — чтобы толпа не снимала все жизни разом. */
    private static final float INVULNERABLE_TIME = 1.2f;
    /** Сколько секунд после удара герой не управляется (его отбрасывает). */
    private static final float KNOCKBACK_TIME = 0.3f;
    private static final float KNOCKBACK_SPEED_X = 3f;
    private static final float KNOCKBACK_SPEED_Y = 3f;
    private static final float BLINK_PERIOD = 0.1f;

    private final Body body;
    private final float heightPx;
    private final SpriteAnimation runAnimation;
    private final Texture weapon;

    private int groundContacts;
    private boolean facingRight = true;
    private boolean running;
    private float fireCooldown;
    private float recoil;
    private int health = MAX_HEALTH;
    private float invulnerable;
    private float knockback;
    /** Угол прицела в градусах: 0 — вперёд по взгляду, 90 — вверх. */
    private float aimDeg;
    private final Vector2 pivot = new Vector2();

    public Hero(Body body, float heightPx, SpriteAnimation runAnimation, Texture weapon) {
        this.body = body;
        this.heightPx = heightPx;
        this.runAnimation = runAnimation;
        this.weapon = weapon;
        body.setUserData(this);
        body.setFixedRotation(true);
        body.setSleepingAllowed(false);
    }

    /** @param direction -1 влево, 0 стоять, 1 вправо */
    public void move(int direction) {
        if (knockback > 0f) return; // летим от удара, управление вернётся позже
        body.setLinearVelocity(direction * RUN_SPEED, body.getLinearVelocity().y);
        running = direction != 0;
        if (running) {
            facingRight = direction > 0;
        }
    }

    /** Целится горизонтально по направлению взгляда. */
    public void aimForward() {
        aimDeg = 0f;
    }

    /** Целится вертикально вверх — по дронам над головой. */
    public void aimUp() {
        aimDeg = MAX_AIM_DEG;
    }

    /** Целится в точку мира (курсор мыши) и поворачивается к ней лицом. */
    public void aimAt(float targetXPx, float targetYPx) {
        float dx = targetXPx - centerXPx();
        float dy = targetYPx - (bottomPx() + WEAPON_OFFSET_Y + GRIP_Y);
        if (Math.abs(dx) > 1f) facingRight = dx > 0f;
        aimDeg = MathUtils.clamp(MathUtils.atan2(dy, Math.abs(dx)) * MathUtils.radiansToDegrees,
                MIN_AIM_DEG, MAX_AIM_DEG);
    }

    /** @return {@code true}, если прыжок выполнен (герой стоял на земле) */
    public boolean jump() {
        if (!isOnGround()) return false;
        body.setLinearVelocity(body.getLinearVelocity().x, JUMP_SPEED);
        return true;
    }

    /**
     * Нажимает на спуск. Сама пуля создаётся уровнем.
     * @return {@code true}, если выстрел произошёл (автомат перезарядился)
     */
    public boolean tryFire() {
        if (fireCooldown > 0f) return false;
        fireCooldown = FIRE_INTERVAL;
        recoil = RECOIL_TIME;
        return true;
    }

    public void update(float delta) {
        fireCooldown = Math.max(0f, fireCooldown - delta);
        invulnerable = Math.max(0f, invulnerable - delta);
        knockback = Math.max(0f, knockback - delta);
        recoil = Math.max(0f, recoil - delta);
        if (!running) {
            runAnimation.reset();
        } else if (isOnGround()) {
            runAnimation.update(delta);
        }
    }

    /**
     * Удар врага: минус жизнь и отбрасывание от него.
     * @param fromXMeters откуда пришёл удар
     * @return {@code true}, если удар засчитан (герой не был неуязвим)
     */
    public boolean hurt(float fromXMeters) {
        if (invulnerable > 0f || health <= 0) return false;
        health--;
        invulnerable = INVULNERABLE_TIME;
        knockback = KNOCKBACK_TIME;
        float away = body.getPosition().x >= fromXMeters ? 1f : -1f;
        body.setLinearVelocity(away * KNOCKBACK_SPEED_X, KNOCKBACK_SPEED_Y);
        return true;
    }

    public int getHealth() {
        return health;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public float getXMeters() {
        return body.getPosition().x;
    }

    public void draw(SpriteBatch batch) {
        // Мигаем, пока неуязвимы.
        if (invulnerable > 0f && (int) (invulnerable / BLINK_PERIOD) % 2 == 0) return;

        TextureRegion frame = runAnimation.getFrame();
        float width = frame.getRegionWidth();
        float height = frame.getRegionHeight();
        float centerX = centerXPx();
        float bottom = bottomPx();

        // Отражаем при отрисовке, не изменяя общие регионы атласа.
        batch.draw(frame, centerX - width / 2f, bottom,
                width / 2f, height / 2f, width, height,
                facingRight ? 1f : -1f, 1f, 0f);

        // Автомат поворачивается вокруг рукоятки; при взгляде влево текстура и угол зеркалятся.
        float kick = recoil > 0f ? RECOIL_PX * recoil / RECOIL_TIME : 0f;
        float weaponWidth = weapon.getWidth();
        float weaponHeight = weapon.getHeight();
        getPivotPx(pivot);
        float cos = MathUtils.cosDeg(aimDeg);
        float sin = MathUtils.sinDeg(aimDeg);
        float pivotX = pivot.x - getFacing() * kick * cos;
        float pivotY = pivot.y - kick * sin;
        float originX = facingRight ? GRIP_X : weaponWidth - GRIP_X;
        batch.draw(weapon, pivotX - originX, pivotY - GRIP_Y,
                originX, GRIP_Y, weaponWidth, weaponHeight, 1f, 1f,
                facingRight ? aimDeg : -aimDeg,
                0, 0, weapon.getWidth(), weapon.getHeight(),
                !facingRight, false);
    }

    /** Точка вылета пули в пикселях мира (с учётом угла прицела). */
    public Vector2 getMuzzlePx(Vector2 out) {
        getPivotPx(pivot);
        float lx = MUZZLE_X - GRIP_X;
        float ly = MUZZLE_Y - GRIP_Y;
        float cos = MathUtils.cosDeg(aimDeg);
        float sin = MathUtils.sinDeg(aimDeg);
        return out.set(pivot.x + getFacing() * (lx * cos - ly * sin),
                pivot.y + lx * sin + ly * cos);
    }

    /** Единичный вектор направления выстрела. */
    public Vector2 getAimDirection(Vector2 out) {
        return out.set(getFacing() * MathUtils.cosDeg(aimDeg), MathUtils.sinDeg(aimDeg));
    }

    private Vector2 getPivotPx(Vector2 out) {
        return out.set(centerXPx() + getFacing() * (WEAPON_OFFSET_X + GRIP_X),
                bottomPx() + WEAPON_OFFSET_Y + GRIP_Y);
    }

    /** @return 1 — смотрит вправо, -1 — влево */
    public int getFacing() {
        return facingRight ? 1 : -1;
    }

    public void onGroundContact(boolean began) {
        groundContacts = Math.max(0, groundContacts + (began ? 1 : -1));
    }

    public boolean isOnGround() {
        return groundContacts > 0;
    }

    public Vector2 getPositionPx(Vector2 out) {
        Vector2 position = body.getPosition();
        return out.set(PhysicsWorld.toPixels(position.x), PhysicsWorld.toPixels(position.y));
    }

    private float centerXPx() {
        return PhysicsWorld.toPixels(body.getPosition().x);
    }

    private float bottomPx() {
        return PhysicsWorld.toPixels(body.getPosition().y) - heightPx / 2f;
    }

    @Override
    public void dispose() {
        runAnimation.dispose();
        weapon.dispose();
    }
}
