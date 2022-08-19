package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

public class MyGdxGame extends ApplicationAdapter {
    SpriteBatch batch;
    int moveX;
    Anim animation;
    boolean isInFrame = true;
    boolean isRevers = true;


    @Override
    public void create() {
        batch = new SpriteBatch();
        animation = new Anim("run.png", 5, 2, Animation.PlayMode.LOOP);
    }

    @Override
    public void render() {
        ScreenUtils.clear(1, 1, 1, 1);

        animation.setTime(Gdx.graphics.getDeltaTime());
        float x = Gdx.input.getX() - animation.getFrame().getRegionWidth() ;
        float y = Gdx.graphics.getHeight() - Gdx.input.getY() - animation.getFrame().getRegionHeight() ;


        batch.begin();
        runMan();
        batch.draw(animation.getFrame(), moveX, 0);
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        animation.dispose();
    }


    /**
     мой персонаж добегает до 1/4 экрана(сократил расстояние,чтобы меньше ждать) ,разворачивается и бежит
     в развороте в начало.добегает до начала и бежит обратно,но не разворачивается.в блок if в 53 строке заходит,
     но не возвращает персонажа в начальное положение. подскажите,пожалуйста,в чем ошибка?
     */

    public void runMan() {
        if (!animation.getFrame().isFlipX() && !isRevers){ animation.getFrame().flip(true, false);
            System.out.println("flipX=true");}
        if (animation.getFrame().isFlipX() || isRevers) {animation.getFrame().flip(false, false);
            System.out.println("flipX=false");
            System.out.println(animation.getFrame().isFlipX());}

        int finish = (Gdx.graphics.getWidth() - animation.getFrame().getRegionWidth()) / 4;
        if (moveX == finish) {
            isRevers = false;
            isInFrame = false;
        }
        if (moveX == 0 && animation.getFrame().isFlipX()) {
            isRevers = true;
            isInFrame = true;
        }
        if (isInFrame) {
            moveX++;
        } else {
            moveX--;
        }
    }
}

