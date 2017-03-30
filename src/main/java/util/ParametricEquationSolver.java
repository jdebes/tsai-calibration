package util;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;

/**
 * Created by jdeb860 on 3/30/2017.
 */
public class ParametricEquationSolver {

    // Returns solved t values in [x,y,z] = 0 = c + t*d
    public static RealVector parametricZeroInterceptTSolve(RealVector constantVector, RealVector parallelVector) {
       int vectorSize = parallelVector.getDimension();
       double[] realSolvedVector = new double[vectorSize];

       for (int i = 0; i < vectorSize; i++) {
            realSolvedVector[i] = -constantVector.getEntry(i) / parallelVector.getEntry(i);
       }

       return MatrixUtils.createRealVector(realSolvedVector);
    }

    // Solve parametric equation given t to obtain point on the line
    public static RealVector solvePointGivenT(RealVector constantVector, RealVector parallelVector, double scaleFactorT) {
        return constantVector.add(parallelVector.mapMultiply(scaleFactorT));
    }
}
