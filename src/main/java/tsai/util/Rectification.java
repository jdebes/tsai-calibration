package tsai.util;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * Created by jonas on 8/06/17.
 */
public class Rectification {
    public static RealMatrix buildRRect(Vector3D epipole) {
        Vector3D r1 = epipole;
        double[] opticalAxis = new double[]{0,0,1};

        Vector3D opticalAxisVector = new Vector3D(opticalAxis);

        Vector3D r2 = r1.crossProduct(opticalAxisVector);
        Vector3D r3 = r1.crossProduct(r2);

        RealMatrix rRect = MatrixUtils.createRealMatrix(3,3);
        rRect.setRow(0, r1.toArray());
        rRect.setRow(1, r2.toArray());
        rRect.setRow(2, r3.toArray());

        return rRect;
    }

    public static void buildRRect(double[] epipole) {
        //Normalise & homogenise
        epipole[0] = epipole[0] / epipole[2];
        epipole[1] = epipole[1] / epipole[2];
        epipole[2] = 1;

        buildRRect(new Vector3D(epipole));
    }
}
