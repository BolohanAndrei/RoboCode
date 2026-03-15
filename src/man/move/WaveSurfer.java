package man.move;

import robocode.AdvancedRobot;
import man.utils.*;

import java.awt.geom.Point2D;
import java.util.List;


public class WaveSurfer implements WaveHitListener {

    private static final double DESIRED_DISTANCE = 500.0;
    private static final int    WAVES_TO_SURF    = 2;
    private static final int    MIN_STAT_DATA    = 5;

    private final AdvancedRobot     robot;
    private final BattleField       battleField;
    private final WaveManager       waveManager;
    private final MovementPredictor predictor;
    private final WallSmoothing     wallSmoothing;
    private final AStarNavigator    astar;

    private int            orbitDirection;
    private Point2D.Double lastDestination;

    private EnemyData enemy;


    public WaveSurfer(AdvancedRobot robot,
                      BattleField battleField,
                      WaveManager waveManager) {
        this.robot         = robot;
        this.battleField   = battleField;
        this.waveManager   = waveManager;
        this.predictor     = new MovementPredictor(battleField);
        this.wallSmoothing = new WallSmoothing(battleField);
        this.astar         = new AStarNavigator(battleField);
        this.orbitDirection = 1;

        waveManager.addListener(this);
    }

    public void init() {
        orbitDirection  = 1;
        lastDestination = null;
        waveManager.clear();
    }

    public void move(Point2D.Double myPosition,
                     double myHeading,
                     double myVelocity,
                     long   currentTime,
                     EnemyData enemy) {

        if (enemy == null) return;

        Point2D.Double enemyPos = enemy.getLocation();
        List<Wave>     waves    = waveManager.getClosestWaves(
                myPosition, currentTime, WAVES_TO_SURF);

        Point2D.Double destination;

        if (waves.isEmpty()) {
            destination = orbitDestination(myPosition, enemyPos);
        } else {
            astar.clearDangerMap();
            astar.markWallDanger();
            for (Wave wave : waves) {
                astar.markWaveDanger(wave, currentTime);
            }

            double cwDanger  = evaluateDirection(myPosition, myHeading,
                    myVelocity, currentTime, enemyPos, 1, waves);
            double ccwDanger = evaluateDirection(myPosition, myHeading,
                    myVelocity, currentTime, enemyPos, -1, waves);

            orbitDirection = ccwDanger < cwDanger ? -1 : 1;

            destination = astar.findSafestDestination(myPosition);
        }

        double angleToDestination = MathUtils.absoluteBearing(myPosition, destination);
        angleToDestination = wallSmoothing.smooth(
                myPosition, angleToDestination, orbitDirection);

        applyMovement(myPosition, angleToDestination, myHeading, myVelocity, enemyPos);

        lastDestination = destination;
    }


    private double evaluateDirection(Point2D.Double myPosition,
                                     double myHeading,
                                     double myVelocity,
                                     long   currentTime,
                                     Point2D.Double enemyPos,
                                     int direction,
                                     List<Wave> waves) {
        double orbitAngle  = orbitAngleForDirection(myPosition, enemyPos, direction);
        double totalDanger = 0;

        for (Wave wave : waves) {
            MovementPredictor.PredictedState state =
                    predictor.predictUntilWaveHits(
                            myPosition, myHeading, myVelocity,
                            currentTime, orbitAngle, wave);

            double gf = wave.guessFactorForPosition(state.location);
            totalDanger += enemy_danger_at_gf(gf, wave);
        }

        return totalDanger;
    }


    private double enemy_danger_at_gf(double gf, Wave wave) {
        if (enemy == null) {
            return gaussianDanger(gf);
        }

        double total = 0;
        for (double stat : enemy.guessFactorStats) {
            total += stat;
        }

        if (total < MIN_STAT_DATA) {
            return gaussianDanger(gf);
        }

        int bin = (int) Math.round(
                (gf + 1.0) / 2.0 * (EnemyData.GF_BINS - 1));
        bin = (int) MathUtils.clamp(bin, 0, EnemyData.GF_BINS - 1);

        double maxStat = 0;
        for (double stat : enemy.guessFactorStats) {
            if (stat > maxStat) maxStat = stat;
        }

        if (maxStat == 0) return gaussianDanger(gf);

        double statDanger   = enemy.guessFactorStats[bin] / maxStat;
        double gaussDanger  = gaussianDanger(gf);
        return 0.8 * statDanger + 0.2 * gaussDanger;
    }

    private double gaussianDanger(double gf) {
        return Math.exp(-gf * gf * 4.0);
    }


    private double orbitAngleForDirection(Point2D.Double myPosition,
                                          Point2D.Double enemyPos,
                                          int direction) {
        double bearingToEnemy     = MathUtils.absoluteBearing(myPosition, enemyPos);
        double orbitAngle         = bearingToEnemy + direction * Math.PI / 2.0;
        double dist               = myPosition.distance(enemyPos);
        double distanceCorrection = (dist - DESIRED_DISTANCE) / 1000.0;
        orbitAngle += distanceCorrection * direction;
        return orbitAngle;
    }

    private Point2D.Double orbitDestination(Point2D.Double myPosition,
                                            Point2D.Double enemyPos) {
        double angle = orbitAngleForDirection(myPosition, enemyPos, orbitDirection);
        angle = wallSmoothing.smooth(myPosition, angle, orbitDirection);
        return MathUtils.project(myPosition, angle, 100.0);
    }


    private void applyMovement(Point2D.Double myPosition,
                               double destinationAngle,
                               double myHeading,
                               double myVelocity,
                               Point2D.Double enemyPos) {

        double turnNeeded = MathUtils.normalRelativeAngle(
                destinationAngle - myHeading);
        int moveDir = 1;

        if (Math.abs(turnNeeded) > Math.PI / 2) {
            turnNeeded += turnNeeded < 0 ? Math.PI : -Math.PI;
            moveDir = -1;
        }

        robot.setTurnRightRadians(turnNeeded);
        robot.setAhead(moveDir * 100.0);

        double dist = myPosition.distance(enemyPos);
        robot.setMaxVelocity(dist < DESIRED_DISTANCE * 0.7 ? 4.0 : 8.0);
    }

    @Override
    public void onWaveHit(Wave wave, Point2D.Double myPosition, long time) {
        double gf = wave.guessFactorForPosition(myPosition);
        wave.hitGuessFactor = gf;
    }

    public void setEnemy(EnemyData enemy) { this.enemy = enemy; }

    public int getOrbitDirection() { return orbitDirection; }

    public Point2D.Double getLastDestination() { return lastDestination; }
}