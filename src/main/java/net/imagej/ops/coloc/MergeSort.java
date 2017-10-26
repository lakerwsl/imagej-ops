package net.imagej.ops.coloc;

/**
 * Helper class for MaxKendallTau op.
 *
 * @author Ellen Arena
 */
public class MergeSort {

	private int[] index;
	private final IntComparator comparator;

	public MergeSort(int[] index, IntComparator comparator) {
		this.index = index;
		this.comparator = comparator;
	}

	public int[] getSorted() {
		return index;
	}

	/**
	 * Sorts the {@link #index} array.
	 * <p>
	 * This implements a non-recursive merge sort.
	 * </p>
	 * @param begin
	 * @param end
	 * @return the equivalent number of BubbleSort swaps
	 */
	public long sort() {
		long swaps = 0;
		int n = index.length;
		// There are merge sorts which perform in-place, but their runtime is worse than O(n log n)
		int[] index2 = new int[n];
		for (int step = 1; step < n; step <<= 1) {
			int begin = 0, k = 0;
			for (;;) {
				int begin2 = begin + step, end = begin2 + step;
				if (end >= n) {
					if (begin2 >= n) {
						break;
					}
					end = n;
				}

				// calculate the equivalent number of BubbleSort swaps
				// and perform merge, too
				int i = begin, j = begin2;
				while (i < begin2 && j < end) {
					int compare = comparator.compare(index[i], index[j]);
					if (compare > 0) {
						swaps += (begin2 - i);
						index2[k++] = index[j++];
					} else {
						index2[k++] = index[i++];
					}
				}
				if (i < begin2) {
					do {
						index2[k++] = index[i++];
					} while (i < begin2);
				} else {
					while (j < end) {
						index2[k++] = index[j++];
					}
				}
				begin = end;
			}
			if (k < n) {
				System.arraycopy(index, k, index2, k, n - k);
			}
			int[] swapIndex = index2;
			index2 = index;
			index = swapIndex;
		}

		return swaps;
	}

}
