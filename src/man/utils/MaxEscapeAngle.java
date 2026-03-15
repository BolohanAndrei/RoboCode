package man.utils;

import java.awt.geom.Point2D;

/**
 * Calculează Maximum Escape Angle (MEA) —
 * unghiul maxim la care se poate deplasa un robot față de
 * linia directă glonț-țintă, dat fiind viteza glonțului.
 *
 * MEA = arcsin(MAX_ROBOT_VELOCITY / bulletSpeed)
 *
 * Folosit de GuessFactor Targeting pentru a normaliza
 * unghiul de tragere în intervalul [-1, 1] (GuessFactor).
 */
public class MaxEscapeAngle {

    // Viteza maximă a unui robot Robocode
    private static final double MAX_ROBOT_VELOCITY = 8.0;

    // -------------------------------------------------------
    // Calcul MEA de bază
    // -------------------------------------------------------

    /**
     * MEA pentru o putere de glonț dată.
     */
    public static double calculate(double bulletPower) {
        double bulletSpeed = bulletSpeed(bulletPower);
        return Math.asin(MAX_ROBOT_VELOCITY / bulletSpeed);
    }

    /**
     * MEA pentru o viteză de glonț dată.
     */
    public static double calculateFromSpeed(double bulletSpeed) {
        if (bulletSpeed <= MAX_ROBOT_VELOCITY) {
            return Math.PI / 2; // limită teoretică
        }
        return Math.asin(MAX_ROBOT_VELOCITY / bulletSpeed);
    }

    // -------------------------------------------------------
    // Calcul MEA ținând cont de pereți (MEA efectiv)
    // -------------------------------------------------------

    /**
     * MEA efectiv ținând cont de distanța față de pereți.
     * Un robot aproape de perete are MEA mai mic în direcția peretelui.
     *
     * @param targetPos     poziția țintei
     * @param shooterPos    poziția trăgătorului
     * @param bulletPower   puterea glonțului
     * @param battleField   arena
     * @return MEA efectiv (poate fi mai mic decât MEA teoretic)
     */
    public static double calculateEffective(Point2D.Double targetPos,
                                            Point2D.Double shooterPos,
                                            double bulletPower,
                                            BattleField battleField) {
        double mea           = calculate(bulletPower);
        double bulletSpeed   = bulletSpeed(bulletPower);
        double distance      = targetPos.distance(shooterPos);

        // Timpul de zbor al glonțului (ticks)
        double flightTime = distance / bulletSpeed;

        // Distanța maximă pe care o poate parcurge robotul lateral
        double maxLateralDist = MAX_ROBOT_VELOCITY * flightTime;

        // Distanța față de pereți în direcție laterală
        double wallDist = battleField.distanceToWall(targetPos);

        // Limităm MEA dacă robotul e aproape de perete
        if (wallDist < maxLateralDist) {
            double wallConstrainedMEA = Math.atan2(wallDist, distance);
            mea = Math.min(mea, wallConstrainedMEA);
        }

        return mea;
    }

    // -------------------------------------------------------
    // Conversii GuessFactor <-> unghi
    // -------------------------------------------------------

    /**
     * Convertește un GuessFactor [-1, 1] în unghi relativ față
     * de bearing-ul direct shooter -> target.
     *
     * GF = 0  → unghi 0 (direct)
     * GF = +1 → MEA în direcția laterală pozitivă
     * GF = -1 → MEA în direcția laterală negativă
     *
     * @param guessFactor   valoarea GF în [-1, 1]
     * @param bulletPower   puterea glonțului
     * @param lateralSign   semnul vitezei laterale (+1 sau -1)
     * @return unghiul relativ față de bearing direct (radiani)
     */
    public static double gfToAngleOffset(double guessFactor,
                                         double bulletPower,
                                         int lateralSign) {
        double mea = calculate(bulletPower);
        return guessFactor * mea * lateralSign;
    }

    /**
     * Convertește un unghi relativ în GuessFactor [-1, 1].
     *
     * @param angleOffset   unghiul relativ față de bearing direct
     * @param bulletPower   puterea glonțului
     * @param lateralSign   semnul vitezei laterale
     * @return GuessFactor în [-1, 1]
     */
    public static double angleOffsetToGF(double angleOffset,
                                         double bulletPower,
                                         int lateralSign) {
        double mea = calculate(bulletPower);
        if (mea == 0) return 0;
        double gf = angleOffset / mea / lateralSign;
        return MathUtils.clamp(gf, -1.0, 1.0);
    }

    // -------------------------------------------------------
    // Viteza glonțului
    // -------------------------------------------------------

    /**
     * Viteza unui glonț pentru o putere dată.
     * Formula Robocode: speed = 20 - 3 * power
     */
    public static double bulletSpeed(double bulletPower) {
        return 20.0 - 3.0 * bulletPower;
    }

    /**
     * Damage-ul unui glonț pentru o putere dată.
     * Formula Robocode: damage = 4 * power (+ 2 * (power - 1) dacă power > 1)
     */
    public static double bulletDamage(double bulletPower) {
        double damage = 4.0 * bulletPower;
        if (bulletPower > 1.0) {
            damage += 2.0 * (bulletPower - 1.0);
        }
        return damage;
    }

    /**
     * Energia recuperată când lovești cu un glonț de puterea dată.
     * Formula Robocode: energyRegained = 3 * power
     */
    public static double energyRegained(double bulletPower) {
        return 3.0 * bulletPower;
    }

    /**
     * Gun heat generat la tragere.
     * Formula Robocode: heat = 1 + power / 5
     */
    public static double gunHeatGenerated(double bulletPower) {
        return 1.0 + bulletPower / 5.0;
    }

    /**
     * Timpul minim între două focuri consecutive (ticks).
     * Gun cooling rate implicit = 0.1/tick.
     */
    public static double minTimeBetweenShots(double bulletPower,
                                             double gunCoolingRate) {
        return gunHeatGenerated(bulletPower) / gunCoolingRate;
    }
}