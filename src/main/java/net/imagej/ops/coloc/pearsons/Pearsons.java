package net.imagej.ops.coloc.pearsons;

import net.imagej.ops.Ops;
import net.imagej.ops.coloc.AutoThresholdRegression;
import net.imagej.ops.coloc.MaskFactory;
import net.imagej.ops.coloc.TwinCursor;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.IterablePair;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;

import org.python.icu.impl.TextTrieMap.ResultHandler;
import org.python.tests.RedundantInterfaceDeclarations.Implementation;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.process.ImageStatistics;
import sc.fiji.coloc.algorithms.Accumulator;
import sc.fiji.coloc.algorithms.MissingPreconditionException;
import sc.fiji.coloc.gadgets.ThresholdMode;

/**
 * This algorithm calculates Li et al.'s ICQ (intensity correlation quotient).
 *
 * @param <T>
 *            Type of the first image
 * @param <U>
 *            Type of the second image
 */
@Plugin(type = Ops.Coloc.Pearsons.class)
public class Pearsons<T extends RealType<T>, U extends RealType<U>>
		extends AbstractBinaryFunctionOp<Iterable<T>, Iterable<U>, Double> implements Ops.Coloc.Pearsons {
	
	//Identifiers for choosing which implementation to use
	public enum Implementation {Classic, Fast}
	@Parameter(required = false)
	private Implementation theImplementation = Implementation.Fast;
	
	@Override
	public Double calculate(Iterable<T> image1, Iterable<U> image2) {
		// get the 2 images for the calculation of Pearson's
		final Iterable<Pair<T, U>> samples = new IterablePair<>(image1, image2);

		// get the thresholds of the images
		AutoThresholdRegression<T> autoThreshold = container.getAutoThreshold();
		T threshold1 = 	autoThreshold.getCh1MaxThreshold();
		T threshold2 = 	autoThreshold.getCh2MaxThreshold();

		/* Create cursors to walk over the images. First go over the
		 * images without a mask. */
		TwinCursor<T> cursor = new TwinCursor<T>(
				img1.randomAccess(), img2.randomAccess(),
				Views.iterable(mask).localizingCursor());

		if (theImplementation == Implementation.Classic) {
			// get the means from the DataContainer
			double ch1Mean = container.getMeanCh1();
			double ch2Mean = container.getMeanCh2();

				cursor.reset();
				pearsonsCorrelationValue = classicPearsons(cursor,
						ch1Mean, ch2Mean);
		
				cursor.reset();
				pearsonsCorrelationValueBelowThr = classicPearsons(cursor,
						ch1Mean, ch2Mean, threshold1, threshold2, ThresholdMode.Below);
		
				cursor.reset();
				pearsonsCorrelationValueAboveThr = classicPearsons(cursor,
						ch1Mean, ch2Mean, threshold1, threshold2, ThresholdMode.Above);
		}
		else if (theImplementation == Implementation.Fast) {

				cursor.reset();
				pearsonsCorrelationValue = fastPearsons(cursor);
		
				cursor.reset();
				pearsonsCorrelationValueBelowThr = fastPearsons(cursor,
						threshold1, threshold2, ThresholdMode.Below);

				cursor.reset();
				pearsonsCorrelationValueAboveThr = fastPearsons(cursor,
						threshold1, threshold2, ThresholdMode.Above);
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Calculates Pearson's R value without any constraint in values, thus it uses no thresholds.
	 * If additional data like the images mean is needed, it is calculated.
	 *
	 * @param <S> The images base type.
	 * @param img1 The first image to walk over.
	 * @param img2 The second image to walk over.
	 * @return Pearson's R value.
	 * @throws MissingPreconditionException
	 */
	public <S extends RealType<S>> double calculatePearsons(
			RandomAccessibleInterval<S> img1, RandomAccessibleInterval<S> img2)
			throws MissingPreconditionException {
		// create an "always true" mask to walk over the images
		final long[] dims = new long[img1.numDimensions()];
		img1.dimensions(dims);
		RandomAccessibleInterval<BitType> alwaysTrueMask = MaskFactory.createMask(dims, true);
		return calculatePearsons(img1, img2, alwaysTrueMask);
	}

	/**
	 * Calculates Pearson's R value without any constraint in values, thus it uses no
	 * thresholds. A mask is required to mark which data points should be visited. If
	 * additional data like the images mean is needed, it is calculated.
	 *
	 * @param <S> The images base type.
	 * @param img1 The first image to walk over.
	 * @param img2 The second image to walk over.
	 * @param mask A mask for the images.
	 * @return Pearson's R value.
	 * @throws MissingPreconditionException
	 */
	public <S extends RealType<S>> double calculatePearsons(
			RandomAccessibleInterval<S> img1, RandomAccessibleInterval<S> img2,
			RandomAccessibleInterval<BitType> mask) throws MissingPreconditionException {
		TwinCursor<S> cursor = new TwinCursor<S>(
				img1.randomAccess(), img2.randomAccess(),
				Views.iterable(mask).localizingCursor());

		double r;
		if (theImplementation == Implementation.Classic) {
			/* since we need the means and apparently don't have them,
			 * calculate them.
			 */
			double mean1 = ImageStatistics.getImageMean(img1);
			double mean2 = ImageStatistics.getImageMean(img2);
			// do the actual calculation
			r = classicPearsons(cursor, mean1, mean2);
		} else {
			r = fastPearsons(cursor);
		}

		return r;
	}

	/**
	 * Calculates Pearson's R value with the possibility to constraint in values.
	 * This could be useful of one wants to apply thresholds. You need to provide
	 * the images means, albeit not used by all implementations.
	 *
	 * @param <S> The images base type.
	 * @param cursor The cursor to walk over both images.
	 * @return Pearson's R value.
	 * @throws MissingPreconditionException
	 */
	public <S extends RealType<S>> double calculatePearsons(TwinCursor<S> cursor,
			double mean1, double mean2, S thresholdCh1, S thresholdCh2,
			ThresholdMode tMode) throws MissingPreconditionException {
		if (theImplementation == Implementation.Classic) {
			// do the actual calculation
			return classicPearsons(cursor, mean1, mean2,
					thresholdCh1, thresholdCh2, tMode);
		} else {
			return fastPearsons(cursor, thresholdCh1,
					thresholdCh2, tMode);
		}
	}

	/**
	 * Calculates Person's R value by using a Classic implementation of the
	 * algorithm. This method allows the specification of a TwinValueRangeCursor.
	 * With such a cursor one for instance can combine different thresholding
	 * conditions for each channel. The cursor is not closed in here.
	 *
	 * @param <T> The image base type
	 * @param cursor The cursor that defines the walk over both images.
	 * @param meanCh1 Mean of channel 1.
	 * @param meanCh2 Mean of channel 2.
	 * @return Person's R value
	 */
	public static <T extends RealType<T>> double classicPearsons(TwinCursor<T> cursor,
			double meanCh1, double meanCh2) throws MissingPreconditionException {
		return classicPearsons(cursor, meanCh1, meanCh2, null, null, ThresholdMode.None);
	}

	public static <T extends RealType<T>> double classicPearsons(TwinCursor<T> cursor,
			double meanCh1, double meanCh2, final T thresholdCh1, final T thresholdCh2,
			ThresholdMode tMode) throws MissingPreconditionException {
		// the actual accumulation of the image values is done in a separate object
		Accumulator<T> acc;

		if (tMode == ThresholdMode.None) {
			acc = new Accumulator<T>(cursor, meanCh1, meanCh2) {
				@Override
				final public boolean accept(T type1, T type2) {
					return true;
				}
			};
		} else if (tMode == ThresholdMode.Below) {
			acc = new Accumulator<T>(cursor, meanCh1, meanCh2) {
				@Override
				final public boolean accept(T type1, T type2) {
					return type1.compareTo(thresholdCh1) < 0 ||
							type2.compareTo(thresholdCh2) < 0;
				}
			};
		} else if (tMode == ThresholdMode.Above) {
			acc = new Accumulator<T>(cursor, meanCh1, meanCh2) {
				@Override
				final public boolean accept(T type1, T type2) {
					return type1.compareTo(thresholdCh1) > 0 ||
							type2.compareTo(thresholdCh2) > 0;
				}
			};
		} else {
			throw new UnsupportedOperationException();
		}

		double pearsonsR = acc.xy / Math.sqrt(acc.xx * acc.yy);

		checkForSanity(pearsonsR, acc.count);
		return pearsonsR;
	}

	/**
	 * Calculates Person's R value by using a fast implementation of the
	 * algorithm. This method allows the specification of a TwinValueRangeCursor.
	 * With such a cursor one for instance can combine different thresholding
	 * conditions for each channel. The cursor is not closed in here.
	 *
	 * @param <T> The image base type
	 * @param cursor The cursor that defines the walk over both images.
	 * @return Person's R value
	 */
	public static <T extends RealType<T>> double fastPearsons(TwinCursor<T> cursor)
			throws MissingPreconditionException {
		return fastPearsons(cursor, null, null, ThresholdMode.None);
	}

	public static <T extends RealType<T>> double fastPearsons(TwinCursor<T> cursor,
			final T thresholdCh1, final T thresholdCh2, ThresholdMode tMode)
			throws MissingPreconditionException {
		// the actual accumulation of the image values is done in a separate object
		Accumulator<T> acc;

		if (tMode == ThresholdMode.None) {
			acc = new Accumulator<T>(cursor) {
				@Override
				final public boolean accept(T type1, T type2) {
					return true;
				}
			};
		} else if (tMode == ThresholdMode.Below) {
			acc = new Accumulator<T>(cursor) {
				@Override
				final public boolean accept(T type1, T type2) {
					return type1.compareTo(thresholdCh1) < 0 ||
							type2.compareTo(thresholdCh2) < 0;
				}
			};
		} else if (tMode == ThresholdMode.Above) {
			acc = new Accumulator<T>(cursor) {
				@Override
				final public boolean accept(T type1, T type2) {
					return type1.compareTo(thresholdCh1) > 0 ||
							type2.compareTo(thresholdCh2) > 0;
				}
			};
		} else {
			throw new UnsupportedOperationException();
		}

		// for faster computation, have the inverse of N available
		double invCount = 1.0 / acc.count;

		double pearsons1 = acc.xy - (acc.x * acc.y * invCount);
		double pearsons2 = acc.xx - (acc.x * acc.x * invCount);
		double pearsons3 = acc.yy - (acc.y * acc.y * invCount);
		double pearsonsR = pearsons1 / (Math.sqrt(pearsons2 * pearsons3));

		checkForSanity(pearsonsR, acc.count);

		return pearsonsR;
	}

	/**
	 * Does a sanity check for calculated Pearsons values. Wrong
	 * values can happen for fast and classic implementation.
	 *
	 * @param val The value to check.
	 */
	private static void checkForSanity(double value, int iterations) throws MissingPreconditionException {
		if ( Double.isNaN(value) || Double.isInfinite(value)) {
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
			throw new MissingPreconditionException("A numerical problem occured: the input data is unsuitable for this algorithm. Possibly too few pixels (in range were: " + iterations + ").");
		}
	}

	@Override
	public void processResults(ResultHandler<T> handler) {
		super.processResults(handler);

		handler.handleValue("Pearson's R value (no threshold)", pearsonsCorrelationValue, 2);
		handler.handleValue("Pearson's R value (below threshold)", pearsonsCorrelationValueBelowThr, 2);
		handler.handleValue("Pearson's R value (above threshold)", pearsonsCorrelationValueAboveThr, 2);
	}

	public double getPearsonsCorrelationValue() {
		return pearsonsCorrelationValue;
	}

	public double getPearsonsCorrelationBelowThreshold() {
		return pearsonsCorrelationValueBelowThr;
	}

	public double getPearsonsCorrelationAboveThreshold() {
		return pearsonsCorrelationValueAboveThr;
	}
}
