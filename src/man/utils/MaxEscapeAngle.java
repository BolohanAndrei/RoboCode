package man.utils;

import java.awt.geom.Point2D;


public class MaxEscapeAngle {

    private static final double MAX_ROBOT_VELOCITY = 8.0;


    public static double calculate(double bulletPower) {
        double bulletSpeed = bulletSpeed(bulletPower);
        return Math.asin(MAX_ROBOT_VELOCITY / bulletSpeed);
    }


    public static double calculateFromSpeed(double bulletSpeed) {
        if (bulletSpeed <= MAX_ROBOT_VELOCITY) {
            return Math.PI / 2; // limită teoretică
        }
        return Math.asin(MAX_ROBOT_VELOCITY / bulletSpeed);
    }


    public static double calculateEffective(Point2D.Double targetPos,
                                            Point2D.Double shooterPos,
                                            double bulletPower,
                                            BattleField battleField) {
        double mea           = calculate(bulletPower);
        double bulletSpeed   = bulletSpeed(bulletPower);
        double distance      = targetPos.distance(shooterPos);

        double flightTime = distance / bulletSpeed;

        double maxLateralDist = MAX_ROBOT_VELOCITY * flightTime;

        double wallDist = battleField.distanceToWall(targetPos);

        if (wallDist < maxLateralDist) {
            double wallConstrainedMEA = Math.atan2(wallDist, distance);
            mea = Math.min(mea, wallConstrainedMEA);
        }

        return mea;
    }


    public static double gfToAngleOffset(double guessFactor,
                                         double bulletPower,
                                         int lateralSign) {
        double mea = calculate(bulletPower);
        return guessFactor * mea * lateralSign;
    }


    public static double angleOffsetToGF(double angleOffset,
                                         double bulletPower,
                                         int lateralSign) {
        double mea = calculate(bulletPower);
        if (mea == 0) return 0;
        double gf = angleOffset / mea / lateralSign;
        return MathUtils.clamp(gf, -1.0, 1.0);
    }


    public static double bulletSpeed(double bulletPower) {
        return 20.0 - 3.0 * bulletPower;
    }


    public static double bulletDamage(double bulletPower) {
        double damage = 4.0 * bulletPower;
        if (bulletPower > 1.0) {
            damage += 2.0 * (bulletPower - 1.0);
        }
        return damage;
    }


    public static double energyRegained(double bulletPower) {
        return 3.0 * bulletPower;
    }


    public static double gunHeatGenerated(double bulletPower) {
        return 1.0 + bulletPower / 5.0;
    }


    public static double minTimeBetweenShots(double bulletPower,
                                             double gunCoolingRate) {
        return gunHeatGenerated(bulletPower) / gunCoolingRate;
    }
}