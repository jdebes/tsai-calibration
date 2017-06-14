package tsai.model;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jonas on 13/06/17.
 */
public class StereoPoint {
    private Point left;
    private Point right;
    private double rightError;
    private MatchedPair matchedPair;

    public StereoPoint(Point left, Point right) {
        this.left = left;
        this.right = right;
    }

    public StereoPoint() {

    }

    public List<String> getAsCsvRow() {
        List<String> csvRow = new ArrayList<>();

        csvRow.add(String.valueOf(left.getX()));
        csvRow.add(String.valueOf(left.getY()));

        csvRow.add(String.valueOf(right.getX()));
        csvRow.add(String.valueOf(right.getY()));

        csvRow.add(String.valueOf(matchedPair.getRightPoint().getX()));
        csvRow.add(String.valueOf(matchedPair.getRightPoint().getY()));

        csvRow.add(String.valueOf(rightError));

        return csvRow;
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

    public MatchedPair getMatchedPair() {
        return matchedPair;
    }

    public void setMatchedPair(MatchedPair matchedPair) {
        this.matchedPair = matchedPair;
    }

    public static List<List<String>> getStereoPointsAsCsv(List<StereoPoint> stereoPoints) {
        List<List<String>> csvList = new ArrayList<>();

        stereoPoints.forEach(point -> csvList.add(point.getAsCsvRow()));

        return csvList;
    }
}
