package com.mygdx.game.physics;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;

/**
 * Переводит контакты Box2D в события {@link ContactHandler}.
 * Каждая пара проверяется в обоих порядках, поэтому порядок фикстур A/B не важен.
 */
final class GameContactListener implements ContactListener {

    private final ContactHandler handler;

    GameContactListener(ContactHandler handler) {
        this.handler = handler;
    }

    @Override
    public void beginContact(Contact contact) {
        dispatch(contact.getFixtureA(), contact.getFixtureB(), true);
        dispatch(contact.getFixtureB(), contact.getFixtureA(), true);
    }

    @Override
    public void endContact(Contact contact) {
        dispatch(contact.getFixtureA(), contact.getFixtureB(), false);
        dispatch(contact.getFixtureB(), contact.getFixtureA(), false);
    }

    private void dispatch(Fixture self, Fixture other, boolean began) {
        Object type = self.getUserData();

        if (type == FixtureType.PLAYER_FOOT && !other.isSensor()) {
            handler.onGroundContact(self.getBody(), began);
        } else if (began && type == FixtureType.PLAYER && other.getUserData() == FixtureType.COLLECTIBLE) {
            handler.onCollectibleTouched(other.getBody());
        } else if (type == FixtureType.PLAYER && other.getUserData() == FixtureType.ENEMY) {
            handler.onEnemyContact(other.getBody(), began);
        } else if (began && type == FixtureType.ENEMY_SHOT) {
            Object otherType = other.getUserData();
            if (otherType == FixtureType.PLAYER || otherType == FixtureType.SOLID) {
                handler.onEnemyShotHit(self.getBody(), otherType == FixtureType.PLAYER);
            }
        } else if (began && type == FixtureType.BULLET) {
            Object otherType = other.getUserData();
            if (otherType == FixtureType.COLLECTIBLE || otherType == FixtureType.SOLID
                    || otherType == FixtureType.ENEMY) {
                handler.onBulletHit(self.getBody(), other.getBody(), (FixtureType) otherType);
            }
        }
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
    }
}
