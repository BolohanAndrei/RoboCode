package man.utils;

import java.awt.geom.Point2D;


public class Wave {

    public final Point2D.Double fireLocation;
    public final long           fireTime;
    public final double         bulletPower;
    public final double         bulletSpeed;


    public final Point2D.Double targetLocation;
    public final double         targetHeading;
    public final double         targetVelocity;
    public final double         targetLateralVelocity;
    public final int            targetLateralSign;

    public final double maxEscapeAngle;

    public final double absoluteBearingAtFire;


    public boolean passed;

    public double hitGuessFactor;

    public Wave(Point2D.Double fireLocation,
                long           fireTime,
                double         bulletPower,
                Point2D.Double targetLocation,
                double         targetHeading,
                double         targetVelocity,
                double         targetLateralVelocity,
                int            targetLateralSign) {

        this.fireLocation          = fireLocation;
        this.fireTime              = fireTime;
        this.bulletPower           = bulletPower;
        this.bulletSpeed           = 20.0 - 3.0 * bulletPower;
        this.targetLocation        = targetLocation;
        this.targetHeading         = targetHeading;
        this.targetVelocity        = targetVelocity;
        this.targetLateralVelocity = targetLateralVelocity;
        this.targetLateralSign     = targetLateralSign;
        this.passed                = false;
        this.hitGuessFactor        = 0;

        this.absoluteBearingAtFire = MathUtils.absoluteBearing(
                fireLocation, targetLocation);


        //robot speed/bullet speed
        this.maxEscapeAngle = Math.asin(8.0 / this.bulletSpeed);
    }


    public double distanceTraveled(long currentTime) {
        return (currentTime - fireTime) * bulletSpeed;
    }

    public double radius(long currentTime) {
        return distanceTraveled(currentTime);
    }

    public boolean hasPassed(Point2D.Double point, long currentTime) {
        double dist = fireLocation.distance(point);
        return radius(currentTime) > dist + 50;
    }

    public boolean hasReached(Point2D.Double point, long currentTime) {
        double dist     = fireLocation.distance(point);
        double prevDist = radius(currentTime - 1);
        double currDist = radius(currentTime);
        return prevDist <= dist && currDist >= dist;
    }

    public double guessFactorForPosition(Point2D.Double position) {
        double bearing = MathUtils.absoluteBearing(fireLocation, position);
        double offset  = MathUtils.normalRelativeAngle(
                bearing - absoluteBearingAtFire);
        if (maxEscapeAngle == 0) return 0;
        double gf = offset / maxEscapeAngle * targetLateralSign;
        return MathUtils.clamp(gf, -1.0, 1.0);
    }

    public double angleForGuessFactor(double gf) {
        return absoluteBearingAtFire
                + gf * maxEscapeAngle * targetLateralSign;
    }

    @Override
    public String toString() {
        return String.format(
                "Wave[t=%d, power=%.2f, speed=%.2f, MEA=%.4f]",
                fireTime, bulletPower, bulletSpeed, maxEscapeAngle);
    }
}