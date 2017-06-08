package tsai;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FilenameUtils;
import tsai.util.SSDUtils;
import tsai.util.TsaiCalibUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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
            app.startStereo();
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


        }
    }

    private void startStereo() {
        System.out.println("######### Building Disparity Map #########");
        String pathWithoutExt = FilenameUtils.removeExtension(leftImagePath);
        leftBufferedImage = readImage(leftImagePath);
        rightBufferedImage = readImage(rightImagePath);

        List matchedPairs = SSDUtils.ssdMatch(leftBufferedImage, rightBufferedImage, WINDOW_SIZE);
        BufferedImage depthMapImage = SSDUtils.buildDisparityImage(matchedPairs, leftBufferedImage, WINDOW_SIZE);

        writeImage(pathWithoutExt + "_dmap.jpg", depthMapImage);
    }
}
