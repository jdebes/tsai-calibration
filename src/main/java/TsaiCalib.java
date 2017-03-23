import model.WorldPoint;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.linear.*;

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

}
