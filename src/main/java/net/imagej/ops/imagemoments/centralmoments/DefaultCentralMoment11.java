/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2014 - 2015 Board of Regents of the University of
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

package net.imagej.ops.imagemoments.centralmoments;

import org.scijava.plugin.Plugin;

import net.imagej.ops.Op;
import net.imagej.ops.Ops.ImageMoments.CentralMoment11;
import net.imagej.ops.imagemoments.AbstractImageMomentOp;
import net.imagej.ops.imagemoments.ImageMomentOp;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;

/**
 * {@link Op} to calculate the {@link CentralMoment11}.
 * 
 * @author Daniel Seebacher, University of Konstanz.
 * @author Christian Dietz, University of Konstanz.
 * @param <I> input type
 * @param <O> output type
 */
@Plugin(type = ImageMomentOp.class, name = CentralMoment11.NAME,
	label = "Image Moment: CentralMoment11")
public class DefaultCentralMoment11<I extends RealType<I>, O extends RealType<O>>
	extends AbstractImageMomentOp<I, O> implements CentralMoment11
{

	@Override
	public void compute(final IterableInterval<I> input, final O output) {
		final double moment00 = ops.imagemoments().moment00(input).getRealDouble();
		final double moment01 = ops.imagemoments().moment01(input).getRealDouble();
		final double moment10 = ops.imagemoments().moment10(input).getRealDouble();
		final double moment11 = ops.imagemoments().moment11(input).getRealDouble();

		final double centerX = moment10 / moment00;

		output.setReal(moment11 - (centerX * moment01));
	}
}