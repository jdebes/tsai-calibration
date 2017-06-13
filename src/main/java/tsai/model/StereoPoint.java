package tsai.model;

import java.awt.*;

/**
 * Created by jonas on 13/06/17.
 */
public class StereoPoint {
    private Point left;
    private Point right;
    private double rightError;

    public StereoPoint(Point left, Point right) {
        this.left = left;
        this.right = right;
    }

    public StereoPoint() {

    }

    public Point getLeft() {
        return left;
    }

    public void setLeft(Point left) {
        this.left = left;
    }

    public Point getRight() {
        return right;
    }

    public void setRight(Point right) {
        this.right = right;
    }

    public double getRightError() {
        return rightError;
    }

    public void setRightError(double rightError) {
        this.rightError = rightError;
    }
}
