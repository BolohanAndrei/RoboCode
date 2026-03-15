package man.radar;

import robocode.AdvancedRobot;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import man.utils.EnemyData;
import man.utils.MathUtils;

public class Radar {

    private final AdvancedRobot robot;

    // if we lock on enemy
    private boolean locked;

    // initial sweep direction
    private int sweepDirection;

    // refernce to enemy data
    private EnemyData enemy;

    // Constructor
    public Radar(AdvancedRobot robot) {
        this.robot = robot;
        this.locked= false;
        this.sweepDirection = 1;
    }

    // init every round
    public void init(EnemyData enemy) {
        this.enemy=enemy;
        this.locked=false;
        this.sweepDirection = 1;

        robot.setAdjustRadarForRobotTurn(true);
        robot.setAdjustRadarForGunTurn(true);
        robot.setAdjustGunForRobotTurn(true);

        // start initial sweep
        robot.setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
    }

    // executed every tick
    public void execute() {
        if (!locked) {
            //continue sweep until enemy found
            robot.setTurnRadarRightRadians(
                    sweepDirection * Double.POSITIVE_INFINITY);
        }
        //if we have lock, radar movement is on onScannedRobot
    }

    // onScannedRobot — called from myBot
    public void onScannedRobot(ScannedRobotEvent e) {
        locked = true;

        // compute angle
        double radarHeading = robot.getRadarHeadingRadians();
        double robotHeading = robot.getHeadingRadians();
        double bearingToEnemy = MathUtils.normalAbsoluteAngle(
                robotHeading + e.getBearingRadians());

        // diff between radar position and enemy position
        double radarTurn = MathUtils.normalRelativeAngle(
                bearingToEnemy - radarHeading);

        // add extra-rotation to not lose on the next tick
        double overscan = Math.PI / 18.0; // ~10 grade
        if (radarTurn < 0) {
            radarTurn -= overscan;
        } else {
            radarTurn += overscan;
        }

        robot.setTurnRadarRightRadians(radarTurn);
    }

    // onRobotDeath
    public void onRobotDeath(RobotDeathEvent e) {
        if (enemy != null && e.getName().equals(enemy.name)) {
            locked = false;
            robot.setTurnRadarRightRadians(
                    sweepDirection * Double.POSITIVE_INFINITY);
        }
    }

    // Getters
    public boolean isLocked() {
        return locked;
    }
}