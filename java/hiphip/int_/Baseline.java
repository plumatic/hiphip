package hiphip.int_;

import clojure.lang.IFn;

/*********************************************************************************************
 * Functions equivalent to (specific applications of) hiphip macros for benchmarking and
 * testing purposes.
 *********************************************************************************************/
public class Baseline {
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

  public static int[] multiply_end_in_place_pointwise(int[] xs, int[] ys) {
    for(int i = (xs.length)/2; i < xs.length; i++) {
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
  
  public static int[] amap_end_inc(int[] arr) {
    int h1 = arr.length/2, h2 = arr.length - h1;
    int[] ret = new int[h2];
    for (int i = 0; i < h2; i++) {
      ret[i] = arr[i+h1] + 1;
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

  public static int asum_end(int[] arr) {
    int s = 0;
    for (int i = arr.length/2; i < arr.length; i++) {
      s += arr[i];
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

  public static Double amean(int[] arr) {
    return (1.0 * asum(arr)) / arr.length;
  }
}
