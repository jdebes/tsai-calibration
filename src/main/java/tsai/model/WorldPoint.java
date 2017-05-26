package tsai.model;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

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
    private Point estimatedProcessedImagePoint;
    private Point3D estimatedWorldPoint;
    private double d3Error;
    private double d2Error;
    private double d3K1;
    private double d2K1;

    public WorldPoint(int id, double x, double y, double z, Point rawImagePoint) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rawImagePoint = rawImagePoint;
    }

    public static double calculateErrorMagnitude(Point rawImagePoint, Point estimatedProcessedImagePoint) {
        double xEr = estimatedProcessedImagePoint.getX() - rawImagePoint.getX();
        double yEr = estimatedProcessedImagePoint.getY() - rawImagePoint.getY();
        double[] errorVector = {xEr, yEr};

        RealVector realErrorVector = MatrixUtils.createRealVector(errorVector);

        return realErrorVector.getNorm();
    }

    public double getProcessedImagePointNormAsVector() {
        return Math.pow(processedImagePoint.getX(),2) + Math.pow(processedImagePoint.getY(),2);
    }

    public RealVector getWorldPointVector() {
        double[] v = {this.x, this.y, this.z};
        return MatrixUtils.createRealVector(v);
    }

    public RealVector getProcessedImagePointVector() {
        double[] v = { this.processedImagePoint.x, this.processedImagePoint.y };
        return MatrixUtils.createRealVector(v);
    }

    public Point getEstimatedProcessedImagePoint() {
        return this.estimatedProcessedImagePoint;
    }

    public RealVector getEstimatedProcessedImagePointVector() {
        double[] vector = { this.estimatedProcessedImagePoint.x, this.estimatedProcessedImagePoint.y};
        return MatrixUtils.createRealVector(vector);
    }

    public void setEstimatedProcessedImagePoint(Point estimatedProcessedImagePoint) {
        this.estimatedProcessedImagePoint = estimatedProcessedImagePoint;
    }

    public void setEstimatedProcessedImagePoint(RealVector homogeneous3DVector) {
        double w = homogeneous3DVector.getEntry(2);
        this.estimatedProcessedImagePoint = new Point((int) Math.round(homogeneous3DVector.getEntry(0) / w), (int) Math.round(homogeneous3DVector.getEntry(1) / w));

    }

    public List<String> toCsvList() {
        List<String> csvList = new ArrayList<>();
        //csvList.add(String.valueOf(this.id));
        csvList.add(String.valueOf(this.x));
        csvList.add(String.valueOf(this.y));
        csvList.add(String.valueOf(this.z));
        csvList.add(String.valueOf(this.estimatedWorldPoint.getX()));
        csvList.add(String.valueOf(this.estimatedWorldPoint.getY()));
        csvList.add(String.valueOf(this.estimatedWorldPoint.getZ()));
        csvList.add("##");
        csvList.add(String.valueOf(this.rawImagePoint.x));
        csvList.add(String.valueOf(this.rawImagePoint.y));
        csvList.add(String.valueOf(this.estimatedProcessedImagePoint.x));
        csvList.add(String.valueOf(this.estimatedProcessedImagePoint.y));
        csvList.add("##");
        csvList.add(String.valueOf(this.getD3Error()));
        csvList.add(String.valueOf(this.getD2Error()));
        csvList.add(String.valueOf(this.getD3K1()));
        return csvList;
    }

    public RealVector getRealRawImagePointVector() {
        double[] v = {rawImagePoint.x,rawImagePoint.y,1};
        return MatrixUtils.createRealVector(v);
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

    public Point.Double getRawImagePointD() {
        return new Point2D.Double(rawImagePoint.getX(), rawImagePoint.getY());
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

    public Point3D getEstimatedWorldPoint() {
        return estimatedWorldPoint;
    }

    public void setEstimatedWorldPoint(Point3D estimatedWorldPoint) {
        this.estimatedWorldPoint = estimatedWorldPoint;
    }

    public double getD3Error() {
        return d3Error;
    }

    public void setD3Error(double d3Error) {
        this.d3Error = d3Error;
    }

    public double getD2Error() {
        return d2Error;
    }

    public void setD2Error(double d2Error) {
        this.d2Error = d2Error;
    }

    public double getD3K1() {
        return d3K1;
    }

    public void setD3K1(double d3K1) {
        this.d3K1 = d3K1;
    }

    public double getD2K1() {
        return d2K1;
    }

    public void setD2K1(double d2K1) {
        this.d2K1 = d2K1;
    }
}
