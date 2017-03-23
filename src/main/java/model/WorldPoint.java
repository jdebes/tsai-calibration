package model;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Created by jdeb860 on 3/23/2017.
 */
public class WorldPoint {
    private int id;
    private double x;
    private double y;
    private double z;
    private Point rawImagePoint;
    private Point2D.Double processedImagePoint;

    public WorldPoint(int id, double x, double y, double z, Point rawImagePoint) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rawImagePoint = rawImagePoint;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public Point getRawImagePoint() {
        return rawImagePoint;
    }

    public void setRawImagePoint(Point rawImagePoint) {
        this.rawImagePoint = rawImagePoint;
    }

    public Point2D.Double getProcessedImagePoint() {
        return processedImagePoint;
    }

    public void setProcessedImagePoint(Point2D.Double processedImagePoint) {
        this.processedImagePoint = processedImagePoint;
    }
}
