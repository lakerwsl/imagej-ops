/*-
 * #%L
 * Fiji's plugin for colocalization analysis.
 * %%
 * Copyright (C) 2009 - 2017 Fiji developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package net.imagej.ops.coloc;

import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;

/**
 * A class allowing an easy accumulation of values visited by an
 * IterablePair. After instantiation the sum of channel one,
 * channel two, products with them self and a product of both of
 * them will be available. It additionally provides the possibility
 * to subtract values from the data before adding them to the sum.:
 * 
 * @author Johannes Schindelin
 * @author Tom Kazimiers
 * @author Ellen T Arena
 */
public abstract class Accumulator<T extends RealType< T >, U extends RealType<U>> {
	private double x, y, xx, xy, yy;
	private int count;

	/**
	 * The two values x and y from each iteration to get
	 * summed up as single values and their combinations.
	 */
	public Accumulator(final Iterable<Pair<T, U>> samples) {
		this(samples, false, 0.0d, 0.0d);
	}

	/**
	 * The two values (x - xDiff) and (y - yDiff) from each
	 * iteration to get summed up as single values and their combinations.
	 */
	public Accumulator(final Iterable<Pair<T,U>> samples, double xDiff, double yDiff) {
		this(samples, true, xDiff, yDiff);
	}

	protected Accumulator(final Iterable<Pair<T, U>> samples, boolean substract, double xDiff, double yDiff) {
		
		for (Pair<T, U> sample : samples) {
			
			T type1 = sample.getA();
			U type2 = sample.getB();
			
			if (!accept(type1, type2))
				continue;
			
			double value1 = sample.getA().getRealDouble();
			double value2 = sample.getB().getRealDouble();

			if (substract) {
				value1 -= xDiff;
				value2 -= yDiff;
			}

			x += value1;
			y += value2;
			xx += value1 * value1;
			xy += value1 * value2;
			yy += value2 * value2;
			count++;
		}
	}

	public abstract boolean accept(T type1, U type2);

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getXX() {
		return xx;
	}

	public double getXY() {
		return xy;
	}

	public double getYY() {
		return yy;
	}

	public int getCount() {
		return count;
	}
}
