package man.gun;

import man.utils.EnemyData;
import man.utils.MaxEscapeAngle;
import man.utils.MathUtils;

/**
 * Power Manager — decide puterea optimă a glonțului
 * la fiecare tragere.
 *
 * Criterii (în ordine de prioritate):
 * 1. Kill shot: dacă inamicul are puțină energie
 * 2. Energie proprie scăzută: tragem mai slab
 * 3. Distanță: mai aproape = putere mai mare
 * 4. Gun heat: nu tragem dacă tunul e fierbinte
 */
public class PowerManager {

    // -------------------------------------------------------
    // Constante
    // -------------------------------------------------------

    // Putere minimă și maximă permisă
    private static final double MIN_POWER = 0.1;
    private static final double MAX_POWER = 3.0;

    // Praguri de energie
    private static final double LOW_ENERGY_THRESHOLD      = 20.0;
    private static final double CRITICAL_ENERGY_THRESHOLD = 10.0;
    private static final double KILL_SHOT_THRESHOLD       = 4.0;

    // Praguri de distanță
    private static final double CLOSE_RANGE    = 150.0;
    private static final double MID_RANGE      = 350.0;
    private static final double LONG_RANGE     = 600.0;
    private static final double MAX_FIRE_RANGE = 800.0;

    // -------------------------------------------------------
    // Constructor
    // -------------------------------------------------------
    public PowerManager() {
        // nimic de inițializat
    }

    // -------------------------------------------------------
    // Metodă principală
    // -------------------------------------------------------

    /**
     * Calculează puterea optimă a glonțului.
     *
     * @param myEnergy    energia proprie
     * @param enemy       datele inamicului
     * @param gunHeat     căldura tunului curent
     * @return puterea glonțului (0 = nu trage)
     */
    public double calculatePower(double myEnergy,
                                 EnemyData enemy,
                                 double gunHeat) {

        // 0. Nu trage dacă tunul e fierbinte
        if (gunHeat > 0) return 0;

        // 0. Nu trage dacă inamicul e prea departe
        if (enemy == null || enemy.distance > MAX_FIRE_RANGE) return 0;

        double power;

        // 1. Kill shot — dacă inamicul are foarte puțină energie
        if (enemy.energy <= KILL_SHOT_THRESHOLD) {
            power = killShotPower(enemy.energy);
            return MathUtils.clamp(power, MIN_POWER, MAX_POWER);
        }

        // 2. Putere de bază în funcție de distanță
        power = powerByDistance(enemy.distance);

        // 3. Ajustare dacă energia noastră e scăzută
        power = adjustForOwnEnergy(power, myEnergy);

        // 4. Ajustare dacă și inamicul e scăzut (oportunitate)
        power = adjustForEnemyEnergy(power, enemy.energy);

        // 5. Ajustare dacă distanța e extremă
        power = adjustForExtremeDistance(power, enemy.distance);

        return MathUtils.clamp(power, MIN_POWER, MAX_POWER);
    }

    // -------------------------------------------------------
    // Kill shot
    // -------------------------------------------------------

    /**
     * Calculează puterea minimă necesară pentru a ucide inamicul.
     * damage = 4*p + 2*(p-1) pentru p > 1  =>  6p - 2 = damage
     * damage = 4*p pentru p <= 1
     *
     * Rezolvăm pentru p: p = (damage + 2) / 6 dacă > 1
     *                    p = damage / 4 dacă <= 1
     */
    private double killShotPower(double enemyEnergy) {
        // Puterea necesară pentru damage = enemyEnergy
        double powerNeeded;
        if (enemyEnergy > 4.0) {
            // damage = 6p - 2 => p = (damage + 2) / 6
            powerNeeded = (enemyEnergy + 2.0) / 6.0;
        } else {
            // damage = 4p => p = damage / 4
            powerNeeded = enemyEnergy / 4.0 + 0.1;
        }
        return powerNeeded;
    }

    // -------------------------------------------------------
    // Putere bazată pe distanță
    // -------------------------------------------------------
    private double powerByDistance(double distance) {
        if (distance <= CLOSE_RANGE) {
            return 3.0;             // aproape: putere maximă
        } else if (distance <= MID_RANGE) {
            // interpolare liniară între 3.0 și 2.0
            double t = (distance - CLOSE_RANGE) / (MID_RANGE - CLOSE_RANGE);
            return MathUtils.lerp(3.0, 2.0, t);
        } else if (distance <= LONG_RANGE) {
            // interpolare liniară între 2.0 și 1.0
            double t = (distance - MID_RANGE) / (LONG_RANGE - MID_RANGE);
            return MathUtils.lerp(2.0, 1.0, t);
        } else {
            // departe: putere mică (glonț rapid = mai ușor de lovit)
            double t = (distance - LONG_RANGE) / (MAX_FIRE_RANGE - LONG_RANGE);
            return MathUtils.lerp(1.0, 0.5, t);
        }
    }

    // -------------------------------------------------------
    // Ajustări
    // -------------------------------------------------------

    /**
     * Reduce puterea dacă energia noastră e scăzută.
     * Nu vrem să rămânem fără energie.
     */
    private double adjustForOwnEnergy(double power, double myEnergy) {
        if (myEnergy <= CRITICAL_ENERGY_THRESHOLD) {
            // Energie critică: tragem foarte slab
            return Math.min(power, 0.5);
        } else if (myEnergy <= LOW_ENERGY_THRESHOLD) {
            // Energie scăzută: limităm puterea
            double maxPower = MathUtils.lerp(0.5, MAX_POWER,
                    (myEnergy - CRITICAL_ENERGY_THRESHOLD)
                            / (LOW_ENERGY_THRESHOLD - CRITICAL_ENERGY_THRESHOLD));
            return Math.min(power, maxPower);
        }
        return power;
    }

    /**
     * Crește ușor puterea dacă inamicul e slăbit
     * (oportunitate de a-l elimina).
     */
    private double adjustForEnemyEnergy(double power, double enemyEnergy) {
        if (enemyEnergy < 20.0 && enemyEnergy > KILL_SHOT_THRESHOLD) {
            // Inamicul e slăbit: creștem puțin puterea
            double bonus = (20.0 - enemyEnergy) / 20.0 * 0.5;
            return power + bonus;
        }
        return power;
    }

    /**
     * Ajustare pentru distanțe extreme.
     */
    private double adjustForExtremeDistance(double power, double distance) {
        if (distance > LONG_RANGE) {
            // La distanță mare, glonțul mai rapid (putere mică) e mai precis
            return Math.min(power, 1.5);
        }
        return power;
    }

    // -------------------------------------------------------
    // Utilitar: raportul risc/recompensă
    // -------------------------------------------------------

    /**
     * Calculează raportul risc/recompensă pentru o putere dată.
     * Recompensă = damage produs + energie recuperată
     * Risc       = energia cheltuită (puterea glonțului)
     */
    public double riskRewardRatio(double bulletPower) {
        double damage   = MaxEscapeAngle.bulletDamage(bulletPower);
        double regained = MaxEscapeAngle.energyRegained(bulletPower);
        double cost     = bulletPower;
        return (damage + regained) / cost;
    }

    /**
     * Puterea optimă pentru maximizarea raportului risc/recompensă
     * la o distanță dat��.
     */
    public double optimalPower(double distance, double myEnergy,
                               EnemyData enemy) {
        double bestPower = MIN_POWER;
        double bestRatio = 0;

        for (double p = MIN_POWER; p <= MAX_POWER; p += 0.1) {
            double ratio = riskRewardRatio(p);
            // Penalizăm puterile mari la distanțe mari
            // (glont lent = mai usor de evitat)
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