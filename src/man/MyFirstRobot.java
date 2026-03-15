package man;

import man.gun.GuessFactorTargeting;
import man.gun.PowerManager;
import man.move.WaveSurfer;
import man.radar.Radar;
import robocode.*;
import man.utils.*;

import java.awt.geom.Point2D;


public class MyFirstRobot extends AdvancedRobot {


    private BattleField          battleField;
    private EnemyData            enemy;
    private Radar                radar;
    private WaveManager          waveManager;
    private WaveSurfer           waveSurfer;
    private GuessFactorTargeting gfTargeting;
    private PowerManager         powerManager;


    @Override
    public void run() {

        initComponents();

        while (true) {
            long   time       = getTime();
            double myX        = getX();
            double myY        = getY();
            double myHeading  = getHeadingRadians();
            double myVelocity = getVelocity();
            Point2D.Double myPos = new Point2D.Double(myX, myY);

            RobotState myState = RobotState.newBuilder()
                    .setLocation(myPos)
                    .setHeading(myHeading)
                    .setVelocity(myVelocity)
                    .setTime(time)
                    .build();

            waveManager.update(myPos, time);

            if (enemy.lastScanTime > 0) {
                waveSurfer.move(myPos, myHeading, myVelocity,
                        time, enemy);
            }

            double bulletPower = powerManager.calculatePower(
                    getEnergy(), enemy, getGunHeat());

            if (enemy.lastScanTime > 0 && bulletPower > 0) {
                gfTargeting.aim(myPos, enemy, bulletPower);
                gfTargeting.fire(myPos, enemy, bulletPower);
            }

            radar.execute();

            execute();
        }
    }


    private void initComponents() {
        battleField  = new BattleField(
                getBattleFieldWidth(), getBattleFieldHeight());
        enemy        = new EnemyData();
        waveManager  = new WaveManager();
        waveSurfer   = new WaveSurfer(this, battleField, waveManager);
        gfTargeting  = new GuessFactorTargeting(this);
        powerManager = new PowerManager();
        radar        = new Radar(this);

        waveSurfer.setEnemy(enemy);

        radar.init(enemy);
        waveSurfer.init();
        gfTargeting.init();
    }


    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        long   time      = getTime();
        double myX       = getX();
        double myY       = getY();
        double myHeading = getHeadingRadians();

        boolean enemyFiredThisTick = enemy.firedThisTick();
        double  prevEnergy         = enemy.energy;

        enemy.update(e, myX, myY, myHeading, time);

        if (enemy.lastScanTime > 0 && enemy.firedThisTick()) {
            double estimatedPower = enemy.estimatedBulletPower();
            if (estimatedPower > 0) {
                Point2D.Double enemyPos = enemy.getLocation();
                Point2D.Double myPos    = new Point2D.Double(myX, myY);

                Wave wave = new Wave(
                        enemyPos,
                        time - 1,
                        estimatedPower,
                        myPos,
                        getHeadingRadians(),
                        getVelocity(),
                        MathUtils.lateralVelocity(
                                myPos, enemyPos,
                                getHeadingRadians(), getVelocity()),
                        MathUtils.lateralSign(
                                myPos, enemyPos,
                                getHeadingRadians(), getVelocity())
                );
                waveManager.addWave(wave);
            }
        }

        radar.onScannedRobot(e);
    }


    @Override
    public void onBulletHit(BulletHitEvent e) {
        Point2D.Double enemyPos = enemy.getLocation();
        gfTargeting.onBulletHit(enemyPos, enemy);
    }


    @Override
    public void onBulletMissed(BulletMissedEvent e) {
        gfTargeting.onBulletMissed();
    }


    @Override
    public void onHitByBullet(HitByBulletEvent e) {

    }


    @Override
    public void onHitWall(HitWallEvent e) {

    }


    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        radar.onRobotDeath(e);

        if (e.getName().equals(enemy.name)) {
            enemy = new EnemyData();
            waveSurfer.setEnemy(enemy);
            waveManager.clear();
        }
    }


    @Override
    public void onRoundEnded(RoundEndedEvent e) {

    }


    @Override
    public void onWin(WinEvent e) {
        for (int i = 0; i < 5; i++) {
            turnRight(45);
            turnLeft(45);
        }
    }


    @Override
    public void onDeath(DeathEvent e) {
    }
}