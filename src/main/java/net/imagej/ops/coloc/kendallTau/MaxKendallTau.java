/*-
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

package net.imagej.ops.coloc.kendallTau;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import net.imagej.ops.Contingent;
import net.imagej.ops.Ops;
import net.imagej.ops.coloc.ColocUtil;
import net.imagej.ops.coloc.IntArraySorter;
import net.imagej.ops.coloc.IntComparator;
import net.imagej.ops.coloc.MergeSort;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.IterablePair;
import net.imglib2.util.Pair;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * This algorithm calculates Maximum Trunctated Kendall Tau (MTKT) from Wang et
 * al. (2017), use explicitly defined thresholds if provided, otherwise compute
 * them using Otsu method.
 *
 * @param <T> Type of the first image
 * @param <U> Type of the second image
 */
@Plugin(type = Ops.Coloc.KendallTau.class)
public class MaxKendallTau<T extends RealType<T>, U extends RealType<U>> extends
	AbstractBinaryFunctionOp<Iterable<T>, Iterable<U>, Double> implements
	Ops.Coloc.KendallTau, Contingent
{

	@Parameter(required = false)
	private T threshold1;

	@Parameter(required = false)
	private U threshold2;

	@Override
	public Double calculate(final Iterable<T> image1, final Iterable<U> image2) {

		final Iterable<Pair<T, U>> samples = new IterablePair<>(image1, image2);

		double[][] values;
		double[][] rank;
		double maxtau;

		int capacity = 0;
		for (@SuppressWarnings("unused")
		final Pair<T, U> sample : samples) {
			capacity++;
		}

		values = dataPreprocessing(samples, capacity);

		final double[] values1 = new double[capacity];
		final double[] values2 = new double[capacity];
		for (int i = 0; i < capacity; i++) {
			values1[i] = values[i][0];
			values2[i] = values[i][1];
		}
		// use explicitly defined thresholds if provided, otherwise compute them
		final double thresh1 = threshold1 == null ? //
			threshold(image1) : threshold1.getRealDouble();
		final double thresh2 = threshold2 == null ? //
			threshold(image2) : threshold2.getRealDouble();

		rank = rankTransformation(values, thresh1, thresh2, capacity);

		maxtau = calculateMaxKendallTau(rank, thresh1, thresh2, capacity);

		return maxtau;
	}

	private <V extends RealType<V>> double threshold(final Iterable<V> image) {
		// call Otsu if explicit threshold was not given
		final Histogram1d<V> histogram = ops().image().histogram(image);
		return ops().threshold().otsu(histogram).getRealDouble();
	}

	protected double[][] dataPreprocessing(
		final Iterable<Pair<T, U>> iterablePair, final int capacity)
	{
		final double[][] values = new double[capacity][2];
		int count = 0;
		for (final Pair<T, U> sample : iterablePair) {
			values[count][0] = sample.getA().getRealDouble();
			values[count][1] = sample.getB().getRealDouble();
			count++;
		}
		return values;
	}

	private double[][] rankTransformation(final double[][] values,
		final double thres1, final double thres2, final int n)
	{
		final double[][] tempRank = new double[n][2];
		for (int i = 0; i < n; i++) {
			tempRank[i][0] = values[i][0];
			tempRank[i][1] = values[i][1];
		}

		Arrays.sort(tempRank, new Comparator<double[]>() {

			@Override
			public int compare(final double[] row1, final double[] row2) {
				return Double.compare(row1[1], row2[1]);
			}
		});

		int start = 0;
		int end = 0;
		int rank = 0;
		while (end < n - 1) {
			while (Double.compare(tempRank[start][1], tempRank[end][1]) == 0) {
				end++;
				if (end >= n) break;
			}
			for (int i = start; i < end; i++) {
				tempRank[i][1] = rank + Math.random();
			}
			rank++;
			start = end;
		}

		Arrays.sort(tempRank, new Comparator<double[]>() {

			@Override
			public int compare(final double[] row1, final double[] row2) {
				return Double.compare(row1[1], row2[1]);
			}
		});

		for (int i = 0; i < n; i++) {
			tempRank[i][1] = i + 1;
		}

		// second
		Arrays.sort(tempRank, new Comparator<double[]>() {

			@Override
			public int compare(final double[] row1, final double[] row2) {
				return Double.compare(row1[0], row2[0]);
			}
		});

		start = 0;
		end = 0;
		rank = 0;
		while (end < n - 1) {
			while (Double.compare(tempRank[start][0], tempRank[end][0]) == 0) {
				end++;
				if (end >= n) break;
			}
			for (int i = start; i < end; i++) {
				tempRank[i][0] = rank + Math.random();
			}
			rank++;
			start = end;
		}

		Arrays.sort(tempRank, new Comparator<double[]>() {

			@Override
			public int compare(final double[] row1, final double[] row2) {
				return Double.compare(row1[0], row2[0]);
			}
		});

		for (int i = 0; i < n; i++) {
			tempRank[i][0] = i + 1;
		}

		final List<Integer> validIndex = new ArrayList<Integer>();
		for (int i = 0; i < n; i++) {
			if (tempRank[i][0] >= thres1 && tempRank[i][1] >= thres2) {
				validIndex.add(i);
			}
		}

		final int rn = validIndex.size();
		final double[][] finalrank = new double[rn][2];
		int index = 0;
		for (final Integer i : validIndex) {
			finalrank[index][0] = tempRank[i][0];
			finalrank[index][1] = tempRank[i][1];
			index++;
		}

		return finalrank;
	}

	private double calculateMaxKendallTau(final double[][] rank,
		final double thresholdRank1, final double thresholdRank2, final int n)
	{
		final int rn = rank.length;
		int an;
		final double step = 1 + 1.0 / Math.log(Math.log(n));
		double tempOff1 = 1;
		double tempOff2;
		List<Integer> activeIndex;
		double sdTau;
		double kendallTau;
		double normalTau;
		double maxNormalTau = Double.MIN_VALUE;

		while (tempOff1 * step + thresholdRank1 < n) {
			tempOff1 *= step;
			tempOff2 = 1;
			while (tempOff2 * step + thresholdRank2 < n) {
				tempOff2 *= step;

				activeIndex = new ArrayList<Integer>();
				for (int i = 0; i < rn; i++) {
					if (rank[i][0] >= n - tempOff1 && rank[i][1] >= n - tempOff2) {
						activeIndex.add(i);
					}
				}
				an = activeIndex.size();
				if (an > 1) {
					kendallTau = calculateKendallTau(rank, activeIndex);
					sdTau = Math.sqrt(2.0 * (2 * an + 5) / 9 / an / (an - 1));
					normalTau = kendallTau / sdTau;
				}
				else {
					normalTau = Double.MIN_VALUE;
				}
				if (normalTau > maxNormalTau) maxNormalTau = normalTau;
			}
		}

		return maxNormalTau;
	}

	private double calculateKendallTau(final double[][] rank,
		final List<Integer> activeIndex)
	{
		final int an = activeIndex.size();
		final double[][] partRank = new double[2][an];
		int indicatr = 0;
		for (final Integer i : activeIndex) {
			partRank[0][indicatr] = rank[i][0];
			partRank[1][indicatr] = rank[i][1];
			indicatr++;
		}
		final double[] partRank1 = partRank[0];
		final double[] partRank2 = partRank[1];

		final int[] index = new int[an];
		for (int i = 0; i < an; i++) {
			index[i] = i;
		}

		IntArraySorter.sort(index, new IntComparator() {

			@Override
			public int compare(final int a, final int b) {
				final double xa = partRank1[a];
				final double xb = partRank1[b];
				return Double.compare(xa, xb);
			}
		});

		final MergeSort mergeSort = new MergeSort(index, new IntComparator() {

			@Override
			public int compare(final int a, final int b) {
				final double ya = partRank2[a];
				final double yb = partRank2[b];
				return Double.compare(ya, yb);
			}
		});

		final long n0 = an * (long) (an - 1) / 2;
		final long S = mergeSort.sort();

		return (n0 - 2 * S) / (double) n0;

	}

	@Override
	public boolean conforms() {
		return ColocUtil.sameIterationOrder(in1(), in2());
	}
}
