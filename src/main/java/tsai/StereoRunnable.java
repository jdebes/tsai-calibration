package tsai;

import tsai.model.MatchedPair;
import tsai.util.SSDUtils;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Created by jonas on 12/06/17.
 */
public class StereoRunnable implements Runnable {

    private List<MatchedPair> matchedPairs;
    private BufferedImage leftImage;
    private BufferedImage rightImage;
    private int windowSize;
    private int start;
    private int cut;

    public StereoRunnable(List<MatchedPair> matchedPairs, BufferedImage leftImage, BufferedImage rightImage, int windowSize, int start, int cut) {
        this.matchedPairs = matchedPairs;
        this.leftImage = leftImage;
        this.rightImage = rightImage;
        this.windowSize = windowSize;
        this.start = start;
        this.cut = cut;
    }

    public void run() {
        SSDUtils.ssdMatch(leftImage, rightImage, windowSize, cut, start, matchedPairs);
    }
}
