/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2014 - 2016 Board of Regents of the University of
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

package net.imagej.ops.threshold;

import net.imagej.ops.Ops;
import net.imagej.ops.pixml.DefaultHardClusterer;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imagej.ops.special.hybrid.AbstractUnaryHybridCF;
import net.imagej.ops.threshold.otsu.OtsuThresholdLearner;
import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.type.BooleanType;
import net.imglib2.type.logic.BitType;

/**
 * Abstract superclass for {@link GlobalThresholder} implementations.
 * 
 * @author Stefan Helfrich (University of Konstanz)
 * @param <I> type of input
 * @param <O> type of output
 */
public abstract class AbstractGlobalThresholder<I, O extends BooleanType<O>>
	extends AbstractUnaryHybridCF<IterableInterval<I>, Iterable<O>> implements
	GlobalThresholder<I, O>
{

	/** Op that is used to learn and apply to the whole input */
	public DefaultHardClusterer<I, O> globalThresholder;

	/** Op that is used for creating the output image */
	protected UnaryFunctionOp<IterableInterval<I>, Img<BitType>> imgCreator;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void initialize() {
		imgCreator = (UnaryFunctionOp) Functions.unary(ops(), Ops.Create.Img.class,
			Img.class, in(), new BitType());
		globalThresholder = ops().op(DefaultHardClusterer.class, in(), out(),
			getLearner());
	}

	@Override
	public void compute1(final IterableInterval<I> input,
		final Iterable<O> output)
	{
		globalThresholder.compute1(input, output);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterable<O> createOutput(IterableInterval<I> input) {
		return (Iterable<O>) imgCreator.compute1(input);
	}

	protected abstract ThresholdLearner<I, O> getLearner();
}
