package man.utils;

import man.utils.Wave;

import java.awt.geom.Point2D;

public interface WaveHitListener {
    void onWaveHit(Wave wave, Point2D.Double myPosition, long time);
}