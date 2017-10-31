package net.imagej.ops.filter.tubeness;

import net.imagej.ops.Ops;
import net.imagej.ops.special.hybrid.UnaryHybridCF;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

public interface TubenessOp< T extends RealType< T > > extends Ops.Filter.Tubeness,
		UnaryHybridCF< RandomAccessibleInterval< T >, IterableInterval< DoubleType > >
{

}
