package hiphip.int_;

import clojure.lang.IFn;

public class JavaBaseline {
  /*********************************************************************************************
   * Functions equivalent to (specific applications of) hiphip macros for benchmarking and
   * testing purposes.
   *********************************************************************************************/

  public static int alength(int [] arr) {
    return arr.length;
  }

  public static int aget(int [] arr, int idx) {
    return arr[idx];
  }

  public static int aset(int [] arr, int idx, int v) {
    arr[idx] = v;
    return v;
  }

  public static int ainc(int [] arr, int idx, int v) {
    return arr[idx]+=v;
  }
  
  public static int[] aclone(int [] arr) {
    return arr.clone();
  }
  
  // tests areduce and dot-product 
  public static int dot_product(int[] arr1, int[] arr2) {
    int s = 0;
    for (int i = 0; i < arr1.length; i++) {
      s += arr1[i] * arr2[i];
    }
    return s;
  }

  
  // tests doarr and afill!
  public static int[] multiply_in_place_pointwise(int[] xs, int[] ys) {
    for(int i = 0; i < xs.length; i++) {
      xs[i] *= ys[i];
    }
    return xs;
  }

  // tests afill!
  public static int[] multiply_in_place_by_idx(int[] xs) {
    for(int i = 0; i < xs.length; i++) {
      xs[i] *= i;
    }
    return xs;
  }

  // tests amake
  public static int[] acopy_inc(int len, int[] ys) {
    int[] ret = new int[len];
    for(int i = 0; i < len; i++) {
      ret[i] = ys[i] + 1;
    }
    return ret;
  }

  public static int[] amap_inc(int[] arr) {
    int[] ret = new int[arr.length];
    for (int i = 0; i < arr.length; i++) {
      ret[i] = arr[i] + 1;
    }
    return ret;
  }

  public static int[] amap_plus_idx(int[] arr) {
    int[] newarr = new int[arr.length];
    for (int i = 0; i < arr.length; i++) {
      newarr[i] = arr[i] + i;
    }
    return newarr;
  }

  public static int asum(int[] arr) {
    int s = 0;
    for (int d : arr) {
      s += d;
    }
    return s;
  }

  public static int asum_square(int[] arr) {
    int s = 0;
    for (int d : arr) {
      s += d * d;
    }
    return s;
  }

  public static int aproduct(int[] arr) {
    int s = 1;
    for (int d : arr) {
      s *= d;
    }
    return s;
  }

  public static int amax(int[] arr) {
    int m = arr[0];
    for (int i = 1; i < arr.length; i++) {
      int v = arr[i];
      if (v > m) m = v;
    }
    return m;
  }

  public static int amin(int[] arr) {
    int m = arr[0];
    for (int i = 1; i < arr.length; i++) {
      int v = arr[i];
      if (v < m) m = v;
    }
    return m;
  }

  public static int amean(int[] arr) {
    return asum(arr) / arr.length;
  }
 
  /*********************************************************************************************
   * Functions used within hiphip API, since we couldn't (yet) generate pure Clojure versions 
   * that are (nearly) as efficient as Java.
   *********************************************************************************************/

  private static void swap(int[] arr, int i, int j) {
    int tmp = arr[i];
    arr[i] = arr[j];
    arr[j] = tmp;
  }
  
  /**
  * Partitions an array using a standard 3-way partitioning algorithm.  Given an array 
  * arr, a range in this array [left, right), and a pivot, modifies arr so that all 
  * elements less than pivot come first (in no particular order), followed by all equal
  * elements, followed by all greater elements.  
  * 
  * @param  arr   the array to be partitioned
  * @param  left  the index to start partitioning at 
  * @param  right the index to stop partitioning at
  * @param  pivot the value to partition by 
  * @return       the 1 + the greatest index less than or equal to pivot.
  */    
  public static int partition(int[] arr, int left, int right, int pivot) {
    int i = left,  // right of last element known less than pivot
        j = right;   // first element known greater than pivot
    for (int k = i; k < j; k++) {
      while (pivot < arr[k]) {
        j--;
        swap(arr, j, k);
      }
      if (arr[k] < pivot) {
        if (i < k) {
          swap(arr, i, k);
        }
        i++;
      }
    }
    return j;
  }
    
  /**
  * Partitions an array using a standard 3-way partitioning algorithm.  Given an array 
  * arr, an array of indices into this array, a range of indices into this array 
  * [left, right), and a pivot, modifies indices so that all elements pointing at arr
  * elements less than pivot come first (in no particular order), followed by all equal
  * elements, followed by all greater elements.  
  * 
  * @param  indices indices into the array to be partitioned
  * @param  arr     the array to be partitioned
  * @param  left    the index to start partitioning at 
  * @param  right   the index to stop partitioning at
  * @param  pivot   the value to partition by 
  * @return         the 1 + the greatest index less than or equal to pivot.
  */    
  public static int partitionIndices(int [] indices, int[] arr, int left, int right, int pivot) {
    int i = left,  // right of last element known less than pivot
        j = right;   // first element known greater than pivot
    for (int k = i; k < j; k++) {
      while (pivot < arr[indices[k]]) {
        j--;
        hiphip.IndexArrays.swap(indices, j, k);
      }
      if (arr[indices[k]] < pivot) {
        if (i < k) {
          hiphip.IndexArrays.swap(indices, i, k);
        }
        i++;
      }
    }
    return j;
  }
    
  /**
  * Returns the first index of a largest value in xs, which must have nonzero length.
  * 
  * @param xs the array
  * @return   the first index of a maximum value in xs
  */    
  public static int maxIndex(int[] xs) {
    int am = 0;
    int m = xs[0];
    for (int i=1; i < xs.length; ++i) {
      int v = xs[i];
      if (v > m) {
	m = v;
	am = i;
      }
    }
    return am;
  }

  /**
  * Returns the first index of a smallest value in xs, which must have nonzero length.
  * 
  * @param xs the array
  * @return   the first index of a minimum value in xs
  */    
  public static int minIndex(int[] xs) {
    int am = 0;
    int m = xs[0];
    for (int i=1; i < xs.length; ++i) {
      int v = xs[i];
      if (v < m) {
	m = v;
	am = i;
      }
    }
    return am;
  }
}
