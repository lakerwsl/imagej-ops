/*-
 * #%L
 * Fiji's plugin for colocalization analysis.
 * %%
 * Copyright (C) 2009 - 2017 Fiji developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package net.imagej.ops.coloc;

import net.imagej.ops.Ops;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.IterablePair;
import net.imglib2.util.Pair;

import org.scijava.plugin.Plugin;

/**
 * A class implementing the automatic finding of a threshold used for Pearson
 * colocalisation calculation.
 */
@Plugin(type = Ops.Coloc.Pearsons.class)
public class AutoThresholdRegression<T extends RealType<T>, U extends RealType<U>>
	extends AbstractBinaryFunctionOp<Iterable<T>, Iterable<U>, Double[]>
	implements Ops.Coloc.Pearsons
{

	// Identifiers for choosing which implementation to use
	public enum Implementation {
			Costes, Bisection
	}

	Implementation implementation = Implementation.Bisection;
	/* The threshold for ratio of y-intercept : y-mean to raise a warning about
	 * it being to high or low, meaning far from zero. Don't use y-max as before,
	 * since this could be a very high value outlier. Mean is probably more
	 * reliable.
	 */
	final double warnYInterceptToYMeanRatioThreshold = 0.01;
	// the slope and and intercept of the regression line
	double autoThresholdSlope = 0.0, autoThresholdIntercept = 0.0;
	/* The thresholds for both image channels. Pixels below a lower
	 * threshold do NOT include the threshold and pixels above an upper
	 * one will NOT either. Pixels "in between (and including)" thresholds
	 * do include the threshold values.
	 */
	T ch1MinThreshold, ch1MaxThreshold;
	U ch2MinThreshold, ch2MaxThreshold;
	// additional information
	double bToYMeanRatio = 0.0;

	@Override
	public Double[] calculate(final Iterable<T> image1,
		final Iterable<U> image2)
	{

		final Iterable<Pair<T, U>> samples = new IterablePair<>(image1, image2);
		final double ch1Mean = ops().stats().mean(image1).getRealDouble();
		final double ch2Mean = ops().stats().mean(image2).getRealDouble();

		final double combinedMean = ch1Mean + ch2Mean;

		// variables for summing up the
		double ch1MeanDiffSum = 0.0, ch2MeanDiffSum = 0.0, combinedMeanDiffSum =
			0.0;
		double combinedSum = 0.0;
		int N = 0, NZero = 0;

		for (final Pair<T, U> sample : samples) {
			// reference image data type
			final T type = sample.getA();

			final double ch1 = sample.getA().getRealDouble();
			final double ch2 = sample.getB().getRealDouble();

			combinedSum = ch1 + ch2;

			// TODO: Shouldn't the whole calculation take only pixels
			// into account that are combined above zero? And not just
			// the denominator (like it is done now)?

			// calculate the numerators for the variances
			ch1MeanDiffSum += (ch1 - ch1Mean) * (ch1 - ch1Mean);
			ch2MeanDiffSum += (ch2 - ch2Mean) * (ch2 - ch2Mean);
			combinedMeanDiffSum += (combinedSum - combinedMean) * (combinedSum -
				combinedMean);

			// count only pixels that are above zero
			if ((ch1 + ch2) > 0.00001) NZero++;

			N++;
		}

		final double ch1Variance = ch1MeanDiffSum / (N - 1);
		final double ch2Variance = ch2MeanDiffSum / (N - 1);
		final double combinedVariance = combinedMeanDiffSum / (N - 1.0);

		// http://mathworld.wolfram.com/Covariance.html
		// ?2 = X2?(X)2
		// = E[X2]?(E[X])2
		// var (x+y) = var(x)+var(y)+2(covar(x,y));
		// 2(covar(x,y)) = var(x+y) - var(x)-var(y);

		final double ch1ch2Covariance = 0.5 * (combinedVariance - (ch1Variance +
			ch2Variance));

		// calculate regression parameters
		final double denom = 2 * ch1ch2Covariance;
		final double num = ch2Variance - ch1Variance + Math.sqrt((ch2Variance -
			ch1Variance) * (ch2Variance - ch1Variance) + (4 * ch1ch2Covariance *
				ch1ch2Covariance));

		final double m = num / denom;
		final double b = ch2Mean - m * ch1Mean;

		// A stepper that walks thresholds
		Stepper stepper;
		// to map working thresholds to channels
		ChannelMapper mapper;

		// let working threshold walk on channel one if the regression line
		// leans more towards the abscissa (which represents channel one) for
		// positive and negative correlation.
		if (m > -1 && m < 1.0) {
			// Map working threshold to channel one (because channel one has a
			// larger maximum value.
			mapper = new ChannelMapper() {

				@Override
				public double getCh1Threshold(final double t) {
					return t;
				}

				@Override
				public double getCh2Threshold(final double t) {
					return (t * m) + b;
				}
			};
			// Select a stepper
			if (implementation == Implementation.Bisection) {
				// Start at the midpoint of channel one
				stepper = new BisectionStepper(Math.abs(container.getMaxCh1() +
					container.getMinCh1()) * 0.5, container.getMaxCh1());
			}
			else {
				stepper = new SimpleStepper(container.getMaxCh1());
			}
		}
		else {
			// Map working threshold to channel two (because channel two has a
			// larger maximum value.
			mapper = new ChannelMapper() {

				@Override
				public double getCh1Threshold(final double t) {
					return (t - b) / m;
				}

				@Override
				public double getCh2Threshold(final double t) {
					return t;
				}
			};
			// Select a stepper
			if (implementation == Implementation.Bisection) {
				// Start at the midpoint of channel two
				stepper = new BisectionStepper(Math.abs(container.getMaxCh2() +
					container.getMinCh2()) * 0.5, container.getMaxCh2());
			}
			else {
				stepper = new SimpleStepper(container.getMaxCh2());
			}
		}

		// Min threshold not yet implemented
		double ch1ThreshMax = container.getMaxCh1();
		double ch2ThreshMax = container.getMaxCh2();

		// define some image type specific threshold variables
		final T thresholdCh1 = type.createVariable();
		final T thresholdCh2 = type.createVariable();
		// reset the previously created cursor
		cursor.reset();

		/* Get min and max value of image data type. Since type of image
		 * one and two are the same, we dont't need to distinguish them.
		 */
		final T dummyT = type.createVariable();
		final double minVal = dummyT.getMinValue();
		final double maxVal = dummyT.getMaxValue();

		// do regression
		while (!stepper.isFinished()) {
			// round ch1 threshold and compute ch2 threshold
			ch1ThreshMax = Math.round(mapper.getCh1Threshold(stepper.getValue()));
			ch2ThreshMax = Math.round(mapper.getCh2Threshold(stepper.getValue()));
			/* Make sure we don't get overflow the image type specific threshold variables
			 * if the image data type doesn't support this value.
			 */
			thresholdCh1.setReal(clamp(ch1ThreshMax, minVal, maxVal));
			thresholdCh2.setReal(clamp(ch2ThreshMax, minVal, maxVal));

			try {
				// do persons calculation within the limits
				final double currentPersonsR = pearsonsCorrellation.calculatePearsons(
					cursor, ch1Mean, ch2Mean, thresholdCh1, thresholdCh2,
					ThresholdMode.Below);
				stepper.update(currentPersonsR);
			}
			catch (final MissingPreconditionException e) {
				/* the exception that could occur is due to numerical
				 * problems within the Pearsons calculation. */
				stepper.update(Double.NaN);
			}

			// reset the cursor to reuse it
			cursor.reset();
		}

		/* Store the new results. The lower thresholds are the types
		 * min value for now. For the max threshold we do a clipping
		 * to make it fit into the image type.
		 */
		ch1MinThreshold = type.createVariable();
		ch1MinThreshold.setReal(minVal);

		ch1MaxThreshold = type.createVariable();
		ch1MaxThreshold.setReal(clamp(ch1ThreshMax, minVal, maxVal));

		ch2MinThreshold = type.createVariable();
		ch2MinThreshold.setReal(minVal);

		ch2MaxThreshold = type.createVariable();
		ch2MaxThreshold.setReal(clamp(ch2ThreshMax, minVal, maxVal));

		autoThresholdSlope = m;
		autoThresholdIntercept = b;
		bToYMeanRatio = b / container.getMeanCh2();

		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Clamp a value to a min or max value. If the value is below min, min is
	 * returned. Accordingly, max is returned if the value is larger. If it is
	 * neither, the value itself is returned.
	 */
	public static double clamp(final double val, final double min,
		final double max)
	{
		return min > val ? min : max < val ? max : val;
	}

	public double getBToYMeanRatio() {
		return bToYMeanRatio;
	}

	public double getWarnYInterceptToYMaxRatioThreshold() {
		return warnYInterceptToYMeanRatioThreshold;
	}

	public double getAutoThresholdSlope() {
		return autoThresholdSlope;
	}

	public double getAutoThresholdIntercept() {
		return autoThresholdIntercept;
	}

	public T getCh1MinThreshold() {
		return ch1MinThreshold;
	}

	public T getCh1MaxThreshold() {
		return ch1MaxThreshold;
	}

	public U getCh2MinThreshold() {
		return ch2MinThreshold;
	}

	public U getCh2MaxThreshold() {
		return ch2MaxThreshold;
	}
}
