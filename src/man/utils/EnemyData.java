package man.utils;

import java.awt.geom.Point2D;
import robocode.ScannedRobotEvent;


public class EnemyData {


    public String name;
    public double x;
    public double y;
    public double heading;
    public double velocity;
    public double energy;
    public double distance;
    public long lastScanTime;

    public double lateralVelocity;
    public double approachVelocity;
    public int lateralSign;

    public double lastEnergy;
    public double energyDelta;

    // State history
    public final RobotStateLog stateLog;

    // GuessFactor stats
    public static final int GF_BINS = 31;
    public final double[] guessFactorStats;

    // Constructor
    public EnemyData() {
        this.stateLog= new RobotStateLog();
        this.guessFactorStats =new double[GF_BINS];
        this.energy= 100.0;
        this.lastEnergy = 100.0;
    }

    // Update
    public void update(ScannedRobotEvent e,
                       double myX, double myY,
                       double myHeading,
                       long time) {

        this.lastEnergy = this.energy;
        this.energyDelta = e.getEnergy() - this.energy;

        this.name = e.getName();
        this.energy = e.getEnergy();
        this.velocity= e.getVelocity();
        this.heading = e.getHeadingRadians();
        this.distance= e.getDistance();
        this.lastScanTime = time;

        double absBearing = MathUtils.normalAbsoluteAngle(
                myHeading + e.getBearingRadians());
        this.x = myX + Math.sin(absBearing) * e.getDistance();
        this.y = myY + Math.cos(absBearing) * e.getDistance();

        Point2D.Double myPos= new Point2D.Double(myX, myY);
        Point2D.Double enemyPos = new Point2D.Double(this.x, this.y);

        double bearingToEnemy = MathUtils.absoluteBearing(myPos, enemyPos);

        this.lateralVelocity = velocity * Math.sin(heading - bearingToEnemy);
        this.approachVelocity = velocity * Math.cos(heading - bearingToEnemy);
        this.lateralSign = lateralVelocity >= 0 ? 1 : -1;

        RobotState state = RobotState.newBuilder()
                .setLocation(enemyPos)
                .setHeading(heading)
                .setVelocity(velocity)
                .setTime(time)
                .build();
        this.stateLog.addState(state);
    }

    public Point2D.Double getLocation() {
        return new Point2D.Double(x, y);
    }

    public boolean firedThisTick(double distanceToMe) {
        //no self collisions
        if (distanceToMe < 50) return false;
        // no collision with wall
        if (Math.abs(energyDelta + 1.0) < 0.001) return false;
        return energyDelta < -0.09 && energyDelta > -3.1;
    }


    public boolean firedThisTick() {
        return energyDelta < -0.09 && energyDelta > -3.1;
    }

    public double estimatedBulletPower() {
        if (!firedThisTick()) return 0;
        return -energyDelta;
    }

    public double estimatedBulletSpeed() {
        return 20.0 - 3.0 * estimatedBulletPower();
    }

    public void logGuessFactorHit(double guessFactor) {
        int bin = (int) Math.round(
                (guessFactor + 1.0) / 2.0 * (GF_BINS - 1));
        bin = (int) MathUtils.clamp(bin, 0, GF_BINS - 1);
        guessFactorStats[bin]++;
    }

    public int bestGuessFactorBin() {
        int best = GF_BINS / 2;
        for (int i = 0; i < GF_BINS; i++) {
            if (guessFactorStats[i] > guessFactorStats[best]) {
                best = i;
            }
        }
        return best;
    }

    public double binToGuessFactor(int bin) {
        return (bin / (double)(GF_BINS - 1)) * 2.0 - 1.0;
    }

    public void resetStats() {
        for (int i = 0; i < GF_BINS; i++) {
            guessFactorStats[i] = 0;
        }
        stateLog.clear();
    }

    public boolean isFresh(long currentTime, long maxAge) {
        return (currentTime - lastScanTime) <= maxAge;
    }

    @Override
    public String toString() {
        return String.format(
                "EnemyData[%s, (%.1f,%.1f), e=%.1f, v=%.2f, lv=%.2f]",
                name, x, y, energy, velocity, lateralVelocity);
    }
}