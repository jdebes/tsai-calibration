package tsai;

import org.apache.commons.math3.geometry.Vector;
import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3DFormat;
import tsai.model.ErrorStats;
import tsai.model.Point3D;
import tsai.model.RotationTranslation;
import tsai.model.WorldPoint;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import tsai.util.ErrorAnalysisSolver;
import tsai.util.ParametricEquationSolver;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by jdeb860 on 3/23/2017.
 */
public class TsaiCalib {
    private static final String[] FILE_HEADER_MAPPING = {"id","wx","wy","wz","px", "py"};
    private static final String[] PARAMS_HEADER_MAPPING = {"desc", "value"};

    //Additional Known Values
    private final String inputFilePath;
    private final String inputParamsPath;

    private List<WorldPoint> calibrationPoints = new ArrayList<WorldPoint>();
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

    private double k1D3;
    private double k1D2;

    public TsaiCalib(String inputFilePath, String inputParamsPath) {
        this.inputFilePath = inputFilePath;
        this.inputParamsPath = inputParamsPath;
    }

    public void start() {
        try {
            CSVFormat csvFileFormat =  CSVFormat.DEFAULT.withHeader(FILE_HEADER_MAPPING);
            FileReader fileReader = new FileReader(inputFilePath);
            CSVParser csvFileParser = new CSVParser(fileReader, csvFileFormat);
            List<CSVRecord> csvRecords = csvFileParser.getRecords();

            for (CSVRecord inputPoint : csvRecords) {
                Point rawImagePoint = new Point(Integer.parseInt(inputPoint.get("px")), Integer.parseInt(inputPoint.get("py")));
                WorldPoint worldPoint = new WorldPoint(Integer.parseInt(inputPoint.get(0)), Double.parseDouble(inputPoint.get(1)), Double.parseDouble(inputPoint.get(2)), Double.parseDouble(inputPoint.get(3)), rawImagePoint);
                calibrationPoints.add(worldPoint);
            }

            csvFileFormat =  CSVFormat.DEFAULT.withHeader(PARAMS_HEADER_MAPPING);
            fileReader = new FileReader(inputParamsPath);
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

        doK1();
        this.k1D3 = getAvgK1(true);
        this.k1D2 = getAvgK1(false);

       // double finalErrorAverage = optimiseFTzK(rotationTranslation, focalLength, rotationTranslation.getTransZ(), 0, 300);

        printStats();

        writeCalibrationPointsToCSV();

        return;

    }

    private double optimiseFTzK(RotationTranslation rotationTranslation, double initialF, double initialTz, double intialK, int numIterations) {

        double percentIncrease = 0.00001;
        List<Double> optimisedErrorAvg = new ArrayList<>();

        for (int f = -numIterations; f < numIterations; f++ ) {
            for (int t = -numIterations; t < numIterations; t++ ) {
               // for (int k = -numIterations; k < numIterations; k++ ) {
                    List<Double> errorList = new ArrayList<>();

    //                        double r = Math.pow(worldPoint.getProcessedImagePoint().getX(), 2.0) + Math.pow(worldPoint.getProcessedImagePoint().getY(), 2.0);
    //                        double rXp = rotationTranslation.getR1()[0] * worldPoint.getX() + rotationTranslation.getR1()[1] * worldPoint.getY() + rotationTranslation.getR1()[2] * worldPoint.getZ() + rotationTranslation.getTranslationVector().getEntry(0);
    //                        double rYp = rotationTranslation.getR2()[0] * worldPoint.getX() + rotationTranslation.getR2()[1] * worldPoint.getY() + rotationTranslation.getR2()[2] * worldPoint.getZ() + rotationTranslation.getTranslationVector().getEntry(1);
    //                        double rYz = rotationTranslation.getR3()[0] * worldPoint.getX() + rotationTranslation.getR3()[1] * worldPoint.getY() + rotationTranslation.getR3()[2] * worldPoint.getZ() + (initialTz + initialTz * percentIncrease * t);
    //
    //                        double AdjustedK = intialK +  intialK * percentIncrease * k;
    //                        double AdjustedF = initialF + initialF * percentIncrease * f;
    //                        double xd = ((AdjustedF * f * rXp) / rYz) - worldPoint.getEstimatedProcessedImagePoint().getX() * AdjustedK * r;
    //                        double yd = ((AdjustedF * f * rYp) / rYz) - worldPoint.getEstimatedProcessedImagePoint().getY() * AdjustedK * r;

    //                        double[] adjustedVector = {xd, yd};
    //                        RealVector adjustedRealVector = MatrixUtils.createRealVector(adjustedVector);
    //
    //                        double[] ydVector = {worldPoint.getEstimatedProcessedImagePoint().getX(), worldPoint.getEstimatedProcessedImagePoint().getY()};
    //                        RealVector realYdVector = MatrixUtils.createRealVector(ydVector);

                    RealMatrix realTransMatrix2DInv = MatrixUtils.createRealMatrix(transMatrix2DInv);
                    RealMatrix realTransMatrix2D = MatrixUtils.inverse(realTransMatrix2DInv);

                    rotationTranslation.setTransZ(initialTz + initialTz * percentIncrease * t);

                    double AdjustedF = initialF + initialF * percentIncrease * f;
                    RealMatrix realRotationTranslationMatrix = rotationTranslation.getRotationTranslationMatrix();


                    final double[][] focalMatrix = {
                            {this.sX * AdjustedF,0,0,0},
                            {0,AdjustedF,0,0},
                            {0,0,1,0},
                    };
                    RealMatrix realFocalMatrix = MatrixUtils.createRealMatrix(focalMatrix);

                    for (WorldPoint worldPoint : calibrationPoints) {
                        RealVector realWorldVector = worldPoint.getWorldPointVector().append(1.0);
                        RealVector estimated2dHomoVector = realTransMatrix2D.operate(realFocalMatrix.operate(realRotationTranslationMatrix.operate(realWorldVector)));

                        double[] estimatedPoint = new double[] { Math.round(estimated2dHomoVector.getEntry(0) / estimated2dHomoVector.getEntry(2)), Math.round(estimated2dHomoVector.getEntry(1) / estimated2dHomoVector.getEntry(2)) };
                        RealVector estimatedRealPoint = MatrixUtils.createRealVector(estimatedPoint);

                        double[] rawImagePoint = {worldPoint.getRawImagePoint().getX(), worldPoint.getRawImagePoint().getY()};
                        RealVector realRawImagePoint = MatrixUtils.createRealVector(rawImagePoint);


                        errorList.add(realRawImagePoint.subtract(estimatedRealPoint).getNorm());
                    }

                    optimisedErrorAvg.add(errorList.stream().mapToDouble(x -> x).average().getAsDouble());
                //}
            }
        }

        return optimisedErrorAvg.stream().min(Double::compare).orElse(null);
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

        /*        w => z homo to 2d
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

        RealMatrix realRotationTranslationMatrixInv = MatrixUtils.inverse(rotationTranslation.getRotationTranslationMatrix());

        RealVector realCameraOriginWorldFrame = getCameraOriginWorldFrame();

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
                wp.setD3Error(errorMag);
            } else {
                errorMag =  WorldPoint.calculateErrorMagnitude(wp.getRawImagePoint(), wp.getEstimatedProcessedImagePoint());
                wp.setD2Error(errorMag);
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
        System.out.println("K1: " + this.k1D3);

    }

    private void writeCalibrationPointsToCSV() {
        CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");
        String fileName = FilenameUtils.getFullPath(inputFilePath) + FilenameUtils.getBaseName(inputFilePath) + "_results." + FilenameUtils.getExtension(inputFilePath);

        FileWriter fileWriter = null;
        CSVPrinter csvFilePrinter = null;

        try {
            fileWriter = new FileWriter(fileName);
            csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);

            for (WorldPoint wp : calibrationPoints) {
                csvFilePrinter.printRecord(wp.toCsvList());
            }

            printPadding();
            System.out.println("Results written to " + fileName);

        } catch (Exception e) {
            System.out.println("Error writing results to csv");
        }

        try {
            fileWriter.flush();
            fileWriter.close();
            csvFilePrinter.close();
        } catch (Exception e) {
            System.out.println("Cant close writing resources");
        }
    }


    private void printPadding() {
        System.out.println("--------");
    }

    private double calculateK1(double avgError, double estimatedDistance) {
        return avgError / Math.pow(estimatedDistance,3);
    }

    private void doK1() {
        for (WorldPoint wp : calibrationPoints) {
            wp.setD3K1(calculateK1(wp.getD3Error(), wp.getWorldPointVector().getNorm()));
            wp.setD2K1(calculateK1(wp.getD2Error(), wp.getProcessedImagePointVector().getNorm()));
        }
    }

    private double getAvgK1(boolean backProjected) {
        double sum = 0;
        for (WorldPoint wp : calibrationPoints) {
            if (backProjected) {
                sum += wp.getD3K1();
            } else {
                sum += wp.getD2K1();
            }

        }

        return sum / numPoints;
    }

    public RealVector getCameraOriginWorldFrame() {
        RealMatrix realRotationTranslationMatrixInv = MatrixUtils.inverse(rotationTranslation.getRotationTranslationMatrix());

        double[] cameraOrigin = {0,0,0,1};
        RealVector realCameraOrigin = MatrixUtils.createRealVector(cameraOrigin);
        RealVector realCameraOriginWorldFrame = realRotationTranslationMatrixInv.operate(realCameraOrigin);

        return  realCameraOriginWorldFrame;
    }

    public List<WorldPoint> getCalibrationPoints() {
        return calibrationPoints;
    }

    public Line getRay(WorldPoint worldPoint) {
        final double[][] focalMatrixInv = {
                {1/(this.sX * focalLength),0,0},
                {0,1/(focalLength),0},
                {0,0,1},
                {0,0,1}
        };
        RealMatrix realFocalMatrixInv = MatrixUtils.createRealMatrix(focalMatrixInv);


        RealVector cameraOriginPoint = getCameraOriginWorldFrame();
        Vector3D cameraOrigin3D = new Vector3D(cameraOriginPoint.getEntry(0), cameraOriginPoint.getEntry(1), cameraOriginPoint.getEntry(2));

        RealMatrix realRotationTranslationMatrixInv = MatrixUtils.inverse(rotationTranslation.getRotationTranslationMatrix());

        double[] rawImageVector = {worldPoint.getProcessedImagePoint().getX(), worldPoint.getProcessedImagePoint().getY(), 1.0};
        RealVector realRawImageVector = MatrixUtils.createRealVector(rawImageVector);
        RealVector estimatedImagePoint = realRotationTranslationMatrixInv.operate(realFocalMatrixInv.operate(realRawImageVector));

        Vector3D estimatedImagePoint3D = new Vector3D(estimatedImagePoint.getEntry(0), estimatedImagePoint.getEntry(1), estimatedImagePoint.getEntry(2));

        return new Line(cameraOrigin3D, estimatedImagePoint3D, 1.0E-10D);
    }
}