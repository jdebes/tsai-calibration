package tsai.model;

import java.awt.*;

/**
 * Created by jonas on 28/05/17.
 */
public class MatchedPair {
    Point leftPoint;
    Point rightPoint;

    public MatchedPair(Point leftPoint, Point rightPoint) {
        this.leftPoint = leftPoint;
        this.rightPoint = rightPoint;
    }

    public Point getLeftPoint() {
        return leftPoint;
    }

    public void setLeftPoint(Point leftPoint) {
        this.leftPoint = leftPoint;
    }

    public Point getRightPoint() {
        return rightPoint;
    }

    public void setRightPoint(Point rightPoint) {
        this.rightPoint = rightPoint;
    }
}
