
package net.imagej.ops.coloc.pValue;

import net.imagej.ops.Ops;
import net.imagej.ops.coloc.BlockShuffle;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imagej.ops.special.function.BinaryFunctionOp;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * This algorithm repeatedly executes a colocalization algorithm, computing a
 * p-value. It is based on a new statistical framework published by Wang et al
 * (2017) IEEE Signal Processing "Automated and Robust Quantification of
 * Colocalization in Dual-Color Fluorescence Microscopy: A Nonparametric
 * Statistical Approach".
 */
@Plugin(type = Ops.Coloc.PValue.class)
public class PValue<T extends RealType<T>, U extends RealType<U>> extends
	AbstractBinaryFunctionOp<Img<T>, Iterable<U>, Double> implements
	Ops.Coloc.PValue
{

	@Parameter
	private BinaryFunctionOp<Iterable<T>, Iterable<U>, Double> op;

	@Parameter(required = false)
	private final int nrRandomizations = 1000;

	@Parameter(required = false)
	private final long seed = 0x27372034;

	@Override
	public Double calculate(final Img<T> image1, final Iterable<U> image2) {

		final BlockShuffle<T> shuffler = new BlockShuffle<>(image1, seed);
		final double[] sampleDistribution = new double[nrRandomizations];

		final double value = op.calculate(image1, image2);

		for (int i = 0; i < nrRandomizations; i++) {
			final Img<T> shuffledImage = shuffler.shuffleBlocks(image1.factory());
			sampleDistribution[i] = op.calculate(shuffledImage, image2);
		}

		return calculatePvalue(value, sampleDistribution);
	}

	private double calculatePvalue(final double input,
		final double[] distribution)
	{
		double count = 0;
		for (int i = 0; i < distribution.length; i++) {
			if (distribution[i] > input) {
				count++;
			}
		}
		final double pvalue = count / distribution.length;
		return pvalue;
	}
}
