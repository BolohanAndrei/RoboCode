package man.gun;

import man.utils.EnemyData;
import man.utils.MaxEscapeAngle;
import man.utils.MathUtils;


public class PowerManager {


    private static final double MIN_POWER = 0.1;
    private static final double MAX_POWER = 3.0;

    private static final double LOW_ENERGY_THRESHOLD      = 20.0;
    private static final double CRITICAL_ENERGY_THRESHOLD = 10.0;
    private static final double KILL_SHOT_THRESHOLD       = 4.0;

    private static final double CLOSE_RANGE    = 150.0;
    private static final double MID_RANGE      = 350.0;
    private static final double LONG_RANGE     = 600.0;
    private static final double MAX_FIRE_RANGE = 800.0;


    public PowerManager() {
    }


    public double calculatePower(double myEnergy,
                                 EnemyData enemy,
                                 double gunHeat) {

        if (gunHeat > 0) return 0;

        if (enemy == null || enemy.distance > MAX_FIRE_RANGE) return 0;

        double power;

        if (enemy.energy <= KILL_SHOT_THRESHOLD) {
            power = killShotPower(enemy.energy);
            return MathUtils.clamp(power, MIN_POWER, MAX_POWER);
        }

        power = powerByDistance(enemy.distance);

        power = adjustForOwnEnergy(power, myEnergy);

        power = adjustForEnemyEnergy(power, enemy.energy);

        power = adjustForExtremeDistance(power, enemy.distance);

        return MathUtils.clamp(power, MIN_POWER, MAX_POWER);
    }


    private double killShotPower(double enemyEnergy) {
        double powerNeeded;
        if (enemyEnergy > 4.0) {
            powerNeeded = (enemyEnergy + 2.0) / 6.0;
        } else {
            powerNeeded = enemyEnergy / 4.0 + 0.1;
        }
        return powerNeeded;
    }

    private double powerByDistance(double distance) {
        if (distance <= CLOSE_RANGE) {
            return 3.0;
        } else if (distance <= MID_RANGE) {
            double t = (distance - CLOSE_RANGE) / (MID_RANGE - CLOSE_RANGE);
            return MathUtils.lerp(3.0, 2.0, t);
        } else if (distance <= LONG_RANGE) {
            double t = (distance - MID_RANGE) / (LONG_RANGE - MID_RANGE);
            return MathUtils.lerp(2.0, 1.0, t);
        } else {
            double t = (distance - LONG_RANGE) / (MAX_FIRE_RANGE - LONG_RANGE);
            return MathUtils.lerp(1.0, 0.5, t);
        }
    }


    private double adjustForOwnEnergy(double power, double myEnergy) {
        if (myEnergy <= CRITICAL_ENERGY_THRESHOLD) {
            return Math.min(power, 0.5);
        } else if (myEnergy <= LOW_ENERGY_THRESHOLD) {
            double maxPower = MathUtils.lerp(0.5, MAX_POWER,
                    (myEnergy - CRITICAL_ENERGY_THRESHOLD)
                            / (LOW_ENERGY_THRESHOLD - CRITICAL_ENERGY_THRESHOLD));
            return Math.min(power, maxPower);
        }
        return power;
    }


    private double adjustForEnemyEnergy(double power, double enemyEnergy) {
        if (enemyEnergy < 20.0 && enemyEnergy > KILL_SHOT_THRESHOLD) {
            double bonus = (20.0 - enemyEnergy) / 20.0 * 0.5;
            return power + bonus;
        }
        return power;
    }


    private double adjustForExtremeDistance(double power, double distance) {
        if (distance > LONG_RANGE) {
            return Math.min(power, 1.5);
        }
        return power;
    }


    public double riskRewardRatio(double bulletPower) {
        double damage   = MaxEscapeAngle.bulletDamage(bulletPower);
        double regained = MaxEscapeAngle.energyRegained(bulletPower);
        double cost     = bulletPower;
        return (damage + regained) / cost;
    }


    public double optimalPower(double distance, double myEnergy,
                               EnemyData enemy) {
        double bestPower = MIN_POWER;
        double bestRatio = 0;

        for (double p = MIN_POWER; p <= MAX_POWER; p += 0.1) {
            double ratio = riskRewardRatio(p);

            double speedPenalty = distance / 1000.0
                    * (MAX_POWER - p) / MAX_POWER;
            ratio -= speedPenalty;

            if (ratio > bestRatio) {
                bestRatio = ratio;
                bestPower = p;
            }
        }

        return bestPower;
    }
}