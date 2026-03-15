package man.utils;

import java.awt.geom.Point2D;

/**
 * Snapshot imutabil al stării unui robot la un anumit tick.
 * Folosit de RobotStateLog, MovementPredictor și Wave Surfing.
 */
public class RobotState {

    public final Point2D.Double location;
    public final double heading;    // radiani
    public final double velocity;   // -8.0 .. 8.0
    public final long time;         // tick-ul

    private RobotState(Builder b) {
        this.location = b.location;
        this.heading  = b.heading;
        this.velocity = b.velocity;
        this.time     = b.time;
    }

    // -------------------------------------------------------
    // Builder
    // -------------------------------------------------------

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Point2D.Double location = new Point2D.Double(0, 0);
        private double heading  = 0;
        private double velocity = 0;
        private long   time     = 0;

        public Builder setLocation(Point2D.Double location) {
            this.location = location;
            return this;
        }

        public Builder setLocation(double x, double y) {
            this.location = new Point2D.Double(x, y);
            return this;
        }

        public Builder setHeading(double heading) {
            this.heading = heading;
            return this;
        }

        public Builder setVelocity(double velocity) {
            this.velocity = velocity;
            return this;
        }

        public Builder setTime(long time) {
            this.time = time;
            return this;
        }

        public RobotState build() {
            return new RobotState(this);
        }
    }

    // -------------------------------------------------------
    // Metode utilitare
    // -------------------------------------------------------

    public double getX() { return location.x; }
    public double getY() { return location.y; }

    // Distanța față de un alt punct
    public double distanceTo(Point2D.Double other) {
        return location.distance(other);
    }

    // Distanța față de altă stare
    public double distanceTo(RobotState other) {
        return location.distance(other.location);
    }

    @Override
    public String toString() {
        return String.format("RobotState[t=%d, (%.1f,%.1f), h=%.3f, v=%.2f]",
                time, location.x, location.y, heading, velocity);
    }
}