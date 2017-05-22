package tsai.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FilenameUtils;

import java.io.FileWriter;
import java.util.List;

/**
 * Created by jonas on 15/05/17.
 */
public class CsvUtil {
    public static void listToCsv(List<List<String>> csvList, String name, String inputFilePath) {
        CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");
        String fileName = FilenameUtils.getFullPath(inputFilePath) + FilenameUtils.getBaseName(inputFilePath) + "_" + name  + "." + FilenameUtils.getExtension(inputFilePath);

        FileWriter fileWriter = null;
        CSVPrinter csvFilePrinter = null;

        try {
            fileWriter = new FileWriter(fileName);
            csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);

            for (List<String> rowList: csvList) {
                csvFilePrinter.printRecord(rowList);
            }
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
}
