package net.imagej.ops.hough;

import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.BooleanType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

@Plugin( type = HoughCircleTransformOp.class )
public class HoughTransformOpWeights< T extends BooleanType< T >, R extends RealType< R > >
		extends AbstractUnaryFunctionOp< IterableInterval< T >, Img< DoubleType > >
{

	@Parameter
	private StatusService statusService;

	@Parameter( label = "Min circle radius", description = "Minimal radius, in pixel units, for the transform.", min = "1", type = ItemIO.INPUT )
	private long minRadius = 1;

	@Parameter( label = "Max circle radius", description = "Maximal radius, in pixel units, for the transform.", min = "1", type = ItemIO.INPUT )
	private long maxRadius = 50;

	@Parameter( label = "Step radius", description = "Radius step, in pixel units, for the transform.", min = "1", type = ItemIO.INPUT, required = false )
	private long stepRadius = 1;
	
	@Parameter( label = "Weights", description = "Weight image for the vote image.", type = ItemIO.INPUT )
	private RandomAccessible< R > weights;

	@Override
	public Img< DoubleType > calculate( final IterableInterval< T > input )
	{
		final int numDimensions = input.numDimensions();

		if ( input.numDimensions() != 2 ) { throw new IllegalArgumentException(
				"Cannot compute Hough circle transform for non-2D images. Got " + numDimensions + "D image." ); }

		maxRadius = Math.max( minRadius, maxRadius );
		minRadius = Math.min( minRadius, maxRadius );
		final long nRadiuses = ( maxRadius - minRadius ) / stepRadius + 1;

		/*
		 * Voting image.
		 */

		// Get a suitable image factory.
		final long[] dims = new long[ numDimensions + 1 ];
		for ( int d = 0; d < numDimensions; d++ )
			dims[ d ] = input.dimension( d );
		dims[ numDimensions ] = nRadiuses;
		final Dimensions dimensions = FinalDimensions.wrap( dims );
		final ImgFactory< DoubleType > factory = ops().create().imgFactory( dimensions );
		final Img< DoubleType > votes = factory.create( dimensions, new DoubleType() );

		/*
		 * Hough transform.
		 */

		final DoubleType weight = new DoubleType( Double.NaN );
		final RandomAccess< R > raWeight = weights.randomAccess( input );
		final double sum = ops().stats().sum( input ).getRealDouble();
		int progress = 0;

		final Cursor< T > cursor = input.localizingCursor();
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			if ( !cursor.get().get() )
				continue;

			raWeight.setPosition( cursor );
			weight.set( raWeight.get().getRealDouble() );

			for ( int i = 0; i < nRadiuses; i++ )
			{
				final IntervalView< DoubleType > slice = Views.hyperSlice( votes, numDimensions, i );
				final RandomAccess< DoubleType > ra = Views.extendZero( slice ).randomAccess();
				final long r = minRadius + i * stepRadius;
				MidPointAlgorithm.add( ra, cursor, r, weight );
			}

			statusService.showProgress( ++progress, ( int ) sum );
		}

		return votes;
	}
}
