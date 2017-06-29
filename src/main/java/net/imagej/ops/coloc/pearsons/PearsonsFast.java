
package net.imagej.ops.coloc.pearsons;

import net.imagej.ops.Ops;
import net.imagej.ops.coloc.Accumulator;
import net.imagej.ops.coloc.ThresholdMode;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.IterablePair;
import net.imglib2.util.Pair;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * A class that represents the mean calculation of the two source images in the
 * data container. It implements the FAST calculation for Pearson's Correlation.
 *
 * @param <T>
 */
@Plugin(type = Ops.Coloc.Pearsons.class)
public class PearsonsFast<T extends RealType<T>, U extends RealType<U>> extends
	AbstractBinaryFunctionOp<Iterable<T>, Iterable<U>, Double[]> implements
	Ops.Coloc.Pearsons
{

	@Parameter(type = ItemIO.OUTPUT)
	private Double pearsonsFastCorrelationValue;

	@Parameter(type = ItemIO.OUTPUT)
	private Double pearsonsFastCorrelationValueBelowThr;

	@Parameter(type = ItemIO.OUTPUT)
	private Double pearsonsFastCorrelationValueAboveThr;

	@Override
	public Double[] calculate(final Iterable<T> image1,
		final Iterable<U> image2)
	{
		// get the 2 images for the calculation of Pearson's
		final Iterable<Pair<T, U>> samples = new IterablePair<>(image1, image2);

		// get the thresholds of the images
		final Iterable<T> threshold1 = image1.getCh1MaxThreshold();
		final Iterable<T> threshold2 = image2.getCh2MaxThreshold();

		pearsonsFastCorrelationValue = fastPearsons(samples);

		pearsonsFastCorrelationValueBelowThr = fastPearsons(samples, threshold1,
			threshold2, ThresholdMode.Below);

		pearsonsFastCorrelationValueAboveThr = fastPearsons(samples, threshold1,
			threshold2, ThresholdMode.Above);
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Calculates Person's R value by using a fast implementation of the
	 * algorithm. This method allows the specification of a TwinValueRangeCursor.
	 * With such a cursor one for instance can combine different thresholding
	 * conditions for each channel. The cursor is not closed in here.
	 *
	 * @param <T> The image base type
	 * @return Person's R value
	 */
	private <T extends RealType<T>> double fastPearsons(
		final Iterable<Pair<T, U>> samples)
	{
		return fastPearsons(samples, null, null, ThresholdMode.None);
	}

	private <T extends RealType<T>> double fastPearsons(
		final Iterable<Pair<T, U>> samples, final Iterable<T> threshold1,
		final Iterable<T> threshold2, final ThresholdMode tMode)
	{
		// the actual accumulation of the image values is done in a separate object
		Accumulator<T, U> acc;

		if (tMode == ThresholdMode.None) {
			acc = new Accumulator<T, U>(samples) {

				@Override
				final public boolean accept(final T type1, final U type2) {
					return true;
				}
			};
		}
		else if (tMode == ThresholdMode.Below) {
			acc = new Accumulator<T, U>(samples) {

				@Override
				final public boolean accept(final T type1, final U type2) {
					return type1.compareTo(threshold1) < 0 || type2.compareTo(
						threshold2) < 0;
				}
			};
		}
		else if (tMode == ThresholdMode.Above) {
			acc = new Accumulator<T, U>(samples) {

				@Override
				final public boolean accept(final T type1, final U type2) {
					return type1.compareTo(threshold1) > 0 || type2.compareTo(
						threshold2) > 0;
				}
			};
		}
		else {
			throw new UnsupportedOperationException();
		}

		// for faster computation, have the inverse of N available
		int count = acc.getCount();
		final double invCount = 1.0 / count;

		final double pearsons1 = acc.getXY() - (acc.getX() * acc.getX() * invCount);
		final double pearsons2 = acc.getXX() - (acc.getX() * acc.getX() * invCount);
		final double pearsons3 = acc.getYY() - (acc.getY() * acc.getY() * invCount);
		final double pearsonsR = pearsons1 / (Math.sqrt(pearsons2 * pearsons3));

		checkForSanity(pearsonsR, count);

		return pearsonsR;
	}

	/**
	 * Does a sanity check for calculated Pearsons values. Wrong values can happen
	 * for fast and classic implementation.
	 *
	 * @param value The value to check.
	 * @throws Exception
	 */
	private static void checkForSanity(final double value, final int iterations)
		throws Exception
	{
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			/* For the _fast_ implementation this could happen:
			 *   Infinity could happen if only the numerator is 0, i.e.:
			 *     sum1squared == sum1 * sum1 * invN
			 *   and
			 *     sum2squared == sum2 * sum2 * invN
			 *   If the denominator is also zero, one will get NaN, i.e:
			 *     sumProduct1_2 == sum1 * sum2 * invN
			 *
			 * For the classic implementation it could happen, too:
			 *   Infinity happens if one channels sum of value-mean-differences
			 *   is zero. If it is negative for one image you will get NaN.
			 *   Additionally, if is zero for both channels at once you
			 *   could get NaN. NaN
			 */
			throw new Exception(
				"A numerical problem occured: the input data is unsuitable for this algorithm. Possibly too few pixels (in range were: " +
					iterations + ").");
		}
	}
}
