package model;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;

/**
 * Created by jdeb860 on 4/4/2017.
 */
public class Point3D {
    private double x;
    private double y;
    private double z;

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

    public static Point3D convertFromRealVector(RealVector vector) {
        Point3D point3D = new Point3D();
        point3D.setX(vector.getEntry(0));
        point3D.setY(vector.getEntry(1));
        point3D.setZ(vector.getEntry(2));
        return point3D;
    }

    public RealVector getAsRealVector() {
        double[] v = {this.x, this.y, this.z};
        return MatrixUtils.createRealVector(v);
    }
}
