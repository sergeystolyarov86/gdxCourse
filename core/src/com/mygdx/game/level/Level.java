package com.mygdx.game.level;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.mygdx.game.entities.Bullet;
import com.mygdx.game.entities.Drone;
import com.mygdx.game.entities.Enemy;
import com.mygdx.game.entities.EnemyContext;
import com.mygdx.game.entities.EnemyShot;
import com.mygdx.game.entities.Hero;
import com.mygdx.game.entities.Jellyfish;
import com.mygdx.game.entities.Octopus;
import com.mygdx.game.entities.Starfish;
import com.mygdx.game.graphics.EnemyArt;
import com.mygdx.game.graphics.Explosion;
import com.mygdx.game.graphics.SpriteAnimation;
import com.mygdx.game.physics.ContactHandler;
import com.mygdx.game.physics.FixtureType;
import com.mygdx.game.physics.PhysicsWorld;

/**
 * Игровой уровень: карта Tiled, физический мир и сущности на нём.
 * Не знает ни про ввод, ни про звук — наружу сообщает только события через {@link Listener}.
 */
public final class Level implements ContactHandler, EnemyContext, Disposable {

    public interface Listener {
        void onJellyfishCollected(int collected, int total);

        void onJellyfishShot(int shot, int total);

        void onEnemyKilled(int kills);

        void onEnemyFired();

        void onExplosion();

        void onHeroHurt(int health);

        void onHeroDied();

        void onLevelComplete();
    }

    private static final String TILE_LAYER = "Слой тайлов 1";
    private static final String SETTING_LAYER = "сеттинг";
    private static final String OBJECTS_LAYER = "объекты";

    private static final String HERO_OBJECT = "hero";
    private static final String JELLYFISH_OBJECT = "jellyfish";
    private static final String OCTOPUS_OBJECT = "octopus";
    private static final String STARFISH_OBJECT = "starfish";
    private static final String DRONE_OBJECT = "drone";
    private static final String FINISH_OBJECT = "finish";
    private static final String BODY_TYPE_PROPERTY = "BodyType";

    private static final String HERO_ATLAS = "atlas/bob.atlas";
    private static final String HERO_RUN_REGION = "bobRun";
    private static final float HERO_FRAME_DURATION = 0.1f;
    private static final float HERO_FOOT_WIDTH_RATIO = 0.8f;
    private static final float HERO_FOOT_HEIGHT_PX = 4f;
    private static final float SOLID_FRICTION = 0.5f;

    private static final String HERO_WEAPON = "weapons/ak.png";
    /** Скорость пули, м/с. */
    private static final float BULLET_SPEED = 10f;

    /** Враг, расставленный на карте, оживает, когда герой подходит ближе этого расстояния. */
    private static final float ACTIVATION_DISTANCE_PX = 640f;

    /** Подкрепления: осьминоги, которые подходят то спереди, то сзади. */
    private static final float REINFORCEMENT_INTERVAL = 7f;
    private static final float FIRST_REINFORCEMENT_DELAY = 6f;
    private static final int MAX_ALIVE_FOR_REINFORCEMENT = 5;
    private static final float SPAWN_DISTANCE_PX = 620f;
    private static final float SPAWN_HEIGHT_PX = 20f;
    private static final float SPAWN_EDGE_MARGIN_PX = 30f;

    private static final float HARPOON_SPEED = 5f;
    private static final float BOMB_BLAST_RADIUS_PX = 50f;
    private static final float ENEMY_EXPLOSION_SCALE = 1.2f;
    private static final float BOMB_EXPLOSION_SCALE = 1f;

    /** Враг, ожидающий, пока герой подойдёт. */
    private static final class SpawnPoint {
        final String type;
        final float x;
        final float y;

        SpawnPoint(String type, float x, float y) {
            this.type = type;
            this.x = x;
            this.y = y;
        }
    }

    private final TiledMap map;
    private final TiledMapTileLayer tileLayer;
    private final PhysicsWorld physics;
    private final Listener listener;
    private final Hero hero;
    private final EnemyArt art = new EnemyArt();

    private final Array<Jellyfish> pendingCollection = new Array<>();
    private final Array<Jellyfish> pendingShot = new Array<>();
    private final Array<Bullet> bullets = new Array<>();
    private final Array<Enemy> enemies = new Array<>();
    private final Array<EnemyShot> enemyShots = new Array<>();
    private final Array<Explosion> explosions = new Array<>();
    private final Array<SpawnPoint> spawnPoints = new Array<>();
    /** Враги, которые сейчас касаются героя. */
    private final Array<Enemy> touchingHero = new Array<>();
    /** Стены и полы — чтобы не создавать врага внутри них. */
    private final Array<Rectangle> solids = new Array<>();

    private final Vector2 muzzle = new Vector2();
    private final Vector2 heroPosition = new Vector2();
    private final Vector2 point = new Vector2();
    private final Rectangle spawnBounds = new Rectangle();

    private Rectangle finishZone;
    private boolean finished;
    private float levelLeft = Float.MAX_VALUE;
    private float levelRight = -Float.MAX_VALUE;
    private float reinforcementTimer = FIRST_REINFORCEMENT_DELAY;
    private boolean reinforcementOnRight;
    private int jellyfishTotal;
    private int jellyfishCollected;
    private int jellyfishShot;
    private int kills;

    public Level(String mapPath, Listener listener) {
        this.listener = listener;
        map = new TmxMapLoader().load(mapPath);
        tileLayer = (TiledMapTileLayer) map.getLayers().get(TILE_LAYER);
        physics = new PhysicsWorld(this);
        hero = createHero(requireLayer(SETTING_LAYER));
        createObjects(requireLayer(OBJECTS_LAYER));
    }

    public void update(float delta) {
        physics.update(delta);
        hero.update(delta);

        for (Bullet bullet : bullets) {
            if (bullet.update(delta)) bullet.markDead();
        }
        for (Enemy enemy : enemies) {
            enemy.update(delta, this);
        }
        for (EnemyShot shot : enemyShots) {
            shot.update(delta);
        }
        for (int i = explosions.size - 1; i >= 0; i--) {
            if (explosions.get(i).update(delta)) explosions.removeIndex(i);
        }

        hurtHeroIfTouched();
        flushCollected();
        flushBullets();
        flushEnemies();
        flushEnemyShots();
        activateSpawnPoints();
        updateReinforcements(delta);
        checkFinish();
    }

    /** @return {@code true}, если герой выстрелил (для звука) */
    public boolean fire() {
        if (!hero.tryFire()) return false;
        hero.getMuzzlePx(muzzle);
        hero.getAimDirection(point).scl(BULLET_SPEED);
        Body body = physics.createBullet(muzzle.x, muzzle.y, Bullet.WIDTH_PX, Bullet.HEIGHT_PX,
                point.x, point.y);
        bullets.add(new Bullet(body));
        return true;
    }

    /** Рисует все сущности уровня поверх карты. */
    public void draw(SpriteBatch batch, Texture bulletTexture) {
        for (Enemy enemy : enemies) enemy.draw(batch);
        for (EnemyShot shot : enemyShots) shot.draw(batch);
        hero.draw(batch);
        for (Bullet bullet : bullets) bullet.draw(batch, bulletTexture);
        for (Explosion explosion : explosions) explosion.draw(batch);
    }

    public TiledMap getMap() {
        return map;
    }

    public Hero getHero() {
        return hero;
    }

    public PhysicsWorld getPhysics() {
        return physics;
    }

    public int getKills() {
        return kills;
    }

    // ---- EnemyContext ----

    @Override
    public Vector2 getHeroPositionPx(Vector2 out) {
        return hero.getPositionPx(out);
    }

    @Override
    public void fireHarpoon(float xPx, float yPx, int direction) {
        EnemyShot.Kind kind = EnemyShot.Kind.HARPOON;
        Body body = physics.createProjectile(xPx, yPx, kind.widthPx, kind.heightPx,
                direction * HARPOON_SPEED, 0f, 0f, FixtureType.ENEMY_SHOT);
        enemyShots.add(new EnemyShot(body, kind, art.harpoon));
        listener.onEnemyFired();
    }

    @Override
    public void dropBomb(float xPx, float yPx) {
        EnemyShot.Kind kind = EnemyShot.Kind.BOMB;
        Body body = physics.createProjectile(xPx, yPx, kind.widthPx, kind.heightPx,
                0f, 0f, 1f, FixtureType.ENEMY_SHOT);
        enemyShots.add(new EnemyShot(body, kind, art.bomb));
    }

    // ---- ContactHandler (вызывается во время шага мира: только помечаем, удаляем позже) ----

    @Override
    public void onGroundContact(Body owner, boolean began) {
        if (owner.getUserData() instanceof Hero) {
            ((Hero) owner.getUserData()).onGroundContact(began);
        }
    }

    @Override
    public void onCollectibleTouched(Body collectible) {
        Object owner = collectible.getUserData();
        if (owner instanceof Jellyfish && ((Jellyfish) owner).markCollected()) {
            pendingCollection.add((Jellyfish) owner);
        }
    }

    @Override
    public void onBulletHit(Body bulletBody, Body target, FixtureType targetType) {
        Object bullet = bulletBody.getUserData();
        if (!(bullet instanceof Bullet) || ((Bullet) bullet).isDead()) return;

        Object owner = target.getUserData();
        if (targetType == FixtureType.COLLECTIBLE) {
            // Уже пойманная/подстреленная медуза пулю не останавливает.
            if (!(owner instanceof Jellyfish) || !((Jellyfish) owner).markCollected()) return;
            pendingShot.add((Jellyfish) owner);
        } else if (targetType == FixtureType.ENEMY) {
            // Убитый (ещё не удалённый) враг пулю не останавливает.
            if (!(owner instanceof Enemy) || ((Enemy) owner).isDead()) return;
            ((Enemy) owner).hit();
        }
        ((Bullet) bullet).markDead();
    }

    @Override
    public void onEnemyContact(Body enemyBody, boolean began) {
        Object owner = enemyBody.getUserData();
        if (!(owner instanceof Enemy)) return;
        if (began) {
            touchingHero.add((Enemy) owner);
        } else {
            touchingHero.removeValue((Enemy) owner, true);
        }
    }

    @Override
    public void onEnemyShotHit(Body shotBody, boolean hitPlayer) {
        Object shot = shotBody.getUserData();
        if (shot instanceof EnemyShot) {
            ((EnemyShot) shot).onHit(hitPlayer);
        }
    }

    // ---- урон герою ----

    private void hurtHeroIfTouched() {
        if (touchingHero.size == 0) return;
        damageHero(touchingHero.first().getBody().getPosition().x);
    }

    private void damageHero(float fromXMeters) {
        if (!hero.isAlive() || finished) return;
        if (!hero.hurt(fromXMeters)) return; // ещё неуязвим

        listener.onHeroHurt(hero.getHealth());
        if (!hero.isAlive()) listener.onHeroDied();
    }

    // ---- удаление после шага мира ----

    private void flushCollected() {
        for (Jellyfish jellyfish : pendingCollection) {
            removeJellyfish(jellyfish);
            jellyfishCollected++;
            listener.onJellyfishCollected(jellyfishCollected, jellyfishTotal);
        }
        pendingCollection.clear();

        for (Jellyfish jellyfish : pendingShot) {
            removeJellyfish(jellyfish);
            jellyfishShot++;
            listener.onJellyfishShot(jellyfishShot, jellyfishTotal);
        }
        pendingShot.clear();
    }

    private void removeJellyfish(Jellyfish jellyfish) {
        physics.destroyBody(jellyfish.getBody());
        GridPoint2 cell = jellyfish.getTileCell();
        if (cell != null) {
            tileLayer.setCell(cell.x, cell.y, null);
        }
    }

    private void flushBullets() {
        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            if (bullet.isDead()) {
                physics.destroyBody(bullet.getBody());
                bullets.removeIndex(i);
            }
        }
    }

    private void flushEnemies() {
        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            if (!enemy.isDead()) continue;

            enemy.getCenterPx(point);
            explosions.add(new Explosion(art.explosion, point.x, point.y, ENEMY_EXPLOSION_SCALE));
            touchingHero.removeValue(enemy, true);
            physics.destroyBody(enemy.getBody());
            enemies.removeIndex(i);
            kills++;
            listener.onEnemyKilled(kills);
        }
    }

    private void flushEnemyShots() {
        for (int i = enemyShots.size - 1; i >= 0; i--) {
            EnemyShot shot = enemyShots.get(i);
            if (!shot.isDead()) continue;

            shot.getCenterPx(point);
            if (shot.getKind() == EnemyShot.Kind.BOMB) {
                explodeBomb(point.x, point.y);
            } else if (shot.hasHitPlayer()) {
                damageHero(shot.getBody().getPosition().x);
            }
            physics.destroyBody(shot.getBody());
            enemyShots.removeIndex(i);
        }
    }

    private void explodeBomb(float x, float y) {
        explosions.add(new Explosion(art.explosion, x, y, BOMB_EXPLOSION_SCALE));
        listener.onExplosion();
        hero.getPositionPx(heroPosition);
        if (heroPosition.dst(x, y) < BOMB_BLAST_RADIUS_PX) {
            damageHero(PhysicsWorld.toMeters(x));
        }
    }

    // ---- появление врагов ----

    private void activateSpawnPoints() {
        hero.getPositionPx(heroPosition);
        for (int i = spawnPoints.size - 1; i >= 0; i--) {
            SpawnPoint spawn = spawnPoints.get(i);
            if (Math.abs(spawn.x - heroPosition.x) < ACTIVATION_DISTANCE_PX) {
                spawnEnemy(spawn.type, spawn.x, spawn.y);
                spawnPoints.removeIndex(i);
            }
        }
    }

    private void updateReinforcements(float delta) {
        if (finished || !hero.isAlive()) return;
        reinforcementTimer -= delta;
        if (reinforcementTimer > 0f || enemies.size >= MAX_ALIVE_FOR_REINFORCEMENT) return;
        reinforcementTimer = REINFORCEMENT_INTERVAL;

        // Чередуем стороны; если сторона занята стеной или за краем уровня — пробуем другую.
        reinforcementOnRight = !reinforcementOnRight;
        if (!trySpawnReinforcement(reinforcementOnRight)) trySpawnReinforcement(!reinforcementOnRight);
    }

    private boolean trySpawnReinforcement(boolean right) {
        hero.getPositionPx(heroPosition);
        float x = heroPosition.x + (right ? SPAWN_DISTANCE_PX : -SPAWN_DISTANCE_PX);
        float bottom = heroPosition.y + SPAWN_HEIGHT_PX - Octopus.HEIGHT_PX / 2f;
        if (x < levelLeft + SPAWN_EDGE_MARGIN_PX || x > levelRight - SPAWN_EDGE_MARGIN_PX) return false;

        spawnBounds.set(x - Octopus.WIDTH_PX / 2f, bottom, Octopus.WIDTH_PX, Octopus.HEIGHT_PX);
        for (Rectangle solid : solids) {
            if (solid.overlaps(spawnBounds)) return false;
        }
        spawnEnemy(OCTOPUS_OBJECT, x, bottom);
        return true;
    }

    /** @param y низ тела для наземных врагов, центр — для дронов */
    private void spawnEnemy(String type, float x, float y) {
        if (DRONE_OBJECT.equals(type)) {
            spawnBounds.set(x - Drone.WIDTH_PX / 2f, y - Drone.HEIGHT_PX / 2f, Drone.WIDTH_PX, Drone.HEIGHT_PX);
            // Сенсор: дрон пролетает сквозь стены, но пули и герой его задевают.
            Body body = physics.createBox(BodyDef.BodyType.DynamicBody, spawnBounds, FixtureType.ENEMY, true, 0f);
            enemies.add(new Drone(body, art));
        } else if (STARFISH_OBJECT.equals(type)) {
            spawnBounds.set(x - Starfish.WIDTH_PX / 2f, y, Starfish.WIDTH_PX, Starfish.HEIGHT_PX);
            Body body = physics.createBox(BodyDef.BodyType.DynamicBody, spawnBounds, FixtureType.ENEMY, false, 0f);
            enemies.add(new Starfish(body, art));
        } else {
            spawnBounds.set(x - Octopus.WIDTH_PX / 2f, y, Octopus.WIDTH_PX, Octopus.HEIGHT_PX);
            Body body = physics.createBox(BodyDef.BodyType.DynamicBody, spawnBounds, FixtureType.ENEMY, false, 0f);
            enemies.add(new Octopus(body, art));
        }
    }

    private void checkFinish() {
        if (finished || finishZone == null || !hero.isAlive()) return;
        hero.getPositionPx(heroPosition);
        if (heroPosition.x >= finishZone.x) {
            finished = true;
            listener.onLevelComplete();
        }
    }

    // ---- построение уровня ----

    private Hero createHero(MapLayer layer) {
        MapObject object = layer.getObjects().get(HERO_OBJECT);
        if (!(object instanceof RectangleMapObject)) {
            throw new IllegalStateException("Layer '" + layer.getName() + "' has no rectangle '" + HERO_OBJECT + "'");
        }
        Rectangle bounds = worldBounds((RectangleMapObject) object, layer);

        Body body = physics.createBox(BodyDef.BodyType.DynamicBody, bounds, FixtureType.PLAYER, false, 0f);
        physics.addSensor(body, bounds.width * HERO_FOOT_WIDTH_RATIO, HERO_FOOT_HEIGHT_PX,
                0f, -bounds.height / 2f, FixtureType.PLAYER_FOOT);

        SpriteAnimation run = SpriteAnimation.fromAtlas(HERO_ATLAS, HERO_RUN_REGION,
                HERO_FRAME_DURATION, Animation.PlayMode.LOOP);
        return new Hero(body, bounds.height, run, new Texture(HERO_WEAPON));
    }

    private void createObjects(MapLayer layer) {
        for (RectangleMapObject object : layer.getObjects().getByType(RectangleMapObject.class)) {
            Rectangle bounds = worldBounds(object, layer);
            String name = object.getName();

            if (JELLYFISH_OBJECT.equals(name)) {
                Body body = physics.createBox(BodyDef.BodyType.StaticBody, bounds, FixtureType.COLLECTIBLE, true, 0f);
                new Jellyfish(body, findTileCell(object.getRectangle()));
                jellyfishTotal++;
            } else if (OCTOPUS_OBJECT.equals(name) || STARFISH_OBJECT.equals(name) || DRONE_OBJECT.equals(name)) {
                // Наземные стоят низом на точке объекта, дрон висит центром в ней.
                float y = DRONE_OBJECT.equals(name) ? bounds.y + bounds.height / 2f : bounds.y;
                spawnPoints.add(new SpawnPoint(name, bounds.x + bounds.width / 2f, y));
            } else if (FINISH_OBJECT.equals(name)) {
                finishZone = bounds;
            } else {
                BodyDef.BodyType bodyType = parseBodyType(object.getProperties().get(BODY_TYPE_PROPERTY, String.class));
                physics.createBox(bodyType, bounds, FixtureType.SOLID, false, SOLID_FRICTION);
                solids.add(bounds);
                levelLeft = Math.min(levelLeft, bounds.x);
                levelRight = Math.max(levelRight, bounds.x + bounds.width);
            }
        }
    }

    /** Прямоугольник объекта с учётом смещения слоя — как его видно в редакторе Tiled. */
    private static Rectangle worldBounds(RectangleMapObject object, MapLayer layer) {
        Rectangle rect = new Rectangle(object.getRectangle());
        rect.x += layer.getRenderOffsetX();
        rect.y -= layer.getRenderOffsetY(); // в Tiled ось Y направлена вниз
        return rect;
    }

    private static BodyDef.BodyType parseBodyType(String value) {
        if ("DynamicBody".equals(value)) return BodyDef.BodyType.DynamicBody;
        if ("KinematicBody".equals(value)) return BodyDef.BodyType.KinematicBody;
        return BodyDef.BodyType.StaticBody;
    }

    /**
     * Ищет анимированную клетку тайлового слоя (медузы анимированы), изображение которой
     * ближе всего к центру объекта и накрывает его. Декорации не анимированы и не мешают поиску.
     */
    private GridPoint2 findTileCell(Rectangle objectBounds) {
        if (tileLayer == null) return null;

        float targetX = objectBounds.x + objectBounds.width / 2f;
        float targetY = objectBounds.y + objectBounds.height / 2f;
        float cellWidth = tileLayer.getTileWidth();
        float cellHeight = tileLayer.getTileHeight();

        GridPoint2 best = null;
        float bestDistance = Float.MAX_VALUE;
        for (int x = 0; x < tileLayer.getWidth(); x++) {
            for (int y = 0; y < tileLayer.getHeight(); y++) {
                TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                if (cell == null || !(cell.getTile() instanceof AnimatedTiledMapTile)) continue;

                TextureRegion region = cell.getTile().getTextureRegion();
                Rectangle image = new Rectangle(x * cellWidth, y * cellHeight,
                        region.getRegionWidth(), region.getRegionHeight());
                if (!image.contains(targetX, targetY)) continue;

                float dx = image.x + image.width / 2f - targetX;
                float dy = image.y + image.height / 2f - targetY;
                float distance = dx * dx + dy * dy;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new GridPoint2(x, y);
                }
            }
        }
        return best;
    }

    private MapLayer requireLayer(String name) {
        MapLayer layer = map.getLayers().get(name);
        if (layer == null) throw new IllegalStateException("Map has no layer '" + name + "'");
        return layer;
    }

    @Override
    public void dispose() {
        hero.dispose();
        art.dispose();
        physics.dispose();
        map.dispose();
    }
}
