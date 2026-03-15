package man.utils;

import java.awt.geom.Point2D;


public class MaxEscapeAngle {

    // maximum speed
    private static final double MAX_ROBOT_VELOCITY = 8.0;

    // Base computing of Max Escape Angle(MEA)

    //mea for a given bullet power
    public static double calculate(double bulletPower) {
        double bulletSpeed = bulletSpeed(bulletPower);
        return Math.asin(MAX_ROBOT_VELOCITY / bulletSpeed);
    }

    //MEA for a given bullet spped
    public static double calculateFromSpeed(double bulletSpeed) {
        if (bulletSpeed <= MAX_ROBOT_VELOCITY) {
            return Math.PI / 2; // limită teoretică
        }
        return Math.asin(MAX_ROBOT_VELOCITY / bulletSpeed);
    }

    //MEA taking into account the distance to the walls

    public static double calculateEffective(Point2D.Double targetPos,
                                            Point2D.Double shooterPos,
                                            double bulletPower,
                                            BattleField battleField) {
        double mea = calculate(bulletPower);
        double bulletSpeed = bulletSpeed(bulletPower);
        double distance = targetPos.distance(shooterPos);

        // fly time of bullet
        double flightTime = distance / bulletSpeed;

        // max lateral distance
        double maxLateralDist = MAX_ROBOT_VELOCITY * flightTime;

        // lateral distance to the walls
        double wallDist = battleField.distanceToWall(targetPos);

        // limit MEA if close to wall
        if (wallDist < maxLateralDist) {
            double wallConstrainedMEA = Math.atan2(wallDist, distance);
            mea = Math.min(mea, wallConstrainedMEA);
        }

        return mea;
    }

    // Convert GuessFactor <-> angle


    //convert a GuessFactor into a relativ angle
    public static double gfToAngleOffset(double guessFactor,
                                         double bulletPower,
                                         int lateralSign) {
        double mea = calculate(bulletPower);
        return guessFactor * mea * lateralSign;
    }

    //convert an angle relativ to GuessFactor
    public static double angleOffsetToGF(double angleOffset,
                                         double bulletPower,
                                         int lateralSign) {
        double mea = calculate(bulletPower);
        if (mea == 0) return 0;
        double gf = angleOffset / mea / lateralSign;
        return MathUtils.clamp(gf, -1.0, 1.0);
    }

    // Bullet Speed

    //bullet speed for a given power
    public static double bulletSpeed(double bulletPower) {
        return 20.0 - 3.0 * bulletPower;
    }

    //bullet damage for a given power
    public static double bulletDamage(double bulletPower) {
        double damage = 4.0 * bulletPower;
        if (bulletPower > 1.0) {
            damage += 2.0 * (bulletPower - 1.0);
        }
        return damage;
    }

    //energy regained when a bullet hits with a given power
    public static double energyRegained(double bulletPower) {
        return 3.0 * bulletPower;
    }


    //gun heat generated when shooting
    public static double gunHeatGenerated(double bulletPower) {
        return 1.0 + bulletPower / 5.0;
    }

    //min time between 2 shots
    public static double minTimeBetweenShots(double bulletPower,
                                             double gunCoolingRate) {
        return gunHeatGenerated(bulletPower) / gunCoolingRate;
    }
}