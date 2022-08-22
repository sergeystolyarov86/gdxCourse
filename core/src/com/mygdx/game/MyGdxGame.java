package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

public class MyGdxGame extends ApplicationAdapter {
//    SpriteBatch batch;
//    int moveX;
//    Anim animation;
//    boolean isInFrame = true;
//    boolean lookRight = true;
//
//
//    @Override
//    public void create() {
//        batch = new SpriteBatch();
//        animation = new Anim("run.png", 5, 2, Animation.PlayMode.LOOP);
//    }
//
//    @Override
//    public void render() {
//        ScreenUtils.clear(0, 0, 1, 0);
//
//        animation.setTime(Gdx.graphics.getDeltaTime());
//        float x = Gdx.input.getX() - animation.getFrame().getRegionWidth();
//        float y = Gdx.graphics.getHeight() - Gdx.input.getY() - animation.getFrame().getRegionHeight();
//
//
//        batch.begin();
//        runMan();
//        batch.draw(animation.getFrame(), moveX, 0);
//        batch.end();
//    }
//
//    @Override
//    public void dispose() {
//        batch.dispose();
//        animation.dispose();
//    }
//
//
//    public void runMan() {
//        if (!animation.getFrame().isFlipX() && !lookRight) animation.getFrame().flip(true, false);
//        if (animation.getFrame().isFlipX() && lookRight) animation.getFrame().flip(true, false);
//
//        int finish = Gdx.graphics.getWidth() - animation.getFrame().getRegionWidth();
//        if (moveX == finish) {
//            lookRight = false;
//            isInFrame = false;
//        }
//        if (moveX == 0 && animation.getFrame().isFlipX()) {
//            lookRight = true;
//            isInFrame = true;
//        }
//        if (isInFrame) {
//            moveX++;
//        } else {
//            moveX--;
//        }
//    }
}

