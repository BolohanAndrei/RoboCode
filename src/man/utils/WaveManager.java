package man.utils;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//manages all active waves
public class WaveManager {

    public interface WaveHitListener {
        //called when the wave has reached myPosition
        void onWaveHit(Wave wave, Point2D.Double myPosition, long time);
    }

    // State
    private final List<Wave> activeWaves;
    private final List<WaveHitListener> listeners;

    // Constructor
    public WaveManager() {
        this.activeWaves = new ArrayList<>();
        this.listeners = new ArrayList<>();
    }

    // Managing Listeners
    public void addListener(WaveHitListener listener) {
        listeners.add(listener);
    }

    public void removeListener(WaveHitListener listener) {
        listeners.remove(listener);
    }

    public void addWave(Wave wave) {
        activeWaves.add(wave);
    }

    // Update — every tick
    public void update(Point2D.Double myPosition, long currentTime) {
        Iterator<Wave> it = activeWaves.iterator();

        while (it.hasNext()) {
            Wave wave = it.next();

            // verify if wave has reached
            if (wave.hasReached(myPosition, currentTime)) {
                // notify listeners
                for (WaveHitListener listener : listeners) {
                    listener.onWaveHit(wave, myPosition, currentTime);
                }
            }

            // delete wave when passed
            if (wave.hasPassed(myPosition, currentTime)) {
                wave.passed = true;
                it.remove();
            }
        }
    }

    // All active waves
    public List<Wave> getActiveWaves() {
        return new ArrayList<>(activeWaves);
    }

    // closest wave
    public Wave getClosestWave(Point2D.Double myPosition, long currentTime) {
        Wave closest = null;
        double closestDist = Double.POSITIVE_INFINITY;

        for (Wave wave : activeWaves) {
            double waveRadius = wave.radius(currentTime);
            double distToMe = wave.fireLocation.distance(myPosition);
            double distToWave = Math.abs(distToMe - waveRadius);

            if (distToWave < closestDist) {
                closestDist = distToWave;
                closest = wave;
            }
        }
        return closest;
    }

    // closest n waves from us
    public List<Wave> getClosestWaves(Point2D.Double myPosition,
                                      long currentTime,
                                      int maxCount) {
        List<Wave> result = new ArrayList<>();
        List<Wave> copy = new ArrayList<>(activeWaves);

        for (int i = 0; i < maxCount && !copy.isEmpty(); i++) {
            Wave closest= null;
            double closestDist = Double.POSITIVE_INFINITY;

            for (Wave wave : copy) {
                double waveRadius = wave.radius(currentTime);
                double distToMe = wave.fireLocation.distance(myPosition);
                double distToWave = Math.abs(distToMe - waveRadius);

                if (distToWave < closestDist) {
                    closestDist = distToWave;
                    closest = wave;
                }
            }

            if (closest != null) {
                result.add(closest);
                copy.remove(closest);
            }
        }
        return result;
    }

    public void clear() {
        activeWaves.clear();
    }

    public int size() {
        return activeWaves.size();
    }

    @Override
    public String toString() {
        return "WaveManager[activeWaves=" + activeWaves.size() + "]";
    }
}