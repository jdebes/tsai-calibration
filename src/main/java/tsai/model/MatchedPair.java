package tsai.model;

import java.awt.*;

/**
 * Created by jonas on 28/05/17.
 */
public class MatchedPair {
    Point leftPoint;
    Point rightPoint;
    int xDisparity;
    double normalisedXDisparity;

    public MatchedPair(Point leftPoint, Point rightPoint, int xDisparity) {
        this.leftPoint = leftPoint;
        this.rightPoint = rightPoint;
        this.xDisparity = xDisparity;
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

    public int getxDisparity() {
        return xDisparity;
    }

    public void setxDisparity(int xDisparity) {
        this.xDisparity = xDisparity;
    }

    public double getNormalisedXDisparity() {
        return normalisedXDisparity;
    }

    public void setNormalisedXDisparity(double normalisedXDisparity) {
        this.normalisedXDisparity = normalisedXDisparity;
    }
}
