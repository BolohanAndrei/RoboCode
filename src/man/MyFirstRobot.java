package man;

import man.gun.GuessFactorTargeting;
import man.gun.PowerManager;
import man.move.WaveSurfer;
import man.radar.Radar;
import robocode.*;
import robocode.util.Utils;
import man.utils.*;

import java.awt.geom.Point2D;


public class MyFirstRobot extends AdvancedRobot {

    // Principal Components
    private BattleField battleField;
    private EnemyData enemy;
    private Radar radar;
    private WaveManager waveManager;
    private WaveSurfer waveSurfer;
    private GuessFactorTargeting gfTargeting;
    private PowerManager powerManager;

    // sliding window of last 6 shots: true = hit enemy, false = missed
    private static final int  WINDOW_SIZE    = 6;
    private static final int  MIN_HITS       = 3;
    private boolean[] shotWindow;
    private int shotWindowIndex;

    // run() — entry point
    @Override
    public void run() {

        // 1. Initialize Components
        initComponents();

        // 2. Main loop
        while (true) {
            long time = getTime();
            double myX = getX();
            double myY = getY();
            double myHeading = getHeadingRadians();
            double myVelocity = getVelocity();
            Point2D.Double myPos = new Point2D.Double(myX, myY);

            // 2a. Update own state in log
            RobotState myState = RobotState.newBuilder()
                    .setLocation(myPos)
                    .setHeading(myHeading)
                    .setVelocity(myVelocity)
                    .setTime(time)
                    .build();

            // 2b. Update wave manager
            waveManager.update(myPos, time);

            // 2c. Move Robot (Wave Surfing + A*)
            if (enemy.lastScanTime > 0) {
                waveSurfer.move(myPos, myHeading, myVelocity,
                        time, enemy);
            }

            // 2d. Compute Bullet Power
            double bulletPower = powerManager.calculatePower(
                    getEnergy(), enemy, getGunHeat());

            // 2e. Aim and fire
            if (enemy.lastScanTime > 0 && bulletPower > 0) {
                gfTargeting.aim(myPos, enemy, bulletPower);
                gfTargeting.fire(myPos, enemy, bulletPower);
            }

            // 2f. Radar
            radar.execute();

            // 2g. Apply all set commands
            execute();
        }
    }

    // Init components
    private void initComponents() {
        battleField = new BattleField(getBattleFieldWidth(), getBattleFieldHeight());
        enemy = new EnemyData();
        waveManager = new WaveManager();
        waveSurfer = new WaveSurfer(this, battleField, waveManager);
        gfTargeting = new GuessFactorTargeting(this);
        powerManager = new PowerManager();
        radar = new Radar(this);

        // init sliding window (all false = no hits yet)
        shotWindow      = new boolean[WINDOW_SIZE];
        shotWindowIndex = 0;

        // tie enemy to wave surfer
        waveSurfer.setEnemy(enemy);

        //init every component for first round
        radar.init(enemy);
        waveSurfer.init();
        gfTargeting.init();
    }

    // count hits in window
    private int countHits() {
        int count = 0;
        for (boolean b : shotWindow) if (b) count++;
        return count;
    }

    // update desired distance: close in if less than MIN_HITS in last WINDOW_SIZE shots
    private void updateDesiredDistance() {
        if (countHits() < MIN_HITS) {
            waveSurfer.setDesiredDistance(waveSurfer.getDesiredDistance() - 100);
        }
    }

    // Events Robocode

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        long time = getTime();
        double myX = getX();
        double myY= getY();
        double myHeading = getHeadingRadians();

        // 1. Verify if enemy has fired
        boolean enemyFiredThisTick = enemy.firedThisTick();
        double prevEnergy = enemy.energy;

        // 2. Update enemy data
        enemy.update(e, myX, myY, myHeading, time);

        // 3. If shot, create a new wave
        if (enemy.lastScanTime > 0 && enemy.firedThisTick()) {
            double estimatedPower = enemy.estimatedBulletPower();
            if (estimatedPower > 0) {
                Point2D.Double enemyPos = enemy.getLocation();
                Point2D.Double myPos    = new Point2D.Double(myX, myY);

                Wave wave = new Wave(
                        enemyPos, // fireLocation
                        time - 1, // fireTime (tick anterior)
                        estimatedPower, // bulletPower
                        myPos, // targetLocation (noi)
                        getHeadingRadians(), // targetHeading
                        getVelocity(), // targetVelocity
                        MathUtils.lateralVelocity(// targetLateralVelocity
                                myPos, enemyPos,
                                getHeadingRadians(), getVelocity()),
                        MathUtils.lateralSign(// targetLateralSign
                                myPos, enemyPos,
                                getHeadingRadians(), getVelocity())
                );
                waveManager.addWave(wave);
            }
        }

        // 4. Update radar
        radar.onScannedRobot(e);
    }

    //our bullet hit enemy
    @Override
    public void onBulletHit(BulletHitEvent e) {
        Point2D.Double enemyPos = enemy.getLocation();
        gfTargeting.onBulletHit(enemyPos, enemy);

        // record hit, re-evaluate distance
        shotWindow[shotWindowIndex % WINDOW_SIZE] = true;
        shotWindowIndex++;
        updateDesiredDistance();
    }

    //our bullet missed
    @Override
    public void onBulletMissed(BulletMissedEvent e) {
        gfTargeting.onBulletMissed();

        // record miss, re-evaluate distance
        shotWindow[shotWindowIndex % WINDOW_SIZE] = false;
        shotWindowIndex++;
        updateDesiredDistance();
    }

    //got hit by enemy
    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        //that wave will be processed by Wave Manger
    }

    //wall hit
    @Override
    public void onHitWall(HitWallEvent e) {
        //wallsmoothing prevents that
    }

    //robot dies
    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        radar.onRobotDeath(e);

        //if enemy died reset data
        if (e.getName().equals(enemy.name)) {
            enemy = new EnemyData();
            waveSurfer.setEnemy(enemy);
            waveManager.clear();
        }
    }


    //new round
    @Override
    public void onRoundEnded(RoundEndedEvent e) {
        // GF stats are kept between rounds
    }

    //win
    @Override
    public void onWin(WinEvent e) {
        for (int i = 0; i < 5; i++) {
            turnRight(45);
            turnLeft(45);
        }
    }

    //death
    @Override
    public void onDeath(DeathEvent e) {
    }
}