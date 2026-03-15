package     man.utils;

import java.awt.geom.Point2D;

public class MathUtils {

    // Angles

    // Unghi absolut (radiani) de la punctul 'from' la punctul 'to'
    //abs ang in rads from FROM to TO
    // 0 = Nord, PI/2 = Est, PI = Sud, 3PI/2 = Vest
    public static double absoluteBearing(Point2D.Double from, Point2D.Double to) {
        return Math.atan2(to.x - from.x, to.y - from.y);
    }

    // normalize in -pi pi
    public static double normalRelativeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    public static double normalAbsoluteAngle(double angle) {
        while (angle < 0) angle += 2 * Math.PI;
        while (angle >= 2 * Math.PI) angle -= 2 * Math.PI;
        return angle;
    }

    //Projections and Distances

    // dist -> angle
    public static Point2D.Double project(Point2D.Double origin,
                                         double angle,
                                         double dist) {
        return new Point2D.Double(
                origin.x + Math.sin(angle) * dist,
                origin.y + Math.cos(angle) * dist
        );
    }

    // Euclidian Distance
    public static double distance(Point2D.Double a, Point2D.Double b) {
        return a.distance(b);
    }

    // Lateral speed and GuessFactor

    public static double lateralVelocity(Point2D.Double targetPos,
                                         Point2D.Double referencePos,
                                         double targetHeading,
                                         double targetVelocity) {
        double angle = absoluteBearing(referencePos, targetPos);
        return targetVelocity * Math.sin(targetHeading - angle);
    }

    //sign of lateral speed +1 clockwise or -1 counter
    public static int lateralSign(Point2D.Double targetPos,
                                  Point2D.Double referencePos,
                                  double targetHeading,
                                  double targetVelocity) {
        double lv = lateralVelocity(targetPos, referencePos,
                targetHeading, targetVelocity);
        return lv >= 0 ? 1 : -1;
    }

    // Robot movement

    //rotates relativ angle for going to goAngle, either backwards for forward, choosing the choice with minimal rotation
    public static double backAsFrontAngle(double goAngle, double currentHeading) {
        double angle = normalRelativeAngle(goAngle - currentHeading);
        if (Math.abs(angle) > Math.PI / 2) {
            return angle + (angle < 0 ? Math.PI : -Math.PI);
        }
        return angle;
    }

    //ret 1 if forward, -1 otherwise
    public static int moveDirection(double goAngle, double currentHeading) {
        double angle = normalRelativeAngle(goAngle - currentHeading);
        return Math.abs(angle) > Math.PI / 2 ? -1 : 1;
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double square(double x) {
        return x * x;
    }

    //liniar interpolation
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}