package man.move;

import robocode.AdvancedRobot;
import man.utils.*;

import java.awt.geom.Point2D;
import java.util.List;

public class WaveSurfer implements WaveManager.WaveHitListener {

    private static final double DEFAULT_DESIRED_DISTANCE = 150.0;
    private static final int    WAVES_TO_SURF            = 2;
    // minimum of data
    private static final int    MIN_STAT_DATA            = 5;

    private final AdvancedRobot     robot;
    private final BattleField       battleField;
    private final WaveManager       waveManager;
    private final MovementPredictor predictor;
    private final WallSmoothing     wallSmoothing;
    private final AStarNavigator    astar;

    private int            orbitDirection;
    private Point2D.Double lastDestination;

    // current desired orbit distance — updated dynamically from MyFirstRobot
    private double desiredDistance;

    // enemy reference for danger management
    private EnemyData enemy;

    // Constructor
    public WaveSurfer(AdvancedRobot robot,
                      BattleField battleField,
                      WaveManager waveManager) {
        this.robot = robot;
        this.battleField = battleField;
        this.waveManager = waveManager;
        this.predictor = new MovementPredictor(battleField);
        this.wallSmoothing = new WallSmoothing(battleField);
        this.astar = new AStarNavigator(battleField);
        this.orbitDirection = 1;
        this.desiredDistance = DEFAULT_DESIRED_DISTANCE;

        waveManager.addListener(this);
    }

    // Init
    public void init() {
        orbitDirection = 1;
        lastDestination = null;
        desiredDistance = DEFAULT_DESIRED_DISTANCE;
        waveManager.clear();
    }

    // Move
    public void move(Point2D.Double myPosition,
                     double myHeading,
                     double myVelocity,
                     long currentTime,
                     EnemyData enemy) {

        if (enemy == null) return;

        Point2D.Double enemyPos = enemy.getLocation();
        List<Wave> waves = waveManager.getClosestWaves(myPosition, currentTime, WAVES_TO_SURF);

        Point2D.Double destination;

        if (waves.isEmpty()) {
            // FIX #7: no wave
            destination = orbitDestination(myPosition, enemyPos);
        } else {
            // Update danger map
            astar.clearDangerMap();
            astar.markWallDanger();
            for (Wave wave : waves) {
                astar.markWaveDanger(wave, currentTime);
            }

            // evaluate
            double cwDanger = evaluateDirection(myPosition, myHeading, myVelocity, currentTime, enemyPos, 1, waves);
            double ccwDanger = evaluateDirection(myPosition, myHeading, myVelocity, currentTime, enemyPos, -1, waves);

            orbitDirection = ccwDanger < cwDanger ? -1 : 1;

            // FIX #7: A* finds destination
            destination = astar.findSafestDestination(myPosition);
        }

        // Wall smoothing
        double angleToDestination = MathUtils.absoluteBearing(myPosition, destination);
        angleToDestination = wallSmoothing.smooth(
                myPosition, angleToDestination, orbitDirection);

        applyMovement(myPosition, angleToDestination, myHeading, myVelocity, enemyPos);

        lastDestination = destination;
    }

    // Danger Evaluation
    private double evaluateDirection(Point2D.Double myPosition,
                                     double myHeading,
                                     double myVelocity,
                                     long currentTime,
                                     Point2D.Double enemyPos,
                                     int direction,
                                     List<Wave> waves) {
        double orbitAngle = orbitAngleForDirection(myPosition, enemyPos, direction);
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

        // Compute every hit
        double total = 0;
        for (double stat : enemy.guessFactorStats) {
            total += stat;
        }

        // Fallback gaussian
        if (total < MIN_STAT_DATA) {
            return gaussianDanger(gf);
        }

        // map GF [-1, 1] in bin [0, GF_BINS-1]
        int bin = (int) Math.round(
                (gf + 1.0) / 2.0 * (EnemyData.GF_BINS - 1));
        bin = (int) MathUtils.clamp(bin, 0, EnemyData.GF_BINS - 1);

        // normalize to [0, 1]
        double maxStat = 0;
        for (double stat : enemy.guessFactorStats) {
            if (stat > maxStat) maxStat = stat;
        }

        if (maxStat == 0) return gaussianDanger(gf);

        // Smooth: combine real with gaussian
        double statDanger = enemy.guessFactorStats[bin] / maxStat;
        double gaussDanger = gaussianDanger(gf);
        return 0.8 * statDanger + 0.2 * gaussDanger;
    }

    // Gaussian centered on GF=0
    private double gaussianDanger(double gf) {
        return Math.exp(-gf * gf * 4.0);
    }

    // Orbiting
    private double orbitAngleForDirection(Point2D.Double myPosition,
                                          Point2D.Double enemyPos,
                                          int direction) {
        double bearingToEnemy     = MathUtils.absoluteBearing(myPosition, enemyPos);
        double orbitAngle         = bearingToEnemy + direction * Math.PI / 2.0;
        double dist               = myPosition.distance(enemyPos);
        // use desiredDistance — updated dynamically from MyFirstRobot
        double distanceCorrection = (dist - desiredDistance) / 1000.0;
        orbitAngle += distanceCorrection * direction;
        return orbitAngle;
    }

    private Point2D.Double orbitDestination(Point2D.Double myPosition,
                                            Point2D.Double enemyPos) {
        double angle = orbitAngleForDirection(myPosition, enemyPos, orbitDirection);
        angle = wallSmoothing.smooth(myPosition, angle, orbitDirection);
        return MathUtils.project(myPosition, angle, 100.0);
    }

    // Apply movement
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
        robot.setMaxVelocity(dist < desiredDistance * 0.7 ? 4.0 : 8.0);
    }

    // WaveHitListener
    @Override
    public void onWaveHit(Wave wave, Point2D.Double myPosition, long time) {
        double gf = wave.guessFactorForPosition(myPosition);
        wave.hitGuessFactor = gf;
    }

    // Getters / Setters
    public void setEnemy(EnemyData enemy) { this.enemy = enemy; }
    public void setDesiredDistance(double distance) { this.desiredDistance = distance; }
    public double getDesiredDistance() { return desiredDistance; }
    public int getOrbitDirection() { return orbitDirection; }
    public Point2D.Double getLastDestination() { return lastDestination; }
}