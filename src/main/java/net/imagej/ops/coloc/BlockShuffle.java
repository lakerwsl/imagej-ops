
package net.imagej.ops.coloc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * Helper class for creating images with shuffled blocks.
 */
public class BlockShuffle<T extends RealType<T>>
{
	private final RandomAccessibleInterval<T> image;
	private final List<IterableInterval<T>> blockIntervals;
	private Random rng;
	
	public BlockShuffle(final RandomAccessibleInterval<T> image, final long seed) {
		this.image = image;
		rng = new Random(seed);

		final int nrDimensions = image.numDimensions();
		final long[] dimensions = new long[nrDimensions];
		int nrBlocksPerImage = 1;
		final long[] nrBlocksPerDimension = new long[nrDimensions];
		final long[] blockSize = new long[nrDimensions];

		for (int i = 0; i < nrDimensions; i++) {
			blockSize[i] = (long) Math.floor(Math.sqrt(dimensions[i]));
			nrBlocksPerDimension[i] = dimensions[i] / blockSize[i];
			// if there is the need for a out-of-bounds block, increase count
			if (dimensions[i] % blockSize[i] != 0) nrBlocksPerDimension[i]++;
			nrBlocksPerImage *= nrBlocksPerDimension[i];
		}

		final double[] floatDimensions = new double[nrDimensions];
		for (int i = 0; i < nrDimensions; ++i)
			floatDimensions[i] = dimensions[i];
		blockIntervals = new ArrayList<IterableInterval<T>>(nrBlocksPerImage);
		final RandomAccessible<T> infiniteImg = Views.extendMirrorSingle(image);
		generateBlocksXYZ(infiniteImg, floatDimensions,
			blockSize);
	}

	public Img<T> shuffleBlocks(ImgFactory<T> factory)
	{
		final int nrBlocksPerImage = blockIntervals.size();
		final List<Cursor<T>> inputBlocks = new ArrayList<Cursor<T>>(
			nrBlocksPerImage);
		final List<Cursor<T>> outputBlocks = new ArrayList<Cursor<T>>(
			nrBlocksPerImage);
		for (final IterableInterval<T> roiIt : blockIntervals) {
			inputBlocks.add(roiIt.localizingCursor());
			outputBlocks.add(roiIt.localizingCursor());
		}
	
		final T zero = image.randomAccess().get().createVariable();
		zero.setZero();
	
		final long[] dims = new long[image.numDimensions()];
		image.dimensions(dims);
		final Img<T> shuffledImage = factory.create(dims, image.randomAccess().get()
			.createVariable());
		final RandomAccessible<T> infiniteShuffledImage = Views.extendValue(
			shuffledImage, zero);
	
		Collections.shuffle(inputBlocks, rng);
		final RandomAccess<T> output = infiniteShuffledImage.randomAccess();
	
		for (int j = 0; j < inputBlocks.size(); j++) {
			final Cursor<T> inputCursor = inputBlocks.get(j);
			final Cursor<T> outputCursor = outputBlocks.get(j);
			while (inputCursor.hasNext() && outputCursor.hasNext()) {
				inputCursor.fwd();
				outputCursor.fwd();
				output.setPosition(outputCursor);
				output.get().set(inputCursor.get());
			}
			inputCursor.reset();
			outputCursor.reset();
		}
	
		return shuffledImage;
	}

	private void generateBlocksXYZ(
		final RandomAccessible<T> infiniteImg,
		final double[] size, final long[] blocksize)
	{
		// get the number of dimensions
		final int nrDimensions = infiniteImg.numDimensions();
		final long[] offset = new long[nrDimensions];
		if (nrDimensions == 2) { // for a 2D image...
			generateBlocksXY(infiniteImg, blockIntervals, offset, size, blocksize);
		}
		else if (nrDimensions == 3) { // for a 3D image...
			final double depth = size[2];
			long z;
			final long originalZ = offset[2];
			// go through the depth in steps of block depth
			for (z = blocksize[2]; z <= depth; z += blocksize[2]) {

				offset[2] = originalZ + z - blocksize[2];
				generateBlocksXY(infiniteImg, blockIntervals, offset, size, blocksize);
			}
			offset[2] = originalZ;
		}
	}

	private void generateBlocksXY(
		final RandomAccessible<T> img, final List<IterableInterval<T>> blockList,
		final long[] offset, final double[] size, final long[] blocksize)
	{
		// potentially masked image height
		final double height = size[1];
		final long originalY = offset[1];
		// go through the height in steps of block width
		long y;
		for (y = blocksize[1]; y <= height; y += blocksize[1]) {
			offset[1] = originalY + y - blocksize[1];
			generateBlocksX(img, blockList, offset, size, blocksize);
		}
		// check is we need to add a out of bounds strategy cursor
		offset[1] = originalY;
	}

	private void generateBlocksX(
		final RandomAccessible<T> img, final List<IterableInterval<T>> blockList,
		final long[] offset, final double[] size, final long[] blocksize)
	{
		// go through the width in steps of block width
		final long[] min = new long[offset.length];
		final long[] max = new long[blocksize.length];
		for (int i = 1; i < offset.length; i++) {
			min[i] = offset[i];
			max[i] = min[i] + blocksize[i];
		}
		for (long x = blocksize[0]; x <= size[0]; x += blocksize[0]) {
			min[0] = offset[0] + x - blocksize[0];
			max[0] = min[0] + blocksize[0];
			blockList.add(Views.interval(img, min, max));
		}
	}
}
