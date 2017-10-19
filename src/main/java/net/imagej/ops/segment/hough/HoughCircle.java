package net.imagej.ops.segment.hough;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.util.Contains;

public class HoughCircle extends RealPoint implements Comparable< HoughCircle >, Contains< RealLocalizable >
{

	private final double radius;

	private final double sensitivity;

	public HoughCircle( final RealLocalizable pos, final double radius, final double sensitivity )
	{
		super( pos );
		this.radius = radius;
		this.sensitivity = sensitivity;
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		char c = '(';
		for ( int i = 0; i < numDimensions(); i++ )
		{
			sb.append( c );
			sb.append( String.format( "%.1f", position[ i ] ) );
			c = ',';
		}
		sb.append( ")" );
		return String.format( "%s\tR=%.1f\tSensitivity=%.1f", sb.toString(), radius, sensitivity );
	}

	public double getRadius()
	{
		return radius;
	}

	public double getSensitivity()
	{
		return sensitivity;
	}

	@Override
	public int compareTo( final HoughCircle o )
	{
		return sensitivity < o.sensitivity ? -1 : sensitivity > o.sensitivity ? +1 : 0;
	}

	@Override
	public boolean contains( final RealLocalizable point )
	{
		final double dx = getDoublePosition( 0 ) - point.getDoublePosition( 0 );
		final double dy = getDoublePosition( 1 ) - point.getDoublePosition( 1 );
		final double dr2 = dx * dx + dy * dy;

		if ( dr2 < radius * radius )
			return false;

		return true;
	}

	@Override
	public HoughCircle copyContains()
	{
		return new HoughCircle( this, radius, sensitivity );
	}
}
