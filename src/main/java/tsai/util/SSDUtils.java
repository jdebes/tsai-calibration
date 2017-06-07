package tsai.util;

import tsai.model.MatchedPair;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jonas on 28/05/17.
 */
public class SSDUtils {
    private static final int MAX_INTENSITY = 255;

    public static List ssdMatch(BufferedImage leftImage, BufferedImage rightImage, int windowSize) {
        List<MatchedPair> matchedPoints = new ArrayList<>();

        for (int r = 0; r < leftImage.getHeight() + windowSize; r += windowSize) {
            for (int c = 0; c < leftImage.getWidth() + windowSize; c += windowSize) {
                List<Long> squaredDifferences = new ArrayList<>();

                Point minLocation = new Point(c, 0);
                Point leftPoint = new Point(c,r);
                long minValue = sumSearchWindows(leftImage, leftPoint, rightImage, minLocation, windowSize);
                squaredDifferences.add(minValue);

                for (int hCol = 0; hCol < leftImage.getWidth(); hCol+= windowSize) {
                    Point rightPoint = new Point(hCol, r);
                    long summedWindow = sumSearchWindows(leftImage, leftPoint, rightImage, rightPoint, windowSize);
                    squaredDifferences.add(summedWindow);

                    if (summedWindow < minValue) {
                        minValue = summedWindow;
                        minLocation = rightPoint;
                    }
                }
                matchedPoints.add(new MatchedPair(leftPoint, minLocation, leftPoint.x - minLocation.x));
            }
        }

        normalisePairDisparities(matchedPoints);

        return matchedPoints;
    }

    public static void normalisePairDisparities(List<MatchedPair> matchedPairs) {
        double min = matchedPairs.stream().map(mp -> mp.getxDisparity()).min(Integer::compare).orElse(-1);
        double max = matchedPairs.stream().map(mp -> mp.getxDisparity()).max(Integer::compare).orElse(-1);

        for (MatchedPair matchedPair : matchedPairs) {
            //double normalised = (matchedPair.getxDisparity() - min) / (max - min);
            //matchedPair.setNormalisedXDisparity(normalised);

            String normIntensity = "0." + String.valueOf(Math.abs(matchedPair.getxDisparity()));
            double intensity = Double.valueOf(normIntensity);

            matchedPair.setNormalisedXDisparity(intensity);

        }


    }

    public static BufferedImage buildDisparityImage(List<MatchedPair> matchedPairs, BufferedImage leftImage, int windowSize) {
        BufferedImage outputGreyScale = new BufferedImage(leftImage.getWidth(), leftImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

        int i = 0;
        for (int r = 0; r < leftImage.getHeight() + windowSize; r += windowSize) {
            for (int c = 0; c < leftImage.getWidth() + windowSize; c += windowSize) {
                int intensity = (int)(matchedPairs.get(i).getNormalisedXDisparity() * MAX_INTENSITY);
                markWindowPoints(new Point(c,r), windowSize, outputGreyScale, intensity);
                i++;
            }
        }

        return outputGreyScale;
    }

    private static void markWindowPoints(Point pixel, int windowSize, BufferedImage output, int intensity) {
        int windowDiameter = windowSize / 2;
        Point startingPixel = new Point(pixel.x - windowDiameter, pixel.y - windowDiameter);
        WritableRaster writableRaster = output.getRaster();

        for (int y = 0; y < windowSize; y++) {
            for (int x = 0; x < windowSize; x++) {
                Point currentPixel = new Point(startingPixel.x + x, startingPixel.y + y);
                if (isInBounds(currentPixel, output)) {
                    writableRaster.setPixel(currentPixel.x, currentPixel.y, new int[]{intensity});

                }

            }
        }

    }

    private static long sumSearchWindows(BufferedImage imageLeft, Point pixelLeft, BufferedImage imageRight, Point pixelRight, int windowSize) {
        int windowDiameter = windowSize / 2;
        Point startingLeft = new Point(pixelLeft.x - windowDiameter, pixelLeft.y - windowDiameter);
        Point startingRight = new Point(pixelRight.x - windowDiameter, pixelRight.y - windowDiameter);

        long sum = 0;

        for (int y = 0; y < windowSize; y++) {
            for (int x = 0; x < windowSize; x++) {
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
