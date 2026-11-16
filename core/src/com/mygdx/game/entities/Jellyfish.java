package com.mygdx.game.entities;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.physics.box2d.Body;

/** Собираемая медуза: физический сенсор и (опционально) клетка тайла, которой она нарисована. */
public final class Jellyfish {

    private final Body body;
    private final GridPoint2 tileCell;
    private boolean collected;

    /** @param tileCell клетка тайлового слоя с изображением медузы или {@code null} */
    public Jellyfish(Body body, GridPoint2 tileCell) {
        this.body = body;
        this.tileCell = tileCell;
        body.setUserData(this);
    }

    /** @return {@code true} только при первом вызове — защищает от двойного сбора. */
    public boolean markCollected() {
        if (collected) return false;
        collected = true;
        return true;
    }

    public Body getBody() {
        return body;
    }

    public GridPoint2 getTileCell() {
        return tileCell;
    }
}
