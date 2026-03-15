package man.utils;

import java.awt.geom.Point2D;

public class WallSmoothing {

    private static final double WALL_STICK = 160.0; // sonding distance
    private static final double STEP = 0.05;  // rotation step(rads)
    private static final int MAX_ITER = 100;   // maximum iterations

    private final BattleField battleField;

    // Constructor
    public WallSmoothing(BattleField battleField) {
        this.battleField = battleField;
    }

    // Principal Method

    // Return an angle adjusted so that the projection from position to distance WALL_STICK in direction of returned angle to be in the interior of the arena
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

    //chooses automatically the shortest direction of adjusting
    public double smoothAuto(Point2D.Double position, double angle) {
        if (isSafe(position, angle)) return angle;

        double angleLeft= angle;
        double angleRight = angle;
        int iterLeft = 0;
        int iterRight = 0;

        //search in both directions at the same time
        while (iterLeft < MAX_ITER && iterRight < MAX_ITER) {
            angleLeft -= STEP;
            angleRight += STEP;
            iterLeft++;
            iterRight++;

            if (isSafe(position, angleLeft)) return angleLeft;
            if (isSafe(position, angleRight)) return angleRight;
        }
        return angle; //fallback
    }

    //Utility Methods

    //verify in projection to WALL_STICK is in the interior of the arena
    public boolean isSafe(Point2D.Double position, double angle) {
        Point2D.Double projected = MathUtils.project(
                position, angle, WALL_STICK);
        return battleField.containsSafe(projected);
    }


    //verify if the projection is safe
    public boolean isSafeCustom(Point2D.Double position,
                                double angle,
                                double wallStick) {
        Point2D.Double projected = MathUtils.project(
                position, angle, wallStick);
        return battleField.containsSafe(projected);
    }


    //smooth with custom WALL_STICK
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

    //return the distance to the closest wall in given direction
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