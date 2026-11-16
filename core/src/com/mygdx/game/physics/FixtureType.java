package com.mygdx.game.physics;

/** Метка фикстуры, хранится в {@code Fixture#getUserData()}. */
public enum FixtureType {
    PLAYER,
    PLAYER_FOOT,
    COLLECTIBLE,
    SOLID,
    BULLET,
    ENEMY,
    ENEMY_SHOT
}
