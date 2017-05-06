import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FilenameUtils;

/**
 * Created by jonas on 5/05/17.
 */
public class Application {
    private static final String PROPERTIES_FILE_NAME= "application.properties";

    private final String inputFilePath;
    private final String inputParamsPath;
    private final boolean isStereo;

    private Application(String inputFilePath, String inputParamsPath, boolean isStereo) {
        this.inputFilePath = inputFilePath;
        this.inputParamsPath = inputParamsPath;
        this.isStereo = isStereo;
    }

    private static Application getInstance() {
        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(params.properties().setFileName(PROPERTIES_FILE_NAME));

        try {
            Configuration config = builder.getConfiguration();
            return new Application(config.getString("input.file"), config.getString("input.params"), config.getBoolean("input.stereo"));
        } catch(ConfigurationException cex) {
            System.err.println("Unable to read " + PROPERTIES_FILE_NAME);
        }

        return null;
    }

    public static void main(String[] args) {
        Application app = Application.getInstance();
        if (app != null) {
            app.start(args);
        }
    }

    private void start(String[] args) {
        TsaiCalib tsaiCalib = new TsaiCalib(inputFilePath, inputParamsPath);
        tsaiCalib.start();

        if (isStereo) {
            String withoutFile = FilenameUtils.getFullPath(inputFilePath);
            String leftFileName = FilenameUtils.getName(inputFilePath);
            String rightFileName = leftFileName.replaceFirst("left", "right");

            TsaiCalib tsaiCalibRight = new TsaiCalib(withoutFile + rightFileName, inputParamsPath);
            tsaiCalibRight.start();
        }
    }
}
