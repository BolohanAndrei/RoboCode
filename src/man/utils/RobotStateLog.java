package man.utils;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class RobotStateLog {

    private static final int MAX_SIZE = 300;

    private final List<RobotState> states;

    public RobotStateLog() {
        this.states = new ArrayList<>();
    }

    // Add / Delete

    public void addState(RobotState state) {
        states.add(state);
        //cut if too big
        if (states.size() > MAX_SIZE) {
            states.remove(0);
        }
    }

    public void clear() {
        states.clear();
    }

    // Access
    public int size() {
        return states.size();
    }

    public boolean isEmpty() {
        return states.isEmpty();
    }

    // Most recent state
    public RobotState latest() {
        if (states.isEmpty()) return null;
        return states.get(states.size() - 1);
    }

    //oldest state
    public RobotState get(int index) {
        return states.get(index);
    }

    //state after tick(exact time)
    public RobotState getAtTime(long time) {
        for (int i = states.size() - 1; i >= 0; i--) {
            if (states.get(i).time == time) {
                return states.get(i);
            }
        }
        return null;
    }

    //closest state from a given tick
    public RobotState getClosestToTime(long time) {
        if (states.isEmpty()) return null;
        RobotState best = states.get(0);
        long bestDiff = Math.abs(best.time - time);
        for (RobotState s : states) {
            long diff = Math.abs(s.time - time);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = s;
            }
        }
        return best;
    }

    public List<RobotState> getAll() {
        return new ArrayList<>(states);
    }

    // Utility for MovementPredictor

    //return the state with N ticks behind from the last state
    public RobotState getNTicksAgo(int n) {
        if (states.isEmpty()) return null;
        RobotState latest = latest();
        long targetTime = latest.time - n;
        return getClosestToTime(targetTime);
    }

    //distance made in the last N ticks
    public double distanceTraveledInLastNTicks(int n) {
        if (states.size() < 2) return 0;
        RobotState recent = latest();
        RobotState old = getNTicksAgo(n);
        if (old == null) return 0;
        return recent.distanceTo(old);
    }

    //avg speed in the last N ticks
    public double averageVelocityInLastNTicks(int n) {
        int count = 0;
        double sum = 0;
        int startIdx = Math.max(0, states.size() - n);
        for (int i = startIdx; i < states.size(); i++) {
            sum += Math.abs(states.get(i).velocity);
            count++;
        }
        return count == 0 ? 0 : sum / count;
    }

    @Override
    public String toString() {
        return "RobotStateLog[size=" + states.size() + "]";
    }
}