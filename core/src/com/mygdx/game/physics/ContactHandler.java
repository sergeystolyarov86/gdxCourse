package com.mygdx.game.physics;

import com.badlogic.gdx.physics.box2d.Body;

/**
 * Игровые события, выделенные из сырых контактов Box2D.
 * Вызывается во время шага мира, поэтому изменять мир (удалять тела) здесь нельзя.
 */
public interface ContactHandler {

    /** Сенсор ног тела {@code owner} коснулся твёрдой поверхности или оторвался от неё. */
    void onGroundContact(Body owner, boolean began);

    /** Игрок коснулся собираемого предмета. */
    void onCollectibleTouched(Body collectible);

    /** Пуля задела медузу, врага или стену. */
    void onBulletHit(Body bullet, Body target, FixtureType targetType);

    /** Враг коснулся игрока ({@code began}) или перестал касаться. */
    void onEnemyContact(Body enemy, boolean began);

    /** Вражеский снаряд попал в игрока ({@code hitPlayer}) или в стену/пол. */
    void onEnemyShotHit(Body shot, boolean hitPlayer);
}
