package tsai.util;

import tsai.model.MatchedPair;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jonas on 28/05/17.
 */
public class SSDUtils {

    public static List ssdMatch(BufferedImage leftImage, BufferedImage rightImage, int windowSize) {
        List<MatchedPair> matchedPoints = new ArrayList<>();
        for (int r = 0; r < leftImage.getHeight(); r++) {
            for (int c = 0; c < leftImage.getWidth(); c++) {
                long[] squaredDifferences = new long[leftImage.getWidth()];

                long minValue;
                Point minLocation = new Point(c, 0);
                Point leftPoint = new Point(c,r);
                squaredDifferences[0] = minValue = sumSearchWindows(leftImage, leftPoint, rightImage, minLocation, windowSize);

                for (int hCol = 1; hCol < leftImage.getWidth(); hCol++) {
                    Point rightPoint = new Point(hCol, r);
                    squaredDifferences[hCol] = sumSearchWindows(leftImage, leftPoint, rightImage, rightPoint, windowSize);

                    if (squaredDifferences[hCol] < minValue) {
                        minValue = squaredDifferences[hCol];
                        minLocation = rightPoint;
                    }
                }
                matchedPoints.add(new MatchedPair(leftPoint, minLocation));
            }
        }

        return matchedPoints;
    }

    private static long sumSearchWindows(BufferedImage imageLeft, Point pixelLeft, BufferedImage imageRight, Point pixelRight, int windowSize) {
        Point startingLeft = new Point(pixelLeft.x - windowSize, pixelLeft.y - windowSize);
        Point startingRight = new Point(pixelRight.x - windowSize, pixelRight.y - windowSize);
        int windowDiameter = windowSize * 2;

        long sum = 0;

        for (int y = 0; y < windowDiameter; y++) {
            for (int x = 0; x < windowDiameter; x++) {
                Point currentLeft = new Point(startingLeft.x + x, startingLeft.y + y);
                Point currentRight = new Point(startingRight.x + x, startingRight.y + y);

                int leftColor = isInBounds(currentLeft, imageLeft) ? Math.abs(imageLeft.getRGB(currentLeft.x,currentLeft.y)) : 0;
                int rightColor = isInBounds(currentRight, imageRight) ? Math.abs(imageRight.getRGB(currentRight.x,currentRight.y)) : 0;

                sum += Math.pow(leftColor - rightColor, 2);
            }
        }

        return sum;
    }

    private static boolean isInBounds(Point pixel, BufferedImage image) {
        return pixel.getX() >= 0 && pixel.getY() >= 0 && pixel.x < image.getWidth() && pixel.y < image.getHeight();
    }
}
