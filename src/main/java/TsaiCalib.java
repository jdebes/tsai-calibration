import model.ErrorStats;
import model.Point3D;
import model.RotationTranslation;
import model.WorldPoint;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import util.ErrorAnalysisSolver;
import util.ParametricEquationSolver;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by jdeb860 on 3/23/2017.
 */
public class TsaiCalib {
    private List<WorldPoint> calibrationPoints = new ArrayList<WorldPoint>();
    private static final String INPUT_FILE_PATH = "C:\\Users\\jdeb860\\Desktop\\tsai-data\\ImageEric\\tsaiInput.csv";
    private static final String INPUT_PARAMS_PATH = "C:\\Users\\jdeb860\\Desktop\\tsai-data\\ImageEric\\params.csv";
    private static final String[] FILE_HEADER_MAPPING = {"id","wx","wy","wz","px", "py"};
    private static final String[] PARAMS_HEADER_MAPPING = {"desc", "value"};

    //Additional Known Values
    private int numPoints;
    private Point2D.Double imageCenter;
    private double pixelWidth;
    private double pixelHeight;
    private RotationTranslation rotationTranslation = new RotationTranslation();
    private double focalLength;
    private double estimatedDistance;
    private double[][] transMatrix2DInv;
    private double sX;

    // 3D to 2D Error
    private ErrorStats errors3to2 = new ErrorStats();

    private ErrorStats errors2to3 = new ErrorStats();



    public static void main(String[] args) {
        TsaiCalib tsaiCalib = new TsaiCalib();
        tsaiCalib.start(args);
    }

    private void start(String[] args) {
        try {
            CSVFormat csvFileFormat =  CSVFormat.DEFAULT.withHeader(FILE_HEADER_MAPPING);
            FileReader fileReader = new FileReader(INPUT_FILE_PATH);
            CSVParser csvFileParser = new CSVParser(fileReader, csvFileFormat);
            List<CSVRecord> csvRecords = csvFileParser.getRecords();

            for (CSVRecord inputPoint : csvRecords) {
                Point rawImagePoint = new Point(Integer.parseInt(inputPoint.get("px")), Integer.parseInt(inputPoint.get("py")));
                WorldPoint worldPoint = new WorldPoint(Integer.parseInt(inputPoint.get(0)), Double.parseDouble(inputPoint.get(1)), Double.parseDouble(inputPoint.get(2)), Double.parseDouble(inputPoint.get(3)), rawImagePoint);
                calibrationPoints.add(worldPoint);
            }

            csvFileFormat =  CSVFormat.DEFAULT.withHeader(PARAMS_HEADER_MAPPING);
            fileReader = new FileReader(INPUT_PARAMS_PATH);
            csvFileParser = new CSVParser(fileReader, csvFileFormat);
            csvRecords = csvFileParser.getRecords();

            numPoints = calibrationPoints.size();
            imageCenter = new Point2D.Double();
            imageCenter.x = Double.parseDouble(csvRecords.get(0).get("value"));
            imageCenter.y = Double.parseDouble(csvRecords.get(1).get("value"));
            pixelWidth = Double.parseDouble(csvRecords.get(2).get("value"));
            pixelHeight = Double.parseDouble(csvRecords.get(3).get("value"));

        } catch (IOException e) {
            System.out.println("Failed to parse csv");
        }

        transMatrix2DInv = new double[][]{
                {-pixelWidth, 0, pixelWidth * imageCenter.getX()},
                {0, -pixelHeight, pixelHeight * imageCenter.getY(),},
                {0, 0, 1}
        };

        convertImagePixelsToMilli();

        RealVector realEstimatedParamVector = calculateLByPinv();

        double absTy = 1 / realEstimatedParamVector.getSubVector(4,3).getNorm();
        double sX = absTy * realEstimatedParamVector.getSubVector(0,3).getNorm();

        final RealVector realEstimatedParamVectorCancelTy = realEstimatedParamVector.mapMultiply(absTy);

        WorldPoint maxWorldPoint = getPointWithGreatestVectorNorm();

        double xti = realEstimatedParamVectorCancelTy.getSubVector(0,3).dotProduct(maxWorldPoint.getWorldPointVector()) + realEstimatedParamVectorCancelTy.getEntry(3);
        double yti = realEstimatedParamVectorCancelTy.getSubVector(4,3).dotProduct(maxWorldPoint.getWorldPointVector()) + absTy;

        double ty = 0;
        if ((xti < 0 && maxWorldPoint.getProcessedImagePoint().getX() < 0) && (yti < 0 && maxWorldPoint.getProcessedImagePoint().getY() < 0)) {
            ty = absTy;
        } else if ((xti > 0 && maxWorldPoint.getProcessedImagePoint().getX() > 0) && (yti > 0 && maxWorldPoint.getProcessedImagePoint().getY() > 0)) {
            ty = absTy;
        } else {
            ty = -absTy;
        }

        final RealVector realEstimatedParamVectorAdjustedSxTy = realEstimatedParamVector.mapMultiply(ty);
        realEstimatedParamVectorAdjustedSxTy.setEntry(0, realEstimatedParamVectorAdjustedSxTy.getEntry(0) / sX);
        realEstimatedParamVectorAdjustedSxTy.setEntry(1, realEstimatedParamVectorAdjustedSxTy.getEntry(1) / sX);
        realEstimatedParamVectorAdjustedSxTy.setEntry(2, realEstimatedParamVectorAdjustedSxTy.getEntry(2) / sX);
        realEstimatedParamVectorAdjustedSxTy.setEntry(3, realEstimatedParamVectorAdjustedSxTy.getEntry(3) / sX);

        rotationTranslation.setR1(realEstimatedParamVectorAdjustedSxTy.getSubVector(0,3).toArray());
        rotationTranslation.setR2(realEstimatedParamVectorAdjustedSxTy.getSubVector(4,3).toArray());

        final Vector3D r1V = new Vector3D(rotationTranslation.getR1());
        final Vector3D r2V= new Vector3D(rotationTranslation.getR2());
        final Vector3D r3V = r1V.crossProduct(r2V);

        rotationTranslation.setR3(r3V.toArray());


        rotationTranslation.setTransX(realEstimatedParamVectorAdjustedSxTy.getEntry(3));
        rotationTranslation.setTransY(ty);

        this.sX = sX;


        RealVector focalAndTz = calculateFocalAndTz(MatrixUtils.createRealVector(rotationTranslation.getR2()), MatrixUtils.createRealVector(rotationTranslation.getR3()));
        this.focalLength = focalAndTz.getEntry(0);
        rotationTranslation.setTransZ(focalAndTz.getEntry(1));

        this.estimatedDistance = rotationTranslation.getTranslationVector().getNorm();

        calculate3Dto2DProjectedPoints();
        double[] errorMagnitudes = calculateErrorSum(false);
        this.errors3to2.setAverageError(this.errors3to2.getErrorMagSum() / numPoints);
        StandardDeviation sd = new StandardDeviation();
        this.errors3to2.setErrorStdDev(sd.evaluate(errorMagnitudes));

        calculate2Dto3DProjectedPoints();
        double[] errorMagnitudes2to3 = calculateErrorSum(true);
        this.errors2to3.setAverageError(this.errors2to3.getErrorMagSum() / numPoints);
        sd = new StandardDeviation();
        this.errors2to3.setErrorStdDev(sd.evaluate(errorMagnitudes2to3));

        printStats();

        return;

    }

    private void convertImagePixelsToMilli() {
        RealMatrix realTransMatrix2DInv = MatrixUtils.createRealMatrix(transMatrix2DInv);

        for (WorldPoint worldPoint: calibrationPoints) {
            Point rawPoint = worldPoint.getRawImagePoint();
            double[] imageVector = {rawPoint.getX(),rawPoint.getY(), 1};
            RealVector realImageVector = MatrixUtils.createRealVector(imageVector);

            Point2D.Double processedImagePoint = new Point2D.Double();
            RealVector processedImageVector = realTransMatrix2DInv.operate(realImageVector);

            processedImagePoint.x = processedImageVector.getEntry(0);
            processedImagePoint.y = processedImageVector.getEntry(1);

            worldPoint.setProcessedImagePoint(processedImagePoint);
        }
    }

    private RealVector calculateLByPinv() {
        RealMatrix m = MatrixUtils.createRealMatrix(numPoints, 7);
        double[] vectorX = new double[numPoints];

        //Build matrix m row by row
        for (WorldPoint wp: calibrationPoints) {
            Point2D.Double pip = wp.getProcessedImagePoint();
            int i = wp.getId() - 1;

            double[] mRow = {pip.getY()*wp.getX(), pip.getY()*wp.getY(), pip.getY()*wp.getZ(), pip.getY(), -pip.getX()*wp.getX(), -pip.getX()*wp.getY(), -pip.getX()*wp.getZ()};
            m.setRow(i, mRow);

            vectorX[i] = pip.getX();
        }

        RealVector realVectorX = MatrixUtils.createRealVector(vectorX);

        // Dont need to X = M*L -> L = M^-1pseudo*X as Commons DecompositionSolver knows how to solve overdetermined system
        QRDecomposition QRDecomposition = new QRDecomposition(m);
        RealVector realVectorL = QRDecomposition.getSolver().solve(realVectorX);

        return realVectorL;
    }

    private WorldPoint getPointWithGreatestVectorNorm() {
        WorldPoint maxWorldPoint = calibrationPoints.get(0);
        for (WorldPoint worldPoint : calibrationPoints) {
            if (worldPoint.getProcessedImagePointNormAsVector() > maxWorldPoint.getProcessedImagePointNormAsVector()) {
                maxWorldPoint = worldPoint;
            }
        }

        return maxWorldPoint;
    }

    private RealVector calculateFocalAndTz(RealVector r2, RealVector r3) {
        RealMatrix m = MatrixUtils.createRealMatrix(numPoints, 2);
        double[] uZV = new double[numPoints];

        for (WorldPoint worldPoint : calibrationPoints) {
            RealVector worldVector = worldPoint.getWorldPointVector();
            int i = worldPoint.getId() - 1;

            double uY = r2.dotProduct(worldVector) + rotationTranslation.getTransY();
            double uZ = r3.dotProduct(worldVector);

            double[] row = {uY, -worldPoint.getProcessedImagePoint().getY()};
            uZV[i] = uZ*worldPoint.getProcessedImagePoint().getY();

            m.setRow(i, row);
        }

        QRDecomposition QRDecomposition = new QRDecomposition(m);
        RealVector realVectorParams = QRDecomposition.getSolver().solve(MatrixUtils.createRealVector(uZV));

        return  realVectorParams;
    }

    private void calculate3Dto2DProjectedPoints() {
        RealMatrix realTransMatrix2DInv = MatrixUtils.createRealMatrix(transMatrix2DInv);
        RealMatrix realTransMatrix2D = MatrixUtils.inverse(realTransMatrix2DInv);
        RealMatrix realRotationTranslationMatrix = rotationTranslation.getRotationTranslationMatrix();

        /*        
        f 0 0   same as 1 0 0  xc = (f * x) / w = x / (w / f)
        0 f 0           0 1 0
        0 0 1           0 0 1/f
        */
        final double[][] focalMatrix = {
            {this.sX * focalLength,0,0,0},
            {0,focalLength,0,0},
            {0,0,1,0},
        };
        RealMatrix realFocalMatrix = MatrixUtils.createRealMatrix(focalMatrix);

        for (WorldPoint worldPoint : calibrationPoints) {
            RealVector realWorldVector = worldPoint.getWorldPointVector().append(1.0);
            RealVector estimated2dHomoVector = realTransMatrix2D.operate(realFocalMatrix.operate(realRotationTranslationMatrix.operate(realWorldVector)));
            worldPoint.setEstimatedProcessedImagePoint(estimated2dHomoVector);
        }
    }

    private void calculate2Dto3DProjectedPoints() {
        final double[][] focalMatrixInv = {
                {1/(this.sX * focalLength),0,0},
                {0,1/(focalLength),0},
                {0,0,1},
                {0,0,1}
        };

        final double[][] focalMatrixInvRowPadding = {
                {1/(this.sX * focalLength),0,0,0},
                {0,1/(focalLength),0,0},
                {0,0,1,0},
                {0,0,0,1}
        };

        RealMatrix realFocalMatrixInv = MatrixUtils.createRealMatrix(focalMatrixInv);
        RealMatrix realFocalMatrixInvRowPadding = MatrixUtils.createRealMatrix(focalMatrixInvRowPadding);

        RealMatrix realTransMatrix2DInv = MatrixUtils.createRealMatrix(transMatrix2DInv);
        RealMatrix realRotationTranslationMatrixInv = MatrixUtils.inverse(rotationTranslation.getRotationTranslationMatrix());

        double[] cameraOrigin = {0,0,0,1};
        RealVector realCameraOrigin = MatrixUtils.createRealVector(cameraOrigin);
        RealVector realCameraOriginWorldFrame = realRotationTranslationMatrixInv.operate(realFocalMatrixInvRowPadding.operate(realCameraOrigin));

        for (WorldPoint wp: calibrationPoints) {
            double[] rawImageVector = {wp.getProcessedImagePoint().getX(), wp.getProcessedImagePoint().getY(), 1.0};
            RealVector realRawImageVector = MatrixUtils.createRealVector(rawImageVector);
            RealVector estimatedImagePoint = realRotationTranslationMatrixInv.operate(realFocalMatrixInv.operate(realRawImageVector));

            RealVector parallelVector = realCameraOriginWorldFrame.subtract(estimatedImagePoint);

            RealVector realSolvedTs = ParametricEquationSolver.parametricZeroInterceptTSolve(realCameraOriginWorldFrame, parallelVector);

            List<RealVector> solvedParametricPoints = new ArrayList<RealVector>();
            double minError = 0;
            RealVector minErrorVec = null;
            List<Double> parametricPointErrorMagnitudes = new ArrayList<Double>();
            for (int i = 0; i < realSolvedTs.getDimension(); i++) {
                RealVector solvedPoint = ParametricEquationSolver.solvePointGivenT(realCameraOriginWorldFrame, parallelVector, realSolvedTs.getEntry(i));
                solvedParametricPoints.add(solvedPoint);
                double error = ErrorAnalysisSolver.calculateVectorErrorMagnitude(solvedPoint.getSubVector(0,3), wp.getWorldPointVector());
                parametricPointErrorMagnitudes.add(error);

                if (Math.abs(error) < Math.abs(minError) || i == 0) {
                    minErrorVec = solvedPoint;
                    minError = error;
                }
            }

            wp.setEstimatedWorldPoint(Point3D.convertFromRealVector(minErrorVec));
        }

    }

    private double[] calculateErrorSum(boolean backProjected) {
        double[] errorMagnitudes = new double[numPoints];
        double errorMagSum = 0;
        for (WorldPoint wp : calibrationPoints) {
            double errorMag;
            if (backProjected) {
                errorMag = ErrorAnalysisSolver.calculateVectorErrorMagnitude(wp.getEstimatedWorldPoint().getAsRealVector(), wp.getWorldPointVector());
            } else {
                errorMag =  WorldPoint.calculateErrorMagnitude(wp.getRawImagePoint(), wp.getEstimatedProcessedImagePoint());
            }

            errorMagSum += errorMag;
            errorMagnitudes[wp.getId()-1] = errorMag;
        }

        if (backProjected) {
            this.errors2to3.setErrorMagSum(errorMagSum);
        } else {
            this.errors3to2.setErrorMagSum(errorMagSum);
        }

        return errorMagnitudes;
    }

    private void printStats() {
        rotationTranslation.printRotationTranslationMatrix();
        printPadding();
        System.out.println("Estimated Distance, " +  this.estimatedDistance);
        System.out.println("Estimated Focal Length, " + this.focalLength);
        System.out.println("sX, " + this.sX);
        printPadding();
        System.out.println("3D to 2D errors");
        this.errors3to2.printStats();
        printPadding();
        System.out.println("2D to 3D errors");
        this.errors2to3.printStats();

    }

    private void printPadding() {
        System.out.println("--------");
    }

}
