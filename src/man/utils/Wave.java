package man.utils;

import java.awt.geom.Point2D;


//represents a flying bullet
public class Wave {

    public final Point2D.Double fireLocation; // from where it was shot
    public final long fireTime; // tick when was shot
    public final double bulletPower;
    public final double bulletSpeed;

    public final Point2D.Double targetLocation; // where was the target
    public final double targetHeading;   // direction of the target
    public final double targetVelocity;  // speed of the target
    public final double targetLateralVelocity; // lateral spped
    public final int targetLateralSign;// +1 sau -1

    //MEA
    public final double maxEscapeAngle;

    //abs angle from fire location to target location
    public final double absoluteBearingAtFire;

    // Dinamic state
    public boolean passed;

    // GuessFactor
    public double hitGuessFactor;

    // Constructor
    public Wave(Point2D.Double fireLocation,
                long fireTime,
                double bulletPower,
                Point2D.Double targetLocation,
                double targetHeading,
                double targetVelocity,
                double targetLateralVelocity,
                int targetLateralSign) {

        this.fireLocation = fireLocation;
        this.fireTime = fireTime;
        this.bulletPower = bulletPower;
        this.bulletSpeed = 20.0 - 3.0 * bulletPower;
        this.targetLocation = targetLocation;
        this.targetHeading = targetHeading;
        this.targetVelocity = targetVelocity;
        this.targetLateralVelocity = targetLateralVelocity;
        this.targetLateralSign = targetLateralSign;
        this.passed = false;
        this.hitGuessFactor = 0;

        this.absoluteBearingAtFire = MathUtils.absoluteBearing(
                fireLocation, targetLocation);
        this.maxEscapeAngle = Math.asin(8.0 / this.bulletSpeed);
    }

    // Utility Methods

    // Distance made by wave until currentTime
    public double distanceTraveled(long currentTime) {
        return (currentTime - fireTime) * bulletSpeed;
    }

    // wave radiu
    public double radius(long currentTime) {
        return distanceTraveled(currentTime);
    }

    public boolean hasPassed(Point2D.Double point, long currentTime) {
        double dist = fireLocation.distance(point);
        return radius(currentTime) > dist + 50; // +50 safe margin
    }

    public boolean hasReached(Point2D.Double point, long currentTime) {
        double dist= fireLocation.distance(point);
        double prevDist=radius(currentTime - 1);
        double currDist=radius(currentTime);
        return prevDist <= dist && currDist >= dist;
    }

    public double guessFactorForPosition(Point2D.Double position) {
        double bearing = MathUtils.absoluteBearing(fireLocation, position);
        double offset = MathUtils.normalRelativeAngle(
                bearing - absoluteBearingAtFire);
        if (maxEscapeAngle == 0) return 0;
        double gf = offset / maxEscapeAngle * targetLateralSign;
        return MathUtils.clamp(gf, -1.0, 1.0);
    }

    // abs angle
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