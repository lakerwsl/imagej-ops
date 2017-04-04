package net.imagej.ops.hough;

import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.type.numeric.RealType;

/**
 * Write circles in an image using the mid-point algorithm.
 * 
 * @author Jean-Yves Tinevez
 * @see <a href=
 *      "https://en.wikipedia.org/wiki/Midpoint_circle_algorithm">Midpoint
 *      circle algorithm on Wikipedia.</a>
 *
 */
public class MidPointAlgorithm
{
	/**
	 * Writes a circle in the target image accessed via the specified random
	 * access. The circle is written by <b>incrementing</b> the pixel values by
	 * 1 along the circle.
	 * 
	 * @param ra
	 *            the random access into the target image. It is the caller
	 *            responsibility to ensure it can be access everywhere the
	 *            circle will be iterated.
	 * @param center
	 *            the circle center. Must be at least of dimension 2. Dimensions
	 *            0 and 1 are used to specify the circle center.
	 * @param radius
	 *            the circle radius. The circle is written in a plane in
	 *            dimensions 0 and 1.
	 * @param <T>
	 *            the type of the target image.
	 */
	public static < T extends RealType< T > > void inc( final RandomAccess< T > ra, final Localizable center, final long radius )
	{
		final int x0 = center.getIntPosition( 0 );
		final int y0 = center.getIntPosition( 1 );

		final int octantSize = ( int ) Math.floor( ( Math.sqrt( 2 ) * ( radius - 1 ) + 4 ) / 2 );

		long x = 0;
		long y = radius;
		long f = 1 - radius;
		long dx = 1;
		long dy = -2 * radius;

		for ( int i = 2; i < octantSize; i++ )
		{
			// We update x & y
			if ( f > 0 )
			{
				y = y - 1;
				dy = dy + 2;
				f = f + dy;
			}
			x = x + 1;
			dx = dx + 2;
			f = f + dx;

			// 1st octant.
			ra.setPosition( x0 + x, 0 );
			ra.setPosition( y0 + y, 1 );
			ra.get().inc();

			// 2nd octant.
			ra.setPosition( x0 - x, 0 );
			ra.get().inc();

			// 3rd octant.
			ra.setPosition( x0 + x, 0 );
			ra.setPosition( y0 - y, 1 );
			ra.get().inc();

			// 4th octant.
			ra.setPosition( x0 - x, 0 );
			ra.get().inc();

			// 5th octant.
			ra.setPosition( x0 + y, 0 );
			ra.setPosition( y0 + x, 1 );
			ra.get().inc();

			// 6th octant.
			ra.setPosition( x0 - y, 0 );
			ra.get().inc();

			// 7th octant.
			ra.setPosition( x0 + y, 0 );
			ra.setPosition( y0 - x, 1 );
			ra.get().inc();

			// 8th octant.
			ra.setPosition( x0 - y, 0 );
			ra.get().inc();
		}
	}

	/**
	 * Writes a circle in the target image accessed via the specified random
	 * access. The circle is written by <b>setting</b> the pixel values with the
	 * specified value.
	 * 
	 * @param ra
	 *            the random access into the target image. It is the caller
	 *            responsibility to ensure it can be access everywhere the
	 *            circle will be iterated.
	 * @param center
	 *            the circle center. Must be at least of dimension 2. Dimensions
	 *            0 and 1 are used to specify the circle center.
	 * @param radius
	 *            the circle radius. The circle is written in a plane in
	 *            dimensions 0 and 1.
	 * @param value
	 *            the value to write along the circle.
	 * @param <T>
	 *            the type of the target image.
	 */
	public static < T extends RealType< T > > void set( final RandomAccess< T > ra, final Localizable center, final long radius, final T value )
	{
		final int x0 = center.getIntPosition( 0 );
		final int y0 = center.getIntPosition( 1 );

		/*
		 * We "zig-zag" through indices, so that we reconstruct a continuous set
		 * of of x,y coordinates, starting from the top of the circle.
		 */

		final int octantSize = ( int ) Math.floor( ( Math.sqrt( 2 ) * ( radius - 1 ) + 4 ) / 2 );

		long x = 0;
		long y = radius;
		long f = 1 - radius;
		long dx = 1;
		long dy = -2 * radius;

		for ( int i = 2; i < octantSize; i++ )
		{
			// We update x & y
			if ( f > 0 )
			{
				y = y - 1;
				dy = dy + 2;
				f = f + dy;
			}
			x = x + 1;
			dx = dx + 2;
			f = f + dx;

			// 1st octant.
			ra.setPosition( x0 + x, 0 );
			ra.setPosition( y0 + y, 1 );
			ra.get().set( value );

			// 2nd octant.
			ra.setPosition( x0 - x, 0 );
			ra.get().set( value );

			// 3rd octant.
			ra.setPosition( x0 + x, 0 );
			ra.setPosition( y0 - y, 1 );
			ra.get().set( value );

			// 4th octant.
			ra.setPosition( x0 - x, 0 );
			ra.get().set( value );

			// 5th octant.
			ra.setPosition( x0 + y, 0 );
			ra.setPosition( y0 + x, 1 );
			ra.get().set( value );

			// 6th octant.
			ra.setPosition( x0 - y, 0 );
			ra.get().set( value );

			// 7th octant.
			ra.setPosition( x0 + y, 0 );
			ra.setPosition( y0 - x, 1 );
			ra.get().set( value );

			// 8th octant.
			ra.setPosition( x0 - y, 0 );
			ra.get().set( value );
		}
	}
	
	/**
	 * Writes a circle in the target image accessed via the specified random
	 * access. The circle is written by <b>adding</b> the specified value to the
	 * pixel values already in the image.
	 * 
	 * @param ra
	 *            the random access into the target image. It is the caller
	 *            responsibility to ensure it can be access everywhere the
	 *            circle will be iterated.
	 * @param center
	 *            the circle center. Must be at least of dimension 2. Dimensions
	 *            0 and 1 are used to specify the circle center.
	 * @param radius
	 *            the circle radius. The circle is written in a plane in
	 *            dimensions 0 and 1.
	 * @param value
	 *            the value to add along the circle.
	 * @param <T>
	 *            the type of the target image.
	 */
	public static < T extends RealType< T > > void add( final RandomAccess< T > ra, final Localizable center, final long radius, final T value )
	{
		final int x0 = center.getIntPosition( 0 );
		final int y0 = center.getIntPosition( 1 );

		final int octantSize = ( int ) Math.floor( ( Math.sqrt( 2 ) * ( radius - 1 ) + 4 ) / 2 );

		long x = 0;
		long y = radius;
		long f = 1 - radius;
		long dx = 1;
		long dy = -2 * radius;

		for ( int i = 2; i < octantSize; i++ )
		{
			// We update x & y
			if ( f > 0 )
			{
				y = y - 1;
				dy = dy + 2;
				f = f + dy;
			}
			x = x + 1;
			dx = dx + 2;
			f = f + dx;

			// 1st octant.
			ra.setPosition( x0 + x, 0 );
			ra.setPosition( y0 + y, 1 );
			ra.get().add( value );

			// 2nd octant.
			ra.setPosition( x0 - x, 0 );
			ra.get().add( value );

			// 3rd octant.
			ra.setPosition( x0 + x, 0 );
			ra.setPosition( y0 - y, 1 );
			ra.get().add( value );

			// 4th octant.
			ra.setPosition( x0 - x, 0 );
			ra.get().add( value );

			// 5th octant.
			ra.setPosition( x0 + y, 0 );
			ra.setPosition( y0 + x, 1 );
			ra.get().add( value );

			// 6th octant.
			ra.setPosition( x0 - y, 0 );
			ra.get().add( value );

			// 7th octant.
			ra.setPosition( x0 + y, 0 );
			ra.setPosition( y0 - x, 1 );
			ra.get().add( value );

			// 8th octant.
			ra.setPosition( x0 - y, 0 );
			ra.get().add( value );
		}
	}
	
	private MidPointAlgorithm()
	{}

}
