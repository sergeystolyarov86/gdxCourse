package com.mygdx.game.entities;

import com.badlogic.gdx.math.Vector2;

/** То, что враг может узнать об уровне и сделать в нём. Реализуется уровнем. */
public interface EnemyContext {

    /** Центр героя в пикселях мира. */
    Vector2 getHeroPositionPx(Vector2 out);

    /** @param direction 1 — вправо, -1 — влево */
    void fireHarpoon(float xPx, float yPx, int direction);

    void dropBomb(float xPx, float yPx);
}
