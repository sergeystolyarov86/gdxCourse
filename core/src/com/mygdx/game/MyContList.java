package com.mygdx.game;

import com.badlogic.gdx.physics.box2d.*;
import com.mygdx.game.screens.GameScreen;

public class MyContList implements ContactListener {
    public int count;

    @Override
    public void beginContact(Contact contact) {
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();
        if (a.getUserData() != null && b.getUserData() != null) {
            String tmpA = (String) a.getUserData();
            String tmpB = (String) b.getUserData();
            if (tmpA.equals("hero") && tmpB.equals("jellyfish")) {
                GameScreen.bodies.add(b.getBody());
            }
            if (tmpB.equals("hero") && tmpA.equals("jellyfish")) {
                GameScreen.bodies.add(b.getBody());
            }

            if (tmpA.equals("hero") && tmpB.equals("floor")) {
                count++;
            }
            if (tmpB.equals("hero") && tmpA.equals("floor")) {
                count++;
            }

        }

    }

    @Override
    public void endContact(Contact contact) {
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();
        if (a.getUserData() != null && b.getUserData() != null) {
            String tmpA = (String) a.getUserData();
            String tmpB = (String) b.getUserData();

            if (tmpA.equals("hero") && tmpB.equals("floor")) {
                count--;
            }
            if (tmpB.equals("hero") && tmpA.equals("floor")) {
                count--;
            }
        }
    }

    public boolean isOnGround() {
        return count > 0;
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {

    }
}
