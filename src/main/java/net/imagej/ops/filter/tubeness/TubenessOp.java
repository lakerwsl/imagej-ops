package net.imagej.ops.filter.tubeness;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;

import org.scijava.app.StatusService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import net.imagej.ops.special.computer.AbstractUnaryComputerOp;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.outofbounds.OutOfBoundsBorderFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

/**
 * The Tubeness filter as an Op.
 * <p>
 * This filter works on 2D and 3D image exclusively and produces a score for how
 * "tube-like" each point in the image is. This is useful as a preprocessing
 * step for tracing neurons or blood vessels, for example. For 3D image stacks,
 * the filter uses the eigenvalues of the Hessian matrix to calculate this
 * measure of "tubeness", using one of the simpler metrics me mentioned in
 * <it>Sato et al 1997</it>: if the larger two eigenvalues (λ₂ and λ₃) are both
 * negative then value is √(λ₂λ₃), otherwise the value is 0. For 2D images, if
 * the large eigenvalue is negative, we return its absolute value and otherwise
 * return 0.
 * <p>
 * The initial version of this filter was written by Mark Longair, Stephan
 * Preibisch and Johannes Schindelin, and was part of the VIB package. This
 * class is a full rewrite from scratch, using the ops framework.
 * 
 * @author Jean-Yves Tinevez
 *
 * @param <T>
 *            the type of the source image, must be real scalar.
 * @see <a href="https://www.longair.net/edinburgh/imagej/tubeness/">Tubeness
 *      VIB page.</a>
 * @see <a href=
 *      "https://github.com/fiji/VIB/blob/master/src/main/java/features/Tubeness_.java">Tubeness
 *      VIB source code.</a>
 */
@Plugin( type = TubenessOp.class )
public class TubenessOp< T extends RealType< T > >
		extends AbstractUnaryFunctionOp< RandomAccessibleInterval< T >, Img< DoubleType > >
{

	@Parameter
	private ThreadService threadService;
	
	@Parameter
	private StatusService statusService;

	/**
	 * Desired scale in sigma in physical units.
	 */
	@Parameter
	private double sigma;

	/**
	 * Pixel sizes in all dimension.
	 */
	@Parameter
	private double[] calibration;

	@Override
	public Img< DoubleType > calculate( final RandomAccessibleInterval< T > input )
	{
		
		final int numDimensions = input.numDimensions();
		// Sigmas in pixel units.
		final double[] sigmas = new double[ numDimensions ];
		for ( int d = 0; d < sigmas.length; d++ )
			sigmas[ d ] = sigma / calibration[ d ];
		
		/*
		 * Hessian.
		 */

		// Get a suitable image factory.
		final long[] dims = new long[ numDimensions + 1 ];
		for ( int d = 0; d < numDimensions; d++ )
			dims[ d ] = input.dimension( d );
		dims[ numDimensions ] = numDimensions * ( numDimensions + 1 ) / 2;
		final Dimensions dimensions = FinalDimensions.wrap( dims );
		final ImgFactory< DoubleType > factory = ops().create().imgFactory( dimensions );
		
		// Handle multithreading.
		final int nThreads = Runtime.getRuntime().availableProcessors();
		final ExecutorService es = threadService.getExecutorService();

		try
		{
			// Hessian calculation.
			final Img< DoubleType > hessian = HessianMatrix.calculateMatrix(
					Views.extendBorder( input ),
					input, 
					sigmas, 
					new OutOfBoundsBorderFactory<>(), 
					factory, new DoubleType(), 
					nThreads, es );

			statusService.showProgress( 1, 3 );

			// Hessian eigenvalues.
			final Img< DoubleType > evs = TensorEigenValues.calculateEigenValuesSymmetric(
					hessian,
					factory, new DoubleType(),
					nThreads, es );

			statusService.showProgress( 2, 3 );

			// Tubeness is derived from largest eigenvalues.
			final Img< DoubleType > tubeness = ops().create().img( input, new DoubleType() );
			final AbstractUnaryComputerOp< Iterable< DoubleType >, DoubleType > method;
			switch ( numDimensions )
			{
			case 2:
				method = new Tubeness2D( sigma );
				break;
			case 3:
				method = new Tubeness3D( sigma );
			default:
				System.err.println( "Cannot compute tubeness for " + numDimensions + "D images." );
				return null;
			}
			ops().transform().project( tubeness, evs, method, numDimensions );

			statusService.showProgress( 3, 3 );

			return tubeness;
		}
		catch ( final IncompatibleTypeException e )
		{
			e.printStackTrace();
			return null;
		}
	}

	private static final class Tubeness2D extends AbstractUnaryComputerOp< Iterable< DoubleType >, DoubleType >
	{

		private final double sigma;

		public Tubeness2D( final double sigma )
		{
			this.sigma = sigma;
		}

		@Override
		public void compute( final Iterable< DoubleType > input, final DoubleType output )
		{
			// Use just the largest one.
			final Iterator< DoubleType > it = input.iterator();
			it.next();
			final double val = it.next().get();
			if ( val >= 0. )
				output.setZero();
			else
				output.set( sigma * sigma * Math.abs( val ) );

		}
	}

	private static final class Tubeness3D extends AbstractUnaryComputerOp< Iterable< DoubleType >, DoubleType >
	{

		private final double sigma;

		public Tubeness3D( final double sigma )
		{
			this.sigma = sigma;
		}

		@Override
		public void compute( final Iterable< DoubleType > input, final DoubleType output )
		{
			// Use geometric mean of the two largest ones.
			final Iterator< DoubleType > it = input.iterator();
			it.next();
			final double val1 = it.next().get();
			final double val2 = it.next().get();
			if ( val1 >= 0. || val2 >= 0. )
				output.setZero();
			else
				output.set( sigma * sigma * Math.sqrt( val1 * val2 ) );

		}
	}


}
