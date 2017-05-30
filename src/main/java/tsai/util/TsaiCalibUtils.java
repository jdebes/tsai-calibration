package tsai.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import tsai.TsaiCalib;
import tsai.model.WorldPoint;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    public static double calculateOptimisedStereoBaseline(TsaiCalib left, TsaiCalib right) {
        return left.getCameraOriginWorldFrame(left.getOptimisedRotationTranslation(), left.getOptimiseRow().getF()).getDistance(right.getCameraOriginWorldFrame(right.getOptimisedRotationTranslation(), right.getOptimiseRow().getF()));
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

    /**
     * Compute fundamental matrix using SVD and 8 point algorithm
     * @param left tsaiCalib object
     * @param right tsaiCalib object
     * @return singular values
     */
    public static RealMatrix computeFundamentalMatrix(TsaiCalib left, TsaiCalib right, boolean normalise) {
        List<WorldPoint> leftPoints = left.getCalibrationPoints();
        List<WorldPoint> rightPoints = right.getCalibrationPoints();

        NormalisationResult leftResult = normalisePoints(leftPoints);
        NormalisationResult rightResult = normalisePoints(leftPoints);

        List<RealVector> normalisedLeft = leftResult.getNormalisedPoints();
        List<RealVector> normalisedRight = rightResult.getNormalisedPoints();

        RealMatrix aMatrix = MatrixUtils.createRealMatrix(leftPoints.size(), 9);

        for (int i = 0; i < leftPoints.size(); i++) {
            Point.Double leftPoint = leftPoints.get(i).getRawImagePointD();
            Point.Double rightPoint = rightPoints.get(i).getRawImagePointD();

            if (normalise) {
               leftPoint = new Point.Double(normalisedLeft.get(i).getEntry(0) / normalisedLeft.get(i).getEntry(2),
                       normalisedLeft.get(i).getEntry(1) / normalisedLeft.get(i).getEntry(2));
               rightPoint = new Point.Double(normalisedRight.get(i).getEntry(0) / normalisedRight.get(i).getEntry(2),
                       normalisedRight.get(i).getEntry(1) / normalisedRight.get(i).getEntry(2));
            }


            double[] row = {leftPoint.getX()*rightPoint.getX(), leftPoint.getX()*rightPoint.getY(), leftPoint.getX(),
                    leftPoint.getY()*rightPoint.getX(), leftPoint.getY()*rightPoint.getY(), leftPoint.getY(), rightPoint.getX(), rightPoint.getY(), 1};
            aMatrix.setRow(i, row);
        }

        //Initial Result | A = USV^T
        SingularValueDecomposition initialSingularValueDecomposition = new SingularValueDecomposition(aMatrix);
        RealMatrix sMatrix = initialSingularValueDecomposition.getS();

        if (normalise) {
            double minValue = Arrays.stream(initialSingularValueDecomposition.getSingularValues()).min().orElse(0);

            // Set min diag to zero
            sMatrix.setColumn(ArrayUtils.indexOf(initialSingularValueDecomposition.getSingularValues(), minValue), new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0});

            RealMatrix finalMatrix = initialSingularValueDecomposition.getU().multiply(initialSingularValueDecomposition.getS()).multiply(initialSingularValueDecomposition.getVT());
            SingularValueDecomposition finalSingularValueDecomposition = new SingularValueDecomposition(finalMatrix);

            double[][] normalisedFundamentalMatrix = singularValuesToMatrix(finalSingularValueDecomposition.getSingularValues());
            RealMatrix unNormalisedFinalMatrix = MatrixUtils.inverse(rightResult.getTransformMatrix()).multiply(MatrixUtils.createRealMatrix(normalisedFundamentalMatrix)).multiply(MatrixUtils.inverse(leftResult.getTransformMatrix()));

            return unNormalisedFinalMatrix;
        } else {
            double[][] fundamentalMatrix =  singularValuesToMatrix(initialSingularValueDecomposition.getSingularValues());
            findRightEpipolarLine(MatrixUtils.createRealMatrix(fundamentalMatrix), leftPoints.get(0).getRawImagePointArray(), rightPoints.get(0).getRawImagePointArray());

            return MatrixUtils.createRealMatrix(fundamentalMatrix);
        }
    }

    public static double[][] singularValuesToMatrix(double[] singularValues) {
        double[][] fundamentalMatrix = new double[3][3];

        int num = 0;
        for (int i = 0; i < fundamentalMatrix.length; i++) {
            for (int j = 0; j < fundamentalMatrix.length; j++) {
                fundamentalMatrix[i][j] = singularValues[num];
                num++;
            }
        }

        return fundamentalMatrix;
    }

    public static void findEpipoles(RealMatrix fundamentalMatrix) {
        SingularValueDecomposition singularValueDecomposition = new SingularValueDecomposition(fundamentalMatrix);

        int zeroIndex = -1;
        for (int i =0 ; i < singularValueDecomposition.getSingularValues().length; i++) {
            if (singularValueDecomposition.getSingularValues()[i] == 0) {
                zeroIndex = i;
            }
        }

        double[] leftEpipole = singularValueDecomposition.getV().getColumn(zeroIndex);
        double[] rightEpipole = singularValueDecomposition.getU().getColumn(zeroIndex);
    }

    public static void findRightEpipolarLine(RealMatrix fundamentalMatrix, double[] leftPoint, double[] rightPoint) {
        RealMatrix realMatrix = MatrixUtils.createRealMatrix(3,1);
        realMatrix.setColumn(0, rightPoint);
        realMatrix = realMatrix.transpose();

        double[] result = fundamentalMatrix.operate(leftPoint);
    }

    public static NormalisationResult normalisePoints(List<WorldPoint> points) {
        double mX = points.stream().collect(Collectors.averagingDouble((wp) -> wp.getRawImagePoint().getX()));
        double mY = points.stream().collect(Collectors.averagingDouble((wp) -> wp.getRawImagePoint().getY()));
        RealVector mVector = MatrixUtils.createRealVector(new double[] {mX, mY, 1});

        double d = 0;
        for (int i = 0; i < points.size(); i++) {
            d += points.get(i).getRealRawImagePointVector().subtract(mVector).getNorm();
        }

        d *= 1/(points.size() * Math.sqrt(2));

        double[][] transformM = new double[][] {
            {1.0/d, 0.0, -mX/d},
            {0, 1.0/d, -mY/d},
            {0, 0, 1}
        };

        RealMatrix realMatrix = MatrixUtils.createRealMatrix(transformM);
        List<RealVector> normalisedPoints = points.stream().map(wp -> realMatrix.operate(wp.getRealRawImagePointVector())).collect(Collectors.toList());

        return new NormalisationResult(normalisedPoints, realMatrix);
    }

    private static class NormalisationResult {
        private List<RealVector> normalisedPoints;
        private RealMatrix transformMatrix;

        public NormalisationResult(List<RealVector> normalisedPoints, RealMatrix transformMatrix) {
            this.normalisedPoints = normalisedPoints;
            this.transformMatrix = transformMatrix;
        }

        public List<RealVector> getNormalisedPoints() {
            return normalisedPoints;
        }

        public RealMatrix getTransformMatrix() {
            return transformMatrix;
        }
    }

}
