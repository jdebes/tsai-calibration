import model.WorldPoint;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

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
    private static final String INPUT_FILE_PATH = "C:\\Users\\jdeb860\\Desktop\\tsaiInput.csv";
    private static final String INPUT_PARAMS_PATH = "C:\\Users\\jdeb860\\Desktop\\params.csv";
    private static final String[] FILE_HEADER_MAPPING = {"id","wx","wy","wz","px", "py"};
    private static final String[] PARAMS_HEADER_MAPPING = {"desc", "value"};

    //Additional Known Values
    private int numPoints;
    private Point2D.Double imageCenter;
    private double pixelWidth;
    private double pixelHeight;

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

            numPoints = Integer.parseInt(csvRecords.get(0).get("value"));
            imageCenter = new Point2D.Double();
            imageCenter.x = Double.parseDouble(csvRecords.get(1).get("value"));
            imageCenter.y = Double.parseDouble(csvRecords.get(2).get("value"));
            pixelWidth = Double.parseDouble(csvRecords.get(3).get("value"));
            pixelHeight = Double.parseDouble(csvRecords.get(4).get("value"));

        } catch (IOException e) {
            System.out.println("Failed to parse csv");
        }

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
            absTy = absTy;
        } else {
            ty = -absTy;
        }

        final RealVector realEstimatedParamVectorAdjustedSxTy = realEstimatedParamVector.mapMultiply(ty);
        realEstimatedParamVectorAdjustedSxTy.setEntry(0, realEstimatedParamVectorAdjustedSxTy.getEntry(0) / sX);
        realEstimatedParamVectorAdjustedSxTy.setEntry(1, realEstimatedParamVectorAdjustedSxTy.getEntry(1) / sX);
        realEstimatedParamVectorAdjustedSxTy.setEntry(2, realEstimatedParamVectorAdjustedSxTy.getEntry(2) / sX);
        realEstimatedParamVectorAdjustedSxTy.setEntry(3, realEstimatedParamVectorAdjustedSxTy.getEntry(3) / sX);

        double[] r1 = realEstimatedParamVectorAdjustedSxTy.getSubVector(0,3).toArray();
        double[] r2 = realEstimatedParamVectorAdjustedSxTy.getSubVector(4,3).toArray();

        final Vector3D r1V = new Vector3D(r1);
        final Vector3D r2V= new Vector3D(r2);
        final Vector3D r3V = r1V.crossProduct(r2V);

        return;

    }

    private void convertImagePixelsToMilli() {
        double[][] transMatrix = {
                {-pixelWidth, 0, pixelWidth*imageCenter.getX()},
                {0, -pixelHeight, pixelHeight*imageCenter.getY(),},
                {0, 0, 1}
        };
        RealMatrix realTransMatrix = MatrixUtils.createRealMatrix(transMatrix);

        for (WorldPoint worldPoint: calibrationPoints) {
            Point rawPoint = worldPoint.getRawImagePoint();
            double[] imageVector = {rawPoint.getX(),rawPoint.getY(), 1};
            RealVector realImageVector = MatrixUtils.createRealVector(imageVector);

            Point2D.Double processedImagePoint = new Point2D.Double();
            RealVector processedImageVector = realTransMatrix.operate(realImageVector);

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

}
