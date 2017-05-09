package tsai.model;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by jdeb860 on 3/24/2017.
 */
public class RotationTranslation {
    private double transX;
    private double transY;
    private double transZ;

    private double[] r1;
    private double[] r2;
    private double[] r3;

    public RealMatrix getRotationTranslationMatrix() {
        List<RealVector> vectors = getRotationVectors();
        vectors.set(0, vectors.get(0).append(transX));
        vectors.set(1, vectors.get(1).append(transY));
        vectors.set(2, vectors.get(2).append(transZ));

        RealMatrix matrix = MatrixUtils.createRealMatrix(4,4);

        for (int i = 0; i < vectors.size(); i++) {
            matrix.setRow(i,vectors.get(i).toArray());
        }
        matrix.setRow(vectors.size(), new double[] {0,0,0,1});

        return matrix;
    }

    public RealMatrix getRotationMatrix() {
        List<RealVector> vectors = getRotationVectors();
        vectors.set(0, vectors.get(0).append(0));
        vectors.set(1, vectors.get(1).append(0));
        vectors.set(2, vectors.get(2).append(0));

        RealMatrix matrix = MatrixUtils.createRealMatrix(4,4);

        for (int i = 0; i < vectors.size(); i++) {
            matrix.setRow(i,vectors.get(i).toArray());
        }
        matrix.setRow(vectors.size(), new double[] {0,0,0,1});

        return matrix;
    }

    public RealVector getTranslationVector(){
        double[] transVector = {-transX, -transY, -transZ};
        return MatrixUtils.createRealVector(transVector);
    }

    private List<RealVector> getRotationVectors() {
        List<RealVector> rotationVectorsList = new ArrayList<RealVector>();
        rotationVectorsList.add(MatrixUtils.createRealVector(r1));
        rotationVectorsList.add(MatrixUtils.createRealVector(r2));
        rotationVectorsList.add(MatrixUtils.createRealVector(r3));
        return  rotationVectorsList;
    }

    public void printRotationTranslationMatrix() {
        List<RealVector> vectors = getRotationVectors();
        System.out.println("Rotation Translation Matrix");
        System.out.println(Arrays.toString(vectors.get(0).append(transX).toArray()).replaceAll("[\\]\\[]", ""));
        System.out.println(Arrays.toString(vectors.get(1).append(transY).toArray()).replaceAll("[\\]\\[]", ""));
        System.out.println(Arrays.toString(vectors.get(2).append(transZ).toArray()).replaceAll("[\\]\\[]", ""));
    }

    public double getTransX() {
        return transX;
    }

    public void setTransX(double transX) {
        this.transX = transX;
    }

    public double getTransY() {
        return transY;
    }

    public void setTransY(double transY) {
        this.transY = transY;
    }

    public double getTransZ() {
        return transZ;
    }

    public void setTransZ(double transZ) {
        this.transZ = transZ;
    }

    public double[] getR1() {
        return r1;
    }

    public void setR1(double[] r1) {
        this.r1 = r1;
    }

    public double[] getR2() {
        return r2;
    }

    public void setR2(double[] r2) {
        this.r2 = r2;
    }

    public double[] getR3() {
        return r3;
    }

    public void setR3(double[] r3) {
        this.r3 = r3;
    }
}
