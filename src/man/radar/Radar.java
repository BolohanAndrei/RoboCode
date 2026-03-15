package man.radar;

import robocode.AdvancedRobot;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import man.utils.EnemyData;
import man.utils.MathUtils;


public class Radar {

    private final AdvancedRobot robot;

    private boolean locked;

    private int sweepDirection;

    private EnemyData enemy;


    public Radar(AdvancedRobot robot) {
        this.robot          = robot;
        this.locked         = false;
        this.sweepDirection = 1;
    }

    public void init(EnemyData enemy) {
        this.enemy          = enemy;
        this.locked         = false;
        this.sweepDirection = 1;

        robot.setAdjustRadarForRobotTurn(true);
        robot.setAdjustRadarForGunTurn(true);
        robot.setAdjustGunForRobotTurn(true);

        robot.setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
    }


    public void execute() {
        if (!locked) {
            robot.setTurnRadarRightRadians(
                    sweepDirection * Double.POSITIVE_INFINITY);
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        locked = true;

        double radarHeading  = robot.getRadarHeadingRadians();
        double robotHeading  = robot.getHeadingRadians();
        double bearingToEnemy = MathUtils.normalAbsoluteAngle(
                robotHeading + e.getBearingRadians());

        double radarTurn = MathUtils.normalRelativeAngle(
                bearingToEnemy - radarHeading);


        double overscan = Math.PI / 18.0; // ~10 grade
        if (radarTurn < 0) {
            radarTurn -= overscan;
        } else {
            radarTurn += overscan;
        }

        robot.setTurnRadarRightRadians(radarTurn);
    }


    public void onRobotDeath(RobotDeathEvent e) {
        if (enemy != null && e.getName().equals(enemy.name)) {
            locked = false;
            robot.setTurnRadarRightRadians(
                    sweepDirection * Double.POSITIVE_INFINITY);
        }
    }

    public boolean isLocked() {
        return locked;
    }
}