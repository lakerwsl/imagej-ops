
package net.imagej.ops.coloc.pearsons;

import net.imagej.ops.Ops;
import net.imagej.ops.Ops.Coloc.Pearsons;
import net.imagej.ops.coloc.Accumulator;
import net.imagej.ops.coloc.ThresholdMode;
import net.imagej.ops.coloc.pearsons.PearsonsResult;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.IterablePair;
import net.imglib2.util.Pair;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * A class that represents the mean calculation of the two source images in the
 * data container. It implements the FAST calculation for Pearson's Correlation.
 *
 * @author Ellen T Arena
 */
@Plugin(type = Pearsons.class)
public class PearsonsClassic<T extends RealType<T>, U extends RealType<U>>
	extends AbstractBinaryFunctionOp<Iterable<T>, Iterable<U>, PearsonsResult>
	implements Ops.Coloc.Pearsons
{
	@Parameter(required = false)
	private DoubleType mean1;

	@Parameter(required = false)
	private DoubleType mean2;

	@Parameter(required = false)
	private T threshold1;

	@Parameter(required = false)
	private U threshold2;

	@Override
	public PearsonsResult calculate(final Iterable<T> image1,
		final Iterable<U> image2)
	{
		// get the 2 images for the calculation of Pearson's
		final Iterable<Pair<T, U>> samples = new IterablePair<>(image1, image2);
		// get the means from the images if not provided
		final double m1 = mean1 == null ? computeMeanOf(image1) : mean1.get();
		final double m2 = mean2 == null ? computeMeanOf(image2) : mean2.get();

		final PearsonsResult result = new PearsonsResult();

		result.correlationValue = classicPearsons(samples, m1, m2,
			ThresholdMode.None);

		if (threshold1 != null && threshold2 != null) {

			result.correlationValueBelowThr = classicPearsons(samples, m1, m2,
				ThresholdMode.Below);

			result.correlationValueAboveThr = classicPearsons(samples, m1, m2,
				ThresholdMode.Above);
		}

		return result;
	}

	private <V extends RealType<V>> double computeMeanOf(final Iterable<V> in) {
		return ops().stats().mean(in).getRealDouble();
	}

	/**
	 * Calculates Person's R value by using a Classic implementation of the
	 * algorithm. 
	 *
	 * @param meanC1 Mean of channel 1.
	 * @param meanC2 Mean of channel 2.
	 * @return Person's R value
	 */
	private double classicPearsons(final Iterable<Pair<T, U>> samples,
		final double meanC1, final double meanC2, final ThresholdMode tMode)
	{
		// the actual accumulation of the image values is done in a separate object
		Accumulator<T, U> acc;

		if (tMode == ThresholdMode.None) {
			acc = new Accumulator<T, U>(samples, meanC1, meanC2) {

				@Override
				final public boolean accept(final T type1, final U type2) {
					return true;
				}
			};
		}
		else if (tMode == ThresholdMode.Below) {
			acc = new Accumulator<T, U>(samples, meanC1, meanC2) {

				@Override
				final public boolean accept(final T type1, final U type2) {
					return type1.compareTo(threshold1) < 0 || type2.compareTo(
						threshold2) < 0;
				}
			};
		}
		else if (tMode == ThresholdMode.Above) {
			acc = new Accumulator<T, U>(samples, meanC1, meanC2) {

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

		final int count = acc.getCount();
		final double pearsonsR = acc.getXY() / Math.sqrt(acc.getXX() * acc.getYY());

		checkForSanity(pearsonsR, count);
		return pearsonsR;
	}

	/**
	 * Does a sanity check for calculated Pearsons values. Wrong values can happen
	 * for fast and classic implementation.
	 *
	 * @param value The value to check.
	 * @throws IllegalArgumentException
	 */
	private static void checkForSanity(final double value, final int iterations) {
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
			throw new IllegalArgumentException(
				"A numerical problem occured: the input data is unsuitable for this algorithm. Possibly too few pixels (in range were: " +
					iterations + ").");
		}
	}
}
