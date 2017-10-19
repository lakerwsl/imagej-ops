package net.imagej.ops.segment.hough;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.localextrema.LocalExtrema;
import net.imglib2.algorithm.localextrema.LocalExtrema.MaximumCheck;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

@Plugin( type = HoughCircleDetectorOp.class )
public class HoughCircleDetectorLocalExtremaOp< T extends RealType< T > & NativeType< T > >
		extends AbstractUnaryFunctionOp< RandomAccessibleInterval< T >, List< HoughCircle > >
{

	@Parameter
	private ThreadService threadService;

	@Parameter( required = true, min = "1" )
	private double minRadius;

	@Parameter( required = true, min = "1" )
	private double stepRadius;

	@Parameter( required = false, min = "0.1" )
	private double sensitivity = 20.;

	@Override
	public List< HoughCircle > calculate( final RandomAccessibleInterval< T > input )
	{
		final int numDimensions = input.numDimensions();
		final ExecutorService es = threadService.getExecutorService();

		final double threshold = 2. * Math.PI * minRadius / sensitivity;
		final T t = Util.getTypeFromInterval( input );
		t.setReal( threshold );
		final MaximumCheck< T > check = new LocalExtrema.MaximumCheck< T >( t );
		final ArrayList< Point > extrema = LocalExtrema.findLocalExtrema( input, check, es );

		/*
		 * Create circles.
		 */

		final ArrayList< HoughCircle > circles = new ArrayList<>( extrema.size() );
		final RandomAccess< T > ra = input.randomAccess( input );
		for ( final Point peak : extrema )
		{
			// Minima are negative.
			final RealPoint center = new RealPoint( numDimensions - 1 );
			for ( int d = 0; d < numDimensions - 1; d++ )
				center.setPosition( peak.getDoublePosition( d ), d );

			final double radius = minRadius + ( peak.getDoublePosition( numDimensions - 1 ) ) * stepRadius;
			ra.setPosition( peak );
			final double ls = -2. * Math.PI * minRadius / ra.get().getRealDouble();
			circles.add( new HoughCircle( center, radius, ls ) );
		}

		return circles;
	}
}
