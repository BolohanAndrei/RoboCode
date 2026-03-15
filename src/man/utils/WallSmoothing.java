package man.utils;

import java.awt.geom.Point2D;


public class WallSmoothing {

    private static final double WALL_STICK = 160.0; // distanța de "sondare"
    private static final double STEP       = 0.05;  // pas de rotație (radiani)
    private static final int    MAX_ITER   = 100;   // iterații maxime

    private final BattleField battleField;


    public WallSmoothing(BattleField battleField) {
        this.battleField = battleField;
    }



    public double smooth(Point2D.Double position,
                         double angle,
                         int direction) {

        int iter = 0;
        while (!isSafe(position, angle) && iter < MAX_ITER) {
            angle += direction * STEP;
            iter++;
        }
        return angle;
    }


    public double smoothAuto(Point2D.Double position, double angle) {
        if (isSafe(position, angle)) return angle;

        double angleLeft  = angle;
        double angleRight = angle;
        int iterLeft  = 0;
        int iterRight = 0;

        // Căutăm în ambele direcții simultan
        while (iterLeft < MAX_ITER && iterRight < MAX_ITER) {
            angleLeft  -= STEP;
            angleRight += STEP;
            iterLeft++;
            iterRight++;

            if (isSafe(position, angleLeft))  return angleLeft;
            if (isSafe(position, angleRight)) return angleRight;
        }

        return angle; // fallback
    }


    public boolean isSafe(Point2D.Double position, double angle) {
        Point2D.Double projected = MathUtils.project(
                position, angle, WALL_STICK);
        return battleField.containsSafe(projected);
    }


    public boolean isSafeCustom(Point2D.Double position,
                                double angle,
                                double wallStick) {
        Point2D.Double projected = MathUtils.project(
                position, angle, wallStick);
        return battleField.containsSafe(projected);
    }


    public double smoothCustom(Point2D.Double position,
                               double angle,
                               int direction,
                               double wallStick) {
        int iter = 0;
        while (!isSafeCustom(position, angle, wallStick) && iter < MAX_ITER) {
            angle += direction * STEP;
            iter++;
        }
        return angle;
    }


    public double distanceToWallInDirection(Point2D.Double position,
                                            double angle) {
        double dist = 0;
        double step = 10.0;
        Point2D.Double probe = position;

        while (battleField.contains(probe) && dist < 1200) {
            dist += step;
            probe = MathUtils.project(position, angle, dist);
        }
        return dist;
    }

    public double getWallStick() {
        return WALL_STICK;
    }
}