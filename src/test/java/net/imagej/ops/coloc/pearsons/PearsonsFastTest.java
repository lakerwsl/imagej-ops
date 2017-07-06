package net.imagej.ops.coloc.pearsons;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Random;

import net.imagej.ops.coloc.ColocalisationTest;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss.Gauss;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.junit.Test;

/**
 * Tests {@link PearsonsFast}.
 *
 * @author Ellen T Arena
 */
public class PearsonsFastTest extends ColocalisationTest {
	
	/**
	 * Tests if the fast implementation of Pearson's correlation with two
	 * zero correlated images produce a Pearson's R value of about zero.
	 */
	@Test
	public void fastPearsonsZeroCorrTest(){
		PearsonsResult result = (PearsonsResult) ops.run(PearsonsFast.class, zeroCorrelationImageCh1, zeroCorrelationImageCh2);
		assertEquals(0.0, result.correlationValue, 0.05);
	}
	
	/**
	 * Tests if the fast implementation of Pearson's correlation with two
	 * positive correlated images produce a Pearson's R value of about 0.75.
	 */
	@Test
	public void fastPearsonsPositiveCorrTest() {
		PearsonsResult result = (PearsonsResult) ops.run(PearsonsFast.class, positiveCorrelationImageCh1, positiveCorrelationImageCh2);
		assertEquals(0.75, result.correlationValue, 0.01);
	}
	
	/**
	 * Tests Pearson's correlation stays close to zero for image pairs with the same mean and spread
	 * of randomized pixel values around that mean.
	 */
	@Test
	public void differentMeansTest()  {
		final double initialMean = 0.2;
		final double spread = 0.1;
		final double[] sigma = new double[] {3.0, 3.0};

		for (double mean = initialMean; mean < 1; mean += spread) {
			RandomAccessibleInterval<FloatType> ch1 = produceMeanBasedNoiseImage(new FloatType(),
					512, 512, mean, spread, sigma, 0x01234567);
			RandomAccessibleInterval<FloatType> ch2 = produceMeanBasedNoiseImage(new FloatType(),
					512, 512, mean, spread, sigma, 0x98765432);
			PearsonsResult resultFast = (PearsonsResult) ops.run(PearsonsFast.class, ch1, ch2);
			assertEquals(0.0, resultFast.correlationValue, 0.1);

			/* If the means are the same, it causes a numerical problem in the classic implementation of Pearson's
			 * double resultClassic = PearsonsCorrelation.classicPearsons(cursor, mean, mean);
			 * assertTrue(Math.abs(resultClassic) < 0.1);
			 */
		}
	}

	/**
	 * This method creates a noise image that has a specified mean.
	 * Every pixel has a value uniformly distributed around mean with
	 * the maximum spread specified.
	 *
	 * @return IllegalArgumentException if specified means and spreads are not valid
	 */
	public static <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> produceMeanBasedNoiseImage(T type, int width,
		int height, double mean, double spread, double[] smoothingSigma, long seed) throws IllegalArgumentException {
		if (mean < spread || (mean + spread) > type.getMaxValue()) {
			throw new IllegalArgumentException("Mean must be larger than spread, and mean plus spread must be smaller than max of the type");
		}
		// create the new image
		ImgFactory<T> imgFactory = new ArrayImgFactory<T>();
		RandomAccessibleInterval<T> noiseImage = imgFactory.create( new int[] {width, height}, type); // "Noise image");

		Random r = new Random(seed);
		for (T value : Views.iterable(noiseImage)) {
			value.setReal( mean + ( (r.nextDouble() - 0.5) * spread ) );
		}

		return gaussianSmooth(noiseImage, smoothingSigma);
	}

	/**
	 * Gaussian Smooth of the input image using intermediate float format.
	 * @param <T>
	 * @param img
	 * @param sigma
	 * @return
	 */
	public static <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> gaussianSmooth(
		RandomAccessibleInterval<T> img, double[] sigma) {
		Interval interval = Views.iterable(img);

		ImgFactory<T> outputFactory = new ArrayImgFactory<T>();
		final long[] dim = new long[ img.numDimensions() ];
		img.dimensions(dim);
		RandomAccessibleInterval<T> output = outputFactory.create( dim,
			img.randomAccess().get().createVariable() );

		final long[] pos = new long[ img.numDimensions() ];
		Arrays.fill(pos, 0);
		Localizable origin = new Point(pos);

		ImgFactory<FloatType> tempFactory = new ArrayImgFactory<FloatType>();
		RandomAccessible<T> input = Views.extendMirrorSingle(img);
		Gauss.inFloat(sigma, input, interval, output, origin, tempFactory);

		return output;
	}
}
