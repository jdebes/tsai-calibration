package tsai;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.linear.RealMatrix;
import tsai.model.MatchedPair;
import tsai.util.Rectification;
import tsai.util.SSDUtils;
import tsai.util.TsaiCalibUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jonas on 5/05/17.
 */
public class Application {
    private static final String PROPERTIES_FILE_NAME= "application.properties";
    private static final int WINDOW_SIZE= 6;

    private final String inputFilePath;
    private final String inputParamsPath;
    private final boolean isStereo;
    private BufferedImage leftBufferedImage;
    private BufferedImage rightBufferedImage;
    private final String leftImagePath;
    private final String rightImagePath;

    private Application(String inputFilePath, String inputParamsPath, boolean isStereo, String leftImagePath, String rightImagePath) {
        this.inputFilePath = inputFilePath;
        this.inputParamsPath = inputParamsPath;
        this.isStereo = isStereo;
        this.leftImagePath = leftImagePath;
        this.rightImagePath = rightImagePath;
    }

    private static Application getInstance() {
        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(params.properties().setFileName(PROPERTIES_FILE_NAME));

        try {
            Configuration config = builder.getConfiguration();
            return new Application(config.getString("input.file"), config.getString("input.params"), config.getBoolean("input.stereo"), config.getString("input.leftImagePath"), config.getString("input.rightImagePath"));
        } catch(ConfigurationException cex) {
            System.err.println("Unable to read " + PROPERTIES_FILE_NAME);
        }

        return null;
    }

    public static void main(String[] args) {
        Application app = Application.getInstance();
        if (app != null) {
            //app.start(args);
            //app.startStereo();
            app.startMultiStereo(4);
        }

    }


    public BufferedImage readImage(String path) {
        BufferedImage bufferedImage = null;

        try {
            bufferedImage = ImageIO.read(new File(path));
        } catch (IOException e) {
            System.out.println("Unable to read image " + path);
        }

        return bufferedImage;
    }

    public void writeImage(String path, BufferedImage bufferedImage) {
        try {
            File depthMapFile = new File(path);
            ImageIO.write(bufferedImage, "jpg", depthMapFile);
        } catch (IOException e) {
            System.out.println("Unable to write depthmap to file");
        }
    }

    private void start(String[] args) {
        TsaiCalib tsaiCalib = new TsaiCalib(inputFilePath, inputParamsPath);
        tsaiCalib.start();

        if (isStereo) {
            System.out.println("######### Right Stereo #########");
            String withoutFile = FilenameUtils.getFullPath(inputFilePath);
            String leftFileName = FilenameUtils.getName(inputFilePath);
            String rightFileName = leftFileName.replaceFirst("left", "right");
            String rightFilePath = withoutFile + rightFileName;

            TsaiCalib tsaiCalibRight = new TsaiCalib(rightFilePath, inputParamsPath);
            tsaiCalibRight.start();

            System.out.println("######### Stereo Results #########");
            double baseline = TsaiCalibUtils.calculateStereoBaseline(tsaiCalib, tsaiCalibRight);
            System.out.println("Baseline: " + baseline);
            double optimisedBaseline = TsaiCalibUtils.calculateOptimisedStereoBaseline(tsaiCalib, tsaiCalibRight);
            System.out.println("OBaseline: " + optimisedBaseline);

            //List<Vector3D> triangulatedPoints = TsaiCalibUtils.getTriangulatedEstimated3DPoints(tsaiCalib, tsaiCalibRight);
            TsaiCalibUtils.getTriangulatedEstimated3DError(tsaiCalib, tsaiCalibRight, false);
            TsaiCalibUtils.getTriangulatedEstimated3DError(tsaiCalib, tsaiCalibRight, true);

            RealMatrix fundamentalMatrix = TsaiCalibUtils.computeFundamentalMatrix(tsaiCalib, tsaiCalibRight, true);
            double[] leftEpipole = TsaiCalibUtils.findEpipoles(fundamentalMatrix, true);

            Rectification.buildRRect(leftEpipole);

        }
    }

    private void startStereo() {
        System.out.println("######### Building Disparity Map #########");
        String pathWithoutExt = FilenameUtils.removeExtension(leftImagePath);
        leftBufferedImage = readImage(leftImagePath);
        rightBufferedImage = readImage(rightImagePath);

        List<MatchedPair> matchedPairs = new ArrayList<>();
        SSDUtils.ssdMatch(leftBufferedImage, rightBufferedImage, WINDOW_SIZE, leftBufferedImage.getHeight(), 0, matchedPairs);

        BufferedImage depthMapImage = SSDUtils.buildDisparityImage(matchedPairs, leftBufferedImage, WINDOW_SIZE);

        writeImage(pathWithoutExt + "_dmap.jpg", depthMapImage);
    }

    private void startMultiStereo(float threads) {
        System.out.println("######### Building Disparity Map Threaded #########");
        leftBufferedImage = readImage(leftImagePath);
        rightBufferedImage = readImage(rightImagePath);
        String pathWithoutExt = FilenameUtils.removeExtension(leftImagePath);

        List<MatchedPair> matchedPairs1 = new ArrayList<>();

        StereoRunnable stereoRunnable1 = new StereoRunnable(matchedPairs1, leftBufferedImage, rightBufferedImage, WINDOW_SIZE, 0, (int)(leftBufferedImage.getHeight() / threads));

        Thread t1 = new Thread(stereoRunnable1);
        t1.start();

        List<List<MatchedPair>> allMatchedPairs = new ArrayList<>();
        List<Thread> threadList = new ArrayList<>();
        for (int i = 1; i <= threads; i++) {
            List<MatchedPair> matchedPairs = new ArrayList<>();

            float start =  leftBufferedImage.getHeight() * (i / threads);
            float end = leftBufferedImage.getHeight() * ((i+1) / threads);

            StereoRunnable stereoRunnable = new StereoRunnable(matchedPairs, leftBufferedImage, rightBufferedImage, WINDOW_SIZE, Math.round(start), Math.round(end));
            Thread t = new Thread(stereoRunnable);
            t.start();

            threadList.add(t);
            allMatchedPairs.add(matchedPairs);
        }

        try {
            t1.join();

            for (Thread thread : threadList) {
                thread.join();
            }

        } catch (InterruptedException e) {
            System.out.println("Thread Interrupted");
        }

        for (List<MatchedPair> mp : allMatchedPairs) {
            matchedPairs1.addAll(mp);
        }

        BufferedImage depthMapImage = SSDUtils.buildDisparityImage(matchedPairs1, leftBufferedImage, WINDOW_SIZE);

        writeImage(pathWithoutExt + "_dmap.jpg", depthMapImage);
    }
}
