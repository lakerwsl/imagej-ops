package net.imagej.ops.coloc.threshold;


public class AutothresholdRegressionResults<T> {
	private final double warnYInterceptToYMeanRatioThreshold = 0.01;
	// the slope and and intercept of the regression line
	private double autoThresholdSlope = 0.0, autoThresholdIntercept = 0.0;
	/* The thresholds for both image channels. Pixels below a lower
	 * threshold do NOT include the threshold and pixels above an upper
	 * one will NOT either. Pixels "in between (and including)" thresholds
	 * do include the threshold values.
	 */
	private T ch1MinThreshold, ch1MaxThreshold, ch2MinThreshold, ch2MaxThreshold;
	// additional information
	private double bToYMeanRatio = 0.0;
	
}
