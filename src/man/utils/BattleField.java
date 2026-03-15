package man.utils;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class BattleField {

    public final double width;
    public final double height;
    private final Rectangle2D.Double rect;

    private static final double MARGIN = 18.0;

    public BattleField(double width, double height) {
        this.width = width;
        this.height = height;
        this.rect = new Rectangle2D.Double(0, 0, width, height);
    }

    public boolean contains(Point2D.Double point) {
        return rect.contains(point);
    }

    public boolean containsSafe(Point2D.Double point) {
        return point.x >= MARGIN
                && point.x <= width - MARGIN
                && point.y >= MARGIN
                && point.y <= height - MARGIN;
    }

    public Point2D.Double clamp(Point2D.Double point) {
        double x = Math.max(MARGIN, Math.min(width - MARGIN, point.x));
        double y = Math.max(MARGIN, Math.min(height - MARGIN, point.y));
        return new Point2D.Double(x, y);
    }

    public double distanceToWall(Point2D.Double point) {
        double distLeft   = point.x;
        double distRight  = width - point.x;
        double distBottom = point.y;
        double distTop    = height - point.y;
        return Math.min(Math.min(distLeft, distRight),
                Math.min(distBottom, distTop));
    }

    public double normalizedWallDistance(Point2D.Double point) {
        return distanceToWall(point) / (Math.min(width, height) / 2.0);
    }

    public Point2D.Double getCenter() {
        return new Point2D.Double(width / 2.0, height / 2.0);
    }

    public Rectangle2D.Double getRect() {
        return rect;
    }

    @Override
    public String toString() {
        return "BattleField[" + width + "x" + height + "]";
    }
}