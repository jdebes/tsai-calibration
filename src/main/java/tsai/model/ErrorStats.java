package tsai.model;

/**
 * Created by jdeb860 on 4/4/2017.
 */
public class ErrorStats {
    private double errorMagSum;
    private double averageError;
    private double errorStdDev;

    public ErrorStats() {
    }

    public double getErrorMagSum() {
        return errorMagSum;
    }

    public void setErrorMagSum(double errorMagSum) {
        this.errorMagSum = errorMagSum;
    }

    public double getAverageError() {
        return averageError;
    }

    public void setAverageError(double averageError) {
        this.averageError = averageError;
    }

    public double getErrorStdDev() {
        return errorStdDev;
    }

    public void setErrorStdDev(double errorStdDev) {
        this.errorStdDev = errorStdDev;
    }

    public void printStats() {
        System.out.println("Average Error, " + this.averageError );
        System.out.println("Average StdDev, " + this.errorStdDev );
    }
}
