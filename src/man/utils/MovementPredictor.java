package man.utils;

import java.awt.geom.Point2D;


public class MovementPredictor {

    private static final double MAX_VELOCITY  = 8.0;
    private static final double ACCELERATION  = 1.0;
    private static final double DECELERATION  = 2.0;

    private final BattleField   battleField;
    private final WallSmoothing wallSmoothing;


    public MovementPredictor(BattleField battleField) {
        this.battleField  = battleField;
        this.wallSmoothing = new WallSmoothing(battleField);
    }


    public static class PredictedState {
        public Point2D.Double location;
        public double heading;
        public double velocity;
        public long   time;

        public PredictedState(Point2D.Double location,
                              double heading,
                              double velocity,
                              long time) {
            this.location = location;
            this.heading  = heading;
            this.velocity = velocity;
            this.time     = time;
        }

        @Override
        public String toString() {
            return String.format(
                    "PredictedState[t=%d, (%.1f,%.1f), h=%.3f, v=%.2f]",
                    time, location.x, location.y, heading, velocity);
        }
    }


    private double maxTurnRateRadians(double velocity) {
        return Math.toRadians(10.0 - 0.75 * Math.abs(velocity));
    }


    public PredictedState predict(Point2D.Double startLocation,
                                  double startHeading,
                                  double startVelocity,
                                  long   startTime,
                                  double destinationAngle,
                                  int    maxTicks) {

        Point2D.Double location = new Point2D.Double(
                startLocation.x, startLocation.y);
        double heading  = startHeading;
        double velocity = startVelocity;
        long   time     = startTime;

        for (int i = 0; i < maxTicks; i++) {
            double smoothedAngle = wallSmoothing.smooth(location, destinationAngle, 1);
            double turnNeeded    = MathUtils.normalRelativeAngle(smoothedAngle - heading);

            int dir = 1;
            if (Math.abs(turnNeeded) > Math.PI / 2) {
                turnNeeded += turnNeeded < 0 ? Math.PI : -Math.PI;
                dir = -1;
            }

            double maxTurn = maxTurnRateRadians(velocity);
            double turn    = MathUtils.clamp(turnNeeded, -maxTurn, maxTurn);
            heading        = MathUtils.normalAbsoluteAngle(heading + turn);

            velocity = calculateNewVelocity(velocity, dir);

            location = new Point2D.Double(
                    location.x + Math.sin(heading) * velocity,
                    location.y + Math.cos(heading) * velocity);
            location = battleField.clamp(location);

            time++;
        }

        return new PredictedState(location, heading, velocity, time);
    }


    public PredictedState predictUntilWaveHits(
            Point2D.Double startLocation,
            double startHeading,
            double startVelocity,
            long   startTime,
            double destinationAngle,
            Wave   wave) {

        Point2D.Double location = new Point2D.Double(
                startLocation.x, startLocation.y);
        double heading  = startHeading;
        double velocity = startVelocity;
        long   time     = startTime;

        for (int i = 0; i < 500; i++) {
            if (wave.hasReached(location, time)) {
                return new PredictedState(location, heading, velocity, time);
            }
            if (wave.hasPassed(location, time)) {
                return new PredictedState(location, heading, velocity, time);
            }

            double smoothedAngle = wallSmoothing.smooth(location, destinationAngle, 1);
            double turnNeeded    = MathUtils.normalRelativeAngle(smoothedAngle - heading);

            int dir = 1;
            if (Math.abs(turnNeeded) > Math.PI / 2) {
                turnNeeded += turnNeeded < 0 ? Math.PI : -Math.PI;
                dir = -1;
            }

            double maxTurn = maxTurnRateRadians(velocity);
            double turn    = MathUtils.clamp(turnNeeded, -maxTurn, maxTurn);
            heading        = MathUtils.normalAbsoluteAngle(heading + turn);

            velocity = calculateNewVelocity(velocity, dir);

            location = new Point2D.Double(
                    location.x + Math.sin(heading) * velocity,
                    location.y + Math.cos(heading) * velocity);
            location = battleField.clamp(location);

            time++;
        }

        return new PredictedState(location, heading, velocity, time);
    }


    private double calculateNewVelocity(double velocity, int direction) {
        if (velocity * direction < 0) {
            velocity += direction * DECELERATION;
        } else if (Math.abs(velocity) < MAX_VELOCITY) {
            velocity += direction * ACCELERATION;
        }
        return MathUtils.clamp(velocity, -MAX_VELOCITY, MAX_VELOCITY);
    }


    public BattleField getBattleField() { return battleField; }
}