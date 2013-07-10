package hiphip.long_;

import clojure.lang.IFn;

/*********************************************************************************************
 * Functions equivalent to (specific applications of) hiphip macros for benchmarking and
 * testing purposes.
 *********************************************************************************************/
public class Baseline {
  public static int alength(long [] arr) {
    return arr.length;
  }

  public static long aget(long [] arr, int idx) {
    return arr[idx];
  }

  public static long aset(long [] arr, int idx, long v) {
    arr[idx] = v;
    return v;
  }

  public static long ainc(long [] arr, int idx, int v) {
    return arr[idx]+=v;
  }
  
  public static long[] aclone(long [] arr) {
    return arr.clone();
  }
  
  // tests areduce and dot-product 
  public static long dot_product(long[] arr1, long[] arr2) {
    long s = 0;
    for (int i = 0; i < arr1.length; i++) {
      s += arr1[i] * arr2[i];
    }
    return s;
  }

  
  // tests doarr and afill!
  public static long[] multiply_in_place_pointwise(long[] xs, long[] ys) {
    for(int i = 0; i < xs.length; i++) {
      xs[i] *= ys[i];
    }
    return xs;
  }

  public static long[] multiply_end_in_place_pointwise(long[] xs, long[] ys) {
    for(int i = (xs.length)/2; i < xs.length; i++) {
      xs[i] *= ys[i];
    }
    return xs;
  }


  // tests afill!
  public static long[] multiply_in_place_by_idx(long[] xs) {
    for(int i = 0; i < xs.length; i++) {
      xs[i] *= i;
    }
    return xs;
  }

  // tests amake
  public static long[] acopy_inc(int len, long[] ys) {
    long[] ret = new long[len];
    for(int i = 0; i < len; i++) {
      ret[i] = ys[i] + 1;
    }
    return ret;
  }

  public static long[] amap_inc(long[] arr) {
    long[] ret = new long[arr.length];
    for (int i = 0; i < arr.length; i++) {
      ret[i] = arr[i] + 1;
    }
    return ret;
  }
  
  public static long[] amap_end_inc(long[] arr) {
    int h1 = arr.length/2, h2 = arr.length - h1;
    long[] ret = new long[h2];
    for (int i = 0; i < h2; i++) {
      ret[i] = arr[i+h1] + 1;
    }
    return ret;
  }

  public static long[] amap_plus_idx(long[] arr) {
    long[] newarr = new long[arr.length];
    for (int i = 0; i < arr.length; i++) {
      newarr[i] = arr[i] + i;
    }
    return newarr;
  }

  public static long asum(long[] arr) {
    long s = 0;
    for (long d : arr) {
      s += d;
    }
    return s;
  }

  public static long asum_end(long[] arr) {
    long s = 0;
    for (int i = arr.length/2; i < arr.length; i++) {
      s += arr[i];
    }
    return s;
  }

  public static long asum_square(long[] arr) {
    long s = 0;
    for (long d : arr) {
      s += d * d;
    }
    return s;
  }

  public static long aproduct(long[] arr) {
    long s = 1;
    for (long d : arr) {
      s *= d;
    }
    return s;
  }

  public static long amax(long[] arr) {
    long m = arr[0];
    for (int i = 1; i < arr.length; i++) {
      long v = arr[i];
      if (v > m) m = v;
    }
    return m;
  }

  public static long amin(long[] arr) {
    long m = arr[0];
    for (int i = 1; i < arr.length; i++) {
      long v = arr[i];
      if (v < m) m = v;
    }
    return m;
  }

  public static Double amean(long[] arr) {
    return (1.0 * asum(arr)) / arr.length;
  }
}
