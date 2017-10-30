package net.imagej.ops.segment.hough;

import net.imagej.ops.special.hybrid.UnaryHybridCF;
import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.type.BooleanType;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * Marker interface for Hough circle transform ops.
 * 
 * @author Jean-Yves Tinevez
 */
public interface HoughCircleTransformOp< T extends BooleanType< T > > extends UnaryHybridCF< IterableInterval< T >, Img< DoubleType > >
{

}
