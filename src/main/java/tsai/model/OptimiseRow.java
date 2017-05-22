package tsai.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jonas on 15/05/17.
 */
public class OptimiseRow {
    private final double k1;
    private final double f;
    private final double tZ;
    private final double error;
    private final double stdDev;
    private final List<Double> errorList;

    public OptimiseRow(double k1, double f, double tZ, double error, double stdDev, List<Double> errorList) {
        this.k1 = k1;
        this.f = f;
        this.tZ = tZ;
        this.error = error;
        this.stdDev = stdDev;
        this.errorList = errorList;
    }

    public double getK1() {
        return k1;
    }

    public double getF() {
        return f;
    }

    public double gettZ() {
        return tZ;
    }

    public double getError() {
        return error;
    }

    public double getStdDev() {
        return stdDev;
    }

    public void printStats() {
        System.out.println("Optimisation Complete: ");
        System.out.println("K1: " + k1);
        System.out.println("focal" + f);
        System.out.println("Tz: " + tZ);
        System.out.println("Avg Error (Px): " + error);
        System.out.println("Error StdDev: " + stdDev);
    }

    public List<String> toCsvList() {
        List<String> csvList = new ArrayList<>();
        csvList.add(String.valueOf(error));
        csvList.add(String.valueOf(stdDev));
        csvList.add(String.valueOf(k1));
        csvList.add(String.valueOf(f));
        csvList.add(String.valueOf(tZ));
        return csvList;
    }

    public List<Double> getErrorList() {
        return errorList;
    }
}
