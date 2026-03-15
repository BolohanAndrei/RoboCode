package man.gun;

import robocode.AdvancedRobot;
import man.utils.*;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;


public class GuessFactorTargeting {

    private static final int    GF_BINS           = 31;
    private static final int    MIN_DATA_FOR_GF   = 5;
    private static final double MAX_FIRE_DISTANCE = 800.0;

    private final AdvancedRobot robot;

    private final List<Wave> activeWaves;

    private int dataCount;

    public GuessFactorTargeting(AdvancedRobot robot) {
        this.robot       = robot;
        this.activeWaves = new ArrayList<>();
        this.dataCount   = 0;
    }


    public void aim(Point2D.Double myPosition,
                    EnemyData enemy,
                    double bulletPower) {

        if (enemy == null) return;

        double         gunHeading = robot.getGunHeadingRadians();
        Point2D.Double targetPos;

        if (dataCount >= MIN_DATA_FOR_GF) {
            targetPos = predictPositionGF(myPosition, enemy, bulletPower);
        } else {
            targetPos = predictPositionLinear(myPosition, enemy, bulletPower);
        }

        double aimAngle = MathUtils.absoluteBearing(myPosition, targetPos);
        double gunTurn  = MathUtils.normalRelativeAngle(aimAngle - gunHeading);
        robot.setTurnGunRightRadians(gunTurn);
    }


    public void fire(Point2D.Double myPosition,
                     EnemyData enemy,
                     double bulletPower) {

        if (enemy == null) return;
        if (robot.getGunHeat() > 0) return;
        if (enemy.distance > MAX_FIRE_DISTANCE) return;

        double gunHeading   = robot.getGunHeadingRadians();
        double enemyBearing = MathUtils.absoluteBearing(myPosition, enemy.getLocation());
        double gunError     = Math.abs(MathUtils.normalRelativeAngle(
                gunHeading - enemyBearing));
        double tolerance    = Math.atan2(18.0, enemy.distance);

        if (gunError <= tolerance) {
            Wave wave = createWave(myPosition, enemy, bulletPower, robot.getTime());
            activeWaves.add(wave);
            robot.setFireBullet(bulletPower);
        }
    }


    public void onBulletHit(Point2D.Double enemyPosition,
                            EnemyData enemy) {
        if (activeWaves.isEmpty() || enemy == null) return;

        Wave closest = null;
        double minDist = Double.POSITIVE_INFINITY;

        for (Wave wave : activeWaves) {
            double dist = wave.fireLocation.distance(enemyPosition);
            double waveRadius = wave.distanceTraveled(robot.getTime());
            double diff = Math.abs(dist - waveRadius);
            if (diff < minDist) {
                minDist  = diff;
                closest  = wave;
            }
        }

        if (closest != null) {
            double gf = closest.guessFactorForPosition(enemyPosition);
            enemy.logGuessFactorHit(gf);
            dataCount++;
            activeWaves.remove(closest);
        }
    }

    public void onBulletMissed() {
        if (!activeWaves.isEmpty()) {
            activeWaves.remove(0);
        }
    }


    private Point2D.Double predictPositionGF(Point2D.Double myPosition,
                                             EnemyData enemy,
                                             double bulletPower) {
        int    bestBin        = enemy.bestGuessFactorBin();
        double gf             = enemy.binToGuessFactor(bestBin);
        double absoluteBearing = MathUtils.absoluteBearing(myPosition, enemy.getLocation());
        double mea            = MaxEscapeAngle.calculate(bulletPower);
        double aimOffset      = gf * mea * enemy.lateralSign;
        double aimAngle       = absoluteBearing + aimOffset;
        return MathUtils.project(myPosition, aimAngle, enemy.distance);
    }


    private Point2D.Double predictPositionLinear(Point2D.Double myPosition,
                                                 EnemyData enemy,
                                                 double bulletPower) {
        double         bulletSpeed   = MaxEscapeAngle.bulletSpeed(bulletPower);
        Point2D.Double predictedPos  = enemy.getLocation();
        double         enemyHeading  = enemy.heading;
        double         enemyVelocity = enemy.velocity;

        for (int i = 0; i < 100; i++) {
            predictedPos = new Point2D.Double(
                    predictedPos.x + Math.sin(enemyHeading) * enemyVelocity,
                    predictedPos.y + Math.cos(enemyHeading) * enemyVelocity);

            double distToPos    = myPosition.distance(predictedPos);
            double bulletDistAt = bulletSpeed * (i + 1);

            if (bulletDistAt >= distToPos) break;
        }
        return predictedPos;
    }


    private Wave createWave(Point2D.Double myPosition,
                            EnemyData enemy,
                            double bulletPower,
                            long time) {
        return new Wave(
                myPosition,
                time,
                bulletPower,
                enemy.getLocation(),
                enemy.heading,
                enemy.velocity,
                enemy.lateralVelocity,
                enemy.lateralSign
        );
    }


    public void init() {
        activeWaves.clear();
    }

    public void fullReset() {
        activeWaves.clear();
        dataCount = 0;
    }


    public int getDataCount() { return dataCount; }

    public boolean hasEnoughData() { return dataCount >= MIN_DATA_FOR_GF; }
}