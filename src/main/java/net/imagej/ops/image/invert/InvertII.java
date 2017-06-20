/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2014 - 2017 Board of Regents of the University of
 * Wisconsin-Madison, University of Konstanz and Brian Northan.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imagej.ops.image.invert;

import net.imagej.ops.Ops;
import net.imagej.ops.special.computer.AbstractUnaryComputerOp;
import net.imagej.ops.special.computer.Computers;
import net.imagej.ops.special.computer.UnaryComputerOp;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;

import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * @author Martin Horn (University of Konstanz)
 */
@Plugin(type = Ops.Image.Invert.class, priority = Priority.NORMAL_PRIORITY + 1)
public class InvertII<I extends RealType<I>> extends
	AbstractUnaryComputerOp<IterableInterval<I>, IterableInterval<I>> implements
	Ops.Image.Invert
{

	@Parameter(required = false)
	private double min = Double.NaN;

	@Parameter(required = false)
	private double max = Double.NaN;

	private UnaryComputerOp<IterableInterval<I>, IterableInterval<I>> mapper;
	private UnaryFunctionOp<IterableInterval<I>, Pair<I, I>> minMaxFunc;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void initialize() {
		minMaxFunc = (UnaryFunctionOp) Functions.unary(ops(),
			Ops.Stats.MinMax.class, Pair.class, IterableInterval.class);

		mapper = Computers.unary(ops(), Ops.Map.class, out(), in(),
			new AbstractUnaryComputerOp<I, I>()
		{
				@Override
				public void compute(I input, I output) {
					output.setReal((min+max)-input.getRealDouble());
				}
			});
	}

	@Override
	public void compute(final IterableInterval<I> input,
		final IterableInterval<I> output)
	{
		// Min-max-based inversion
		if (min == Double.NaN && max == Double.NaN) {
		// MinMax conversion (min, max not provided
			final Pair<I, I> minMax = minMaxFunc.calculate(input);
			min = minMax.getA().getRealDouble();
			max = minMax.getB().getRealDouble();
		}

		// Type-based inversion
		if (min == Double.NaN && max == Double.NaN) {
			I type = input.firstElement();
			min = type.getMinValue();
			max = type.getMaxValue();
		}

		mapper.compute(input, output);
	}

}
