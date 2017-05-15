package tsai.util;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;

/**
 * Created by jdeb860 on 3/30/2017.
 */
public class ErrorAnalysisSolver {

    public static double calculateError(double estimatedValue, double knownValue) {
        return estimatedValue - knownValue;
    }

    public static RealVector calculateVectorError(RealVector estimatedValue, RealVector knownValue) {
        return estimatedValue.subtract(knownValue);
    }

    public static double calculateVectorErrorMagnitude(RealVector estimatedValue, RealVector knownValue) {
        return calculateVectorError(estimatedValue, knownValue).getNorm();
    }

    public static RealVector vector3DToRealVector(Vector3D vector3D) {
        return MatrixUtils.createRealVector(vector3D.toArray());
    }
}
