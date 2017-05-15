package tsai.util;

import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import tsai.TsaiCalib;
import tsai.model.WorldPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by jonas on 6/05/17.
 */
public class TsaiCalibUtils {

    public static List<Double> calculateDisparity(TsaiCalib left, TsaiCalib right) {
        List<WorldPoint> leftPoints = left.getCalibrationPoints();
        List<WorldPoint> rightPoints = right.getCalibrationPoints();

        List<Double> disparityList = new ArrayList<>();

        for (int i = 0; i < left.getCalibrationPoints().size(); i++) {
            RealVector difference = leftPoints.get(i).getProcessedImagePointVector().subtract(rightPoints.get(i).getProcessedImagePointVector());
            disparityList.add(difference.getNorm());
        }

        return disparityList;
    }

    public static double calculateStereoBaseline(TsaiCalib left, TsaiCalib right) {
        return left.getCameraOriginWorldFrame().getDistance(right.getCameraOriginWorldFrame());
    }

    public static List<Vector3D> getTriangulatedEstimated3DPoints(TsaiCalib left, TsaiCalib right, boolean useOptimised) {
        List<WorldPoint> leftPoints = left.getCalibrationPoints();
        List<WorldPoint> rightPoints = right.getCalibrationPoints();

        List<Vector3D> estimatedPoints = new ArrayList<>();

        for (int i = 0; i < left.getCalibrationPoints().size(); i++) {
            Line leftRay = left.getRay(leftPoints.get(i));
            Line rightRay = right.getRay(rightPoints.get(i));

            if (useOptimised) {
                leftRay = left.getRay(leftPoints.get(i), left.getOptimiseRow().getF(), left.getOptimisedRotationTranslation(), left.getOptimiseRow().getK1());
                rightRay = right.getRay(rightPoints.get(i), right.getOptimiseRow().getF(), right.getOptimisedRotationTranslation(), right.getOptimiseRow().getK1());
            }

            Vector3D triangulatedPoint = leftRay.intersection(rightRay);
            double distance = leftRay.distance(rightRay);

            // If no rays do not intersect find midpoint between two closest
            if (triangulatedPoint == null) {
                Vector3D closestLeftPoint = leftRay.closestPoint(rightRay);
                Vector3D closestRightPoint = rightRay.closestPoint(leftRay);

                Vector3D leftRightPointSum = closestLeftPoint.add(closestRightPoint);
                Vector3D closestMidPoint = leftRightPointSum.scalarMultiply(0.5);
                triangulatedPoint = closestMidPoint;
            }

            estimatedPoints.add(triangulatedPoint);
        }

        return estimatedPoints;
    }

    public static void getTriangulatedEstimated3DError(TsaiCalib left, TsaiCalib right, boolean useOptimised) {
        List<WorldPoint> leftPoints = left.getCalibrationPoints();
        List<Vector3D> triangulatedPoints = getTriangulatedEstimated3DPoints(left,right, useOptimised);

        double[] errorArray = new double[left.getCalibrationPoints().size()];

        for (int i = 0; i < leftPoints.size(); i++) {
            errorArray[i] = ErrorAnalysisSolver.calculateVectorErrorMagnitude(ErrorAnalysisSolver.vector3DToRealVector(triangulatedPoints.get(i)), leftPoints.get(i).getWorldPointVector());
        }

        double meanError = Arrays.stream(errorArray).average().orElse(-1);
        StandardDeviation standardDeviation = new StandardDeviation();
        double stdDev = standardDeviation.evaluate(errorArray);

        System.out.println("--------");
        System.out.println("Triangulated Errors backproject");
        System.out.println("Mean Error mm:" + meanError);
        System.out.println("Stdev: " + stdDev);

    }
}
