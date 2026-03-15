package man.utils;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;


public class AStarNavigator {

    private static final int GRID_COLS = 20;
    private static final int GRID_ROWS = 20;

    private static final double MAX_ROBOT_VELOCITY = 8.0;

    private final BattleField battleField;
    private final double      cellWidth;
    private final double      cellHeight;

    private double[][] dangerMap;
    private static class Node implements Comparable<Node> {
        int col, row;
        double gCost;
        double hCost;
        Node   parent;

        Node(int col, int row, double gCost, double hCost, Node parent) {
            this.col    = col;
            this.row    = row;
            this.gCost  = gCost;
            this.hCost  = hCost;
            this.parent = parent;
        }

        double fCost() { return gCost + hCost; }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.fCost(), other.fCost());
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Node)) return false;
            Node n = (Node) o;
            return col == n.col && row == n.row;
        }

        @Override
        public int hashCode() {
            return col * 31 + row;
        }
    }


    public AStarNavigator(BattleField battleField) {
        this.battleField = battleField;
        this.cellWidth   = battleField.width  / GRID_COLS;
        this.cellHeight  = battleField.height / GRID_ROWS;
        this.dangerMap   = new double[GRID_COLS][GRID_ROWS];
    }


    public void clearDangerMap() {
        for (int c = 0; c < GRID_COLS; c++) {
            for (int r = 0; r < GRID_ROWS; r++) {
                dangerMap[c][r] = 0;
            }
        }
    }

    public void markWaveDanger(Wave wave, long currentTime) {
        Point2D.Double origin    = wave.fireLocation;
        double         waveAngle = wave.absoluteBearingAtFire;
        double         mea       = wave.maxEscapeAngle;

        for (int c = 0; c < GRID_COLS; c++) {
            for (int r = 0; r < GRID_ROWS; r++) {
                Point2D.Double cellCenter = cellToWorld(c, r);
                double bearing = MathUtils.absoluteBearing(origin, cellCenter);
                double offset  = MathUtils.normalRelativeAngle(bearing - waveAngle);

                double normalizedOffset = mea == 0 ? 0 : Math.abs(offset) / mea;

                if (normalizedOffset <= 1.0) {
                    double danger = 1.0 - normalizedOffset;
                    double dist   = origin.distance(cellCenter);
                    danger *= (1.0 / (1.0 + dist * 0.001));
                    dangerMap[c][r] += danger;
                }
            }
        }
    }

    public void markWallDanger() {
        for (int c = 0; c < GRID_COLS; c++) {
            for (int r = 0; r < GRID_ROWS; r++) {
                Point2D.Double center  = cellToWorld(c, r);
                double         wallDist = battleField.distanceToWall(center);
                if (wallDist < 80) {
                    dangerMap[c][r] += (80 - wallDist) / 80.0 * 2.0;
                }
            }
        }
    }


    public Point2D.Double findSafestDestination(Point2D.Double myPosition,
                                                Wave closestWave,
                                                long currentTime) {

        double maxReachDist = Double.POSITIVE_INFINITY;

        if (closestWave != null) {
            double distToWave   = closestWave.fireLocation.distance(myPosition)
                    - closestWave.radius(currentTime);
            double timeToImpact = Math.max(1, distToWave / closestWave.bulletSpeed);
            maxReachDist = timeToImpact * MAX_ROBOT_VELOCITY;
        }

        int    goalCol   = 0, goalRow = 0;
        double minDanger = Double.POSITIVE_INFINITY;

        for (int c = 0; c < GRID_COLS; c++) {
            for (int r = 0; r < GRID_ROWS; r++) {
                Point2D.Double cellCenter = cellToWorld(c, r);
                double distToCell = myPosition.distance(cellCenter);

                if (distToCell > maxReachDist) continue;

                if (dangerMap[c][r] < minDanger) {
                    minDanger = dangerMap[c][r];
                    goalCol   = c;
                    goalRow   = r;
                }
            }
        }

        return runAStar(myPosition, goalCol, goalRow);
    }


    public Point2D.Double findSafestDestination(Point2D.Double myPosition) {
        return findSafestDestination(myPosition, null, 0);
    }


    public Point2D.Double runAStar(Point2D.Double myPosition,
                                   int goalCol, int goalRow) {
        int[] startCell = worldToCell(myPosition);
        int startCol    = startCell[0];
        int startRow    = startCell[1];

        if (startCol == goalCol && startRow == goalRow) {
            return cellToWorld(goalCol, goalRow);
        }

        PriorityQueue<Node>  openSet  = new PriorityQueue<>();
        Map<Integer, Node>   cameFrom = new HashMap<>();
        Map<Integer, Double> gScore   = new HashMap<>();

        int startKey = key(startCol, startRow);
        int goalKey  = key(goalCol,  goalRow);

        Node startNode = new Node(startCol, startRow, 0,
                heuristic(startCol, startRow, goalCol, goalRow), null);
        openSet.add(startNode);
        gScore.put(startKey, 0.0);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            int  currKey = key(current.col, current.row);

            if (currKey == goalKey) {
                return firstStepInPath(current, startCol, startRow, myPosition);
            }

            for (int[] neighbor : getNeighbors(current.col, current.row)) {
                int nc = neighbor[0];
                int nr = neighbor[1];
                int nk = key(nc, nr);

                double moveCost   = MathUtils.distance(
                        cellToWorld(current.col, current.row),
                        cellToWorld(nc, nr));
                double danger     = dangerMap[nc][nr];
                double tentativeG = gScore.getOrDefault(currKey, Double.MAX_VALUE)
                        + moveCost + danger * 50.0;

                if (tentativeG < gScore.getOrDefault(nk, Double.MAX_VALUE)) {
                    gScore.put(nk, tentativeG);
                    Node neighborNode = new Node(nc, nr, tentativeG,
                            heuristic(nc, nr, goalCol, goalRow), current);
                    openSet.add(neighborNode);
                    cameFrom.put(nk, current);
                }
            }
        }

        return cellToWorld(goalCol, goalRow);
    }


    private Point2D.Double firstStepInPath(Node goalNode,
                                           int startCol, int startRow,
                                           Point2D.Double myPosition) {
        List<Node> path    = new ArrayList<>();
        Node       current = goalNode;
        while (current != null) {
            path.add(current);
            current = current.parent;
        }
        Collections.reverse(path);

        if (path.size() > 1) {
            Node next = path.get(1);
            return cellToWorld(next.col, next.row);
        }
        return cellToWorld(goalNode.col, goalNode.row);
    }

    private double heuristic(int c1, int r1, int c2, int r2) {
        return MathUtils.distance(cellToWorld(c1, r1), cellToWorld(c2, r2));
    }

    private List<int[]> getNeighbors(int col, int row) {
        List<int[]> neighbors = new ArrayList<>();
        int[] dc = {-1, -1, -1, 0, 0,  1, 1, 1};
        int[] dr = {-1,  0,  1, -1, 1, -1, 0, 1};
        for (int i = 0; i < 8; i++) {
            int nc = col + dc[i];
            int nr = row + dr[i];
            if (nc >= 0 && nc < GRID_COLS && nr >= 0 && nr < GRID_ROWS) {
                neighbors.add(new int[]{nc, nr});
            }
        }
        return neighbors;
    }


    public int[] worldToCell(Point2D.Double point) {
        int col = (int) MathUtils.clamp(point.x / cellWidth,  0, GRID_COLS - 1);
        int row = (int) MathUtils.clamp(point.y / cellHeight, 0, GRID_ROWS - 1);
        return new int[]{col, row};
    }

    public Point2D.Double cellToWorld(int col, int row) {
        double x = (col + 0.5) * cellWidth;
        double y = (row + 0.5) * cellHeight;
        return new Point2D.Double(x, y);
    }

    private int key(int col, int row) { return col * GRID_ROWS + row; }


    public double[][] getDangerMap() { return dangerMap; }
    public int getGridCols()         { return GRID_COLS; }
    public int getGridRows()         { return GRID_ROWS; }
}